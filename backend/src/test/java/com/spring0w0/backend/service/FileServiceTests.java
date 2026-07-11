package com.spring0w0.backend.service;

import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.config.UploadProperties;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.FileAssetMapper;
import com.spring0w0.backend.mapper.FileReferenceMapper;
import com.spring0w0.backend.pojo.entity.FileAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTests {

    @TempDir
    Path temporaryDirectory;

    @Mock
    private FileAssetMapper fileAssetMapper;

    @Mock
    private FileReferenceMapper fileReferenceMapper;

    @Test
    void uploadValidPngCreatesMetadataAndStoresItUnderConfiguredScope() throws Exception {
        FileService fileService = newFileService();
        when(fileAssetMapper.insert(any(FileAsset.class))).thenAnswer(invocation -> {
            FileAsset asset = invocation.getArgument(0);
            asset.setId(42L);
            return 1;
        });
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", pngContent());

        FileAsset asset = fileService.uploadImage(file, "blog-images");

        assertThat(asset.getId()).isEqualTo(42L);
        assertThat(asset.getRelativePath()).startsWith("blog-images/").endsWith(".png");
        assertThat(asset.getContentType()).isEqualTo("image/png");
        assertThat(asset.getWidth()).isEqualTo(1);
        assertThat(asset.getHeight()).isEqualTo(1);
        assertThat(temporaryDirectory.resolve(asset.getRelativePath())).isRegularFile();

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetMapper).insert(captor.capture());
        assertThat(captor.getValue().getSha256()).hasSize(64);
    }

    @Test
    void uploadRejectsMismatchedFileExtensionAndContent() throws Exception {
        FileService fileService = newFileService();
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", pngContent());

        Throwable throwable = catchThrowable(() -> fileService.uploadImage(file, "blog-images"));

        assertThat(throwable).isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) throwable).getResultCode()).isEqualTo(ResultCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void deleteRejectsAFileThatIsStillReferenced() {
        FileService fileService = newFileService();
        FileAsset asset = storedAsset(42L, "cover.png");
        when(fileAssetMapper.selectById(42L)).thenReturn(asset);
        when(fileReferenceMapper.countByRelativePath(asset.getRelativePath())).thenReturn(1L);

        Throwable throwable = catchThrowable(() -> fileService.deleteImage(42L));

        assertThat(throwable).isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) throwable).getResultCode()).isEqualTo(ResultCode.FILE_IN_USE);
    }

    @Test
    void deleteRemovesUnreferencedMetadataAndPhysicalFile() throws Exception {
        FileService fileService = newFileService();
        FileAsset asset = storedAsset(42L, "cover.png");
        Path storedFile = temporaryDirectory.resolve(asset.getRelativePath());
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, pngContent());
        when(fileAssetMapper.selectById(42L)).thenReturn(asset);
        when(fileReferenceMapper.countByRelativePath(asset.getRelativePath())).thenReturn(0L);
        when(fileAssetMapper.deleteById(42L)).thenReturn(1);

        fileService.deleteImage(42L);

        verify(fileAssetMapper).deleteById(42L);
        assertThat(storedFile).doesNotExist();
    }

    private FileService newFileService() {
        UploadProperties properties = new UploadProperties();
        properties.setRootDir(temporaryDirectory);
        return new FileService(fileAssetMapper, fileReferenceMapper, properties);
    }

    private FileAsset storedAsset(Long id, String filename) {
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setScope("blog-images");
        asset.setStoredFilename(filename);
        asset.setRelativePath("blog-images/" + filename);
        return asset;
    }

    private byte[] pngContent() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
