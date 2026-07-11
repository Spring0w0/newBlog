package com.spring0w0.backend.service;

import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.config.UploadProperties;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.FileAssetMapper;
import com.spring0w0.backend.mapper.FileReferenceMapper;
import com.spring0w0.backend.pojo.entity.FileAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/**
 * 管理运行期图片的校验、存储、元数据和安全删除。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private static final DateTimeFormatter STORED_FILE_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final FileAssetMapper fileAssetMapper;
    private final FileReferenceMapper fileReferenceMapper;
    private final UploadProperties uploadProperties;

    @Transactional
    public FileAsset uploadImage(MultipartFile file, String requestedScope) {
        ImageUploadScope scope = ImageUploadScope.fromValue(requestedScope);
        ValidatedImage image = validateImage(file);
        Path targetFile = null;
        boolean fileStored = false;

        try {
            Path scopeDirectory = resolveScopeDirectory(scope);
            Files.createDirectories(scopeDirectory);

            String storedFilename = createStoredFilename(image.format());
            targetFile = scopeDirectory.resolve(storedFilename).normalize();
            if (!targetFile.startsWith(scopeDirectory)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "图片存储路径不合法");
            }

            writeFileAtomically(scopeDirectory, targetFile, image.content());
            fileStored = true;

            FileAsset asset = new FileAsset();
            asset.setScope(scope.value());
            asset.setStoredFilename(storedFilename);
            asset.setRelativePath(scope.value() + "/" + storedFilename);
            asset.setOriginalName(image.originalName());
            asset.setContentType(image.format().contentType());
            asset.setFileSize((long) image.content().length);
            asset.setSha256(sha256(image.content()));
            asset.setWidth(image.dimensions().width());
            asset.setHeight(image.dimensions().height());

            if (fileAssetMapper.insert(asset) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "图片元数据保存失败");
            }

            log.info("图片上传并保存元数据成功，返回参数：fileId={}，scope={}，size={}，contentType={}",
                    asset.getId(), scope.value(), asset.getFileSize(), asset.getContentType());
            return asset;
        } catch (IOException exception) {
            if (fileStored) {
                deleteQuietly(targetFile);
            }
            log.error("写入上传图片失败，传入参数：scope={}，size={}", scope.value(), image.content().length, exception);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "图片保存失败");
        } catch (RuntimeException exception) {
            if (fileStored) {
                deleteQuietly(targetFile);
            }
            throw exception;
        }
    }

    @Transactional
    public void deleteImage(Long fileId) {
        FileAsset asset = fileAssetMapper.selectById(fileId);
        if (asset == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "图片文件不存在");
        }

        long referenceCount = fileReferenceMapper.countByRelativePath(asset.getRelativePath());
        if (referenceCount > 0) {
            log.warn("拒绝删除被引用图片，传入参数：fileId={}，引用数量={}", fileId, referenceCount);
            throw new BusinessException(ResultCode.FILE_IN_USE);
        }

        Path targetFile = resolveStoredFile(asset);
        if (fileAssetMapper.deleteById(fileId) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "图片文件不存在");
        }

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException exception) {
            log.error("删除上传图片失败，传入参数：fileId={}，scope={}", fileId, asset.getScope(), exception);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "图片删除失败");
        }

        log.info("删除未被引用图片成功，返回参数：fileId={}，scope={}", fileId, asset.getScope());
    }

    private ValidatedImage validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片文件不能为空");
        }
        if (file.getSize() > uploadProperties.getMaxFileSize().toBytes()) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无法读取上传图片");
        }
        if (content.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片文件不能为空");
        }

        ImageFormat format = ImageFormat.detect(content);
        if (format == null || !format.supportsExtension(extension) || !format.contentType().equals(normalizeContentType(file.getContentType()))) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "图片类型、扩展名或文件内容不匹配");
        }

        ImageDimensions dimensions = readDimensions(content, format);
        return new ValidatedImage(originalName, content, format, dimensions);
    }

    private String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片文件名不能为空");
        }
        String normalized = originalName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片文件名不能为空");
        }
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private String extractExtension(String filename) {
        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex <= 0 || extensionIndex == filename.length() - 1) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "图片文件缺少受支持的扩展名");
        }
        return filename.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "图片 MIME 类型不能为空");
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return mediaType.getType().toLowerCase(Locale.ROOT) + "/" + mediaType.getSubtype().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "图片 MIME 类型不合法");
        }
    }

    private ImageDimensions readDimensions(byte[] content, ImageFormat format) {
        if (format == ImageFormat.WEBP) {
            return readWebpDimensions(content);
        }

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw unsupportedImageContent();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw unsupportedImageContent();
                }
                return new ImageDimensions(width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw unsupportedImageContent();
        }
    }

    private ImageDimensions readWebpDimensions(byte[] content) {
        if (content.length < 20 || readLittleEndianUnsignedInt(content, 4) + 8L > content.length) {
            throw unsupportedImageContent();
        }
        String chunkType = new String(content, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"VP8 ".equals(chunkType) && !"VP8L".equals(chunkType) && !"VP8X".equals(chunkType)) {
            throw unsupportedImageContent();
        }
        if (!"VP8X".equals(chunkType) || content.length < 30) {
            return new ImageDimensions(null, null);
        }

        int width = readLittleEndian24(content, 24) + 1;
        int height = readLittleEndian24(content, 27) + 1;
        if (width <= 0 || height <= 0) {
            throw unsupportedImageContent();
        }
        return new ImageDimensions(width, height);
    }

    private long readLittleEndianUnsignedInt(byte[] content, int offset) {
        return (content[offset] & 0xFFL)
                | ((content[offset + 1] & 0xFFL) << 8)
                | ((content[offset + 2] & 0xFFL) << 16)
                | ((content[offset + 3] & 0xFFL) << 24);
    }

    private int readLittleEndian24(byte[] content, int offset) {
        return (content[offset] & 0xFF)
                | ((content[offset + 1] & 0xFF) << 8)
                | ((content[offset + 2] & 0xFF) << 16);
    }

    private BusinessException unsupportedImageContent() {
        return new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "图片文件内容不合法");
    }

    private String createStoredFilename(ImageFormat format) {
        String date = STORED_FILE_DATE_FORMATTER.format(LocalDate.now());
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return date + "-" + randomPart + "." + format.storageExtension();
    }

    private void writeFileAtomically(Path scopeDirectory, Path targetFile, byte[] content) throws IOException {
        Path temporaryFile = Files.createTempFile(scopeDirectory, ".upload-", ".tmp");
        try {
            Files.write(temporaryFile, content, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, targetFile);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private Path resolveScopeDirectory(ImageUploadScope scope) {
        Path rootDirectory = uploadProperties.normalizedRootDir();
        Path scopeDirectory = rootDirectory.resolve(scope.value()).normalize();
        if (!scopeDirectory.startsWith(rootDirectory)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片存储路径不合法");
        }
        return scopeDirectory;
    }

    private Path resolveStoredFile(FileAsset asset) {
        ImageUploadScope scope = ImageUploadScope.fromValue(asset.getScope());
        String expectedRelativePath = scope.value() + "/" + asset.getStoredFilename();
        if (!expectedRelativePath.equals(asset.getRelativePath())) {
            log.error("文件元数据路径不一致，传入参数：fileId={}，scope={}", asset.getId(), asset.getScope());
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件元数据异常");
        }
        Path scopeDirectory = resolveScopeDirectory(scope);
        Path targetFile = scopeDirectory.resolve(asset.getStoredFilename()).normalize();
        if (!targetFile.startsWith(scopeDirectory)) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件元数据异常");
        }
        return targetFile;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private void deleteQuietly(Path targetFile) {
        if (targetFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException exception) {
            log.error("清理失败上传文件时发生异常，文件物理路径已省略", exception);
        }
    }

    private record ValidatedImage(String originalName, byte[] content, ImageFormat format, ImageDimensions dimensions) {
    }

    private record ImageDimensions(Integer width, Integer height) {
    }

    private enum ImageFormat {
        JPEG("image/jpeg", "jpg", new String[]{"jpg", "jpeg"}),
        PNG("image/png", "png", new String[]{"png"}),
        GIF("image/gif", "gif", new String[]{"gif"}),
        WEBP("image/webp", "webp", new String[]{"webp"});

        private final String contentType;
        private final String storageExtension;
        private final String[] supportedExtensions;

        ImageFormat(String contentType, String storageExtension, String[] supportedExtensions) {
            this.contentType = contentType;
            this.storageExtension = storageExtension;
            this.supportedExtensions = supportedExtensions;
        }

        String contentType() {
            return contentType;
        }

        String storageExtension() {
            return storageExtension;
        }

        boolean supportsExtension(String extension) {
            for (String supportedExtension : supportedExtensions) {
                if (supportedExtension.equals(extension)) {
                    return true;
                }
            }
            return false;
        }

        static ImageFormat detect(byte[] content) {
            if (hasPrefix(content, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) {
                return JPEG;
            }
            if (hasPrefix(content, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A)) {
                return PNG;
            }
            if (hasPrefix(content, (byte) 'G', (byte) 'I', (byte) 'F', (byte) '8', (byte) '7', (byte) 'a')
                    || hasPrefix(content, (byte) 'G', (byte) 'I', (byte) 'F', (byte) '8', (byte) '9', (byte) 'a')) {
                return GIF;
            }
            if (hasPrefix(content, (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F')
                    && content.length >= 12
                    && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
                return WEBP;
            }
            return null;
        }

        private static boolean hasPrefix(byte[] content, byte... prefix) {
            if (content.length < prefix.length) {
                return false;
            }
            for (int index = 0; index < prefix.length; index++) {
                if (content[index] != prefix[index]) {
                    return false;
                }
            }
            return true;
        }
    }
}
