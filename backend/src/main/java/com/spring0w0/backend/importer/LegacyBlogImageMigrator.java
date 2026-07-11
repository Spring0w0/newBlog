package com.spring0w0.backend.importer;

import com.spring0w0.backend.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 历史文章图片的一次性、可重复执行迁移器。
 * 数据库存储后端公开相对路径，API 返回时再结合环境公开基础地址生成绝对 URL。
 */
@RequiredArgsConstructor
final class LegacyBlogImageMigrator {

    private static final String BLOG_IMAGE_SCOPE = "blog-images";
    private static final String LEGACY_BLOG_PREFIX = "/blogs/";
    private static final String MANAGED_BLOG_IMAGE_PREFIX = "/images/blog-images/";
    private static final Pattern LEGACY_BLOG_URL_PATTERN = Pattern.compile("/blogs/[A-Za-z0-9._-]+/[A-Za-z0-9._-]+");
    private static final Pattern MARKDOWN_IMAGE_URL_PATTERN = Pattern.compile("!\\[[^\\]]*]\\((?:<)?([^\\s)>]+)(?:>)?(?:\\s+[^)]*)?\\)");

    private final JdbcTemplate jdbcTemplate;
    private final UploadProperties uploadProperties;
    private final Map<String, AssetReference> migratedAssetsByLegacyUrl = new ConcurrentHashMap<>();

    MigrationResult migrate(Path frontendRoot) {
        Path sourcePublicRoot = frontendRoot.resolve("public").normalize();
        Path sourceBlogsRoot = sourcePublicRoot.resolve("blogs").normalize();
        if (!Files.isDirectory(sourceBlogsRoot)) {
            throw new IllegalStateException("历史文章图片目录不存在");
        }

        MigrationCounters counters = new MigrationCounters();
        List<BlogRecord> blogs = jdbcTemplate.query(
                "SELECT id, cover_url, markdown, cover_file_asset_id FROM blog_posts ORDER BY id",
                (resultSet, rowNum) -> new BlogRecord(
                        resultSet.getLong("id"),
                        resultSet.getString("cover_url"),
                        resultSet.getString("markdown"),
                        resultSet.getObject("cover_file_asset_id", Long.class)
                )
        );
        counters.scannedBlogCount = blogs.size();

        for (BlogRecord blog : blogs) {
            String migratedCover = rewriteUrl(blog.coverUrl(), sourcePublicRoot, counters);
            String migratedMarkdown = rewriteMarkdown(blog.markdown(), sourcePublicRoot, counters);
            Long coverAssetId = resolveManagedAssetId(migratedCover);
            boolean changed = !equalsNullable(blog.coverUrl(), migratedCover)
                    || !equalsNullable(blog.markdown(), migratedMarkdown)
                    || !equalsNullable(blog.coverFileAssetId(), coverAssetId);

            if (changed) {
                jdbcTemplate.update(
                        "UPDATE blog_posts SET cover_url = ?, cover_file_asset_id = ?, markdown = ? WHERE id = ?",
                        migratedCover, coverAssetId, migratedMarkdown, blog.id()
                );
                counters.updatedBlogCount++;
            }

            counters.blogImageReferenceCount += rebuildBlogImageReferences(blog.id(), migratedMarkdown);
        }
        return counters.toResult();
    }

    private String rewriteMarkdown(String markdown, Path sourcePublicRoot, MigrationCounters counters) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
        Matcher matcher = MARKDOWN_IMAGE_URL_PATTERN.matcher(markdown);
        StringBuilder result = new StringBuilder(markdown.length());
        int previousEnd = 0;
        while (matcher.find()) {
            result.append(markdown, previousEnd, matcher.start(1));
            result.append(rewriteUrl(matcher.group(1), sourcePublicRoot, counters));
            previousEnd = matcher.end(1);
        }
        if (previousEnd == 0) {
            return markdown;
        }
        result.append(markdown, previousEnd, markdown.length());
        return result.toString();
    }

    private String rewriteUrl(String url, Path sourcePublicRoot, MigrationCounters counters) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (LEGACY_BLOG_URL_PATTERN.matcher(url).matches()) {
            AssetReference asset = migrateLegacyAsset(url, sourcePublicRoot, counters);
            return asset == null ? url : asset.publicPath();
        }
        return toCanonicalManagedPath(url);
    }

    private AssetReference migrateLegacyAsset(String legacyUrl, Path sourcePublicRoot, MigrationCounters counters) {
        AssetReference cached = migratedAssetsByLegacyUrl.get(legacyUrl);
        if (cached != null) {
            return cached;
        }
        Path sourceFile = sourcePublicRoot.resolve(legacyUrl.substring(1)).normalize();
        if (!sourceFile.startsWith(sourcePublicRoot) || !Files.isRegularFile(sourceFile)) {
            counters.skippedFileCount++;
            return null;
        }

        String extension = extension(sourceFile.getFileName().toString());
        String contentType = contentType(extension);
        if (contentType == null) {
            counters.skippedFileCount++;
            return null;
        }

        try {
            String sha256 = sha256(sourceFile);
            AssetReference existing = findAssetBySha256(sha256);
            if (existing != null) {
                migratedAssetsByLegacyUrl.put(legacyUrl, existing);
                return existing;
            }

            String storageExtension = "jpeg".equals(extension) ? "jpg" : extension;
            String storedFilename = "legacy-" + sha256 + "." + storageExtension;
            String relativePath = BLOG_IMAGE_SCOPE + "/" + storedFilename;
            Path targetDirectory = uploadProperties.normalizedRootDir().resolve(BLOG_IMAGE_SCOPE).normalize();
            Path targetFile = targetDirectory.resolve(storedFilename).normalize();
            if (!targetDirectory.startsWith(uploadProperties.normalizedRootDir()) || !targetFile.startsWith(targetDirectory)) {
                throw new IllegalStateException("历史图片目标路径不合法");
            }
            Files.createDirectories(targetDirectory);
            if (!Files.exists(targetFile)) {
                Files.copy(sourceFile, targetFile, StandardCopyOption.COPY_ATTRIBUTES);
            }

            ImageDimensions dimensions = readDimensions(sourceFile);
            long fileSize = Files.size(sourceFile);
            long id = insertFileAsset(storedFilename, relativePath, sourceFile.getFileName().toString(), contentType, fileSize, sha256, dimensions);
            AssetReference asset = new AssetReference(id, uploadProperties.normalizedPublicPath() + "/" + relativePath);
            migratedAssetsByLegacyUrl.put(legacyUrl, asset);
            counters.migratedFileCount++;
            return asset;
        } catch (IOException exception) {
            throw new IllegalStateException("历史文章图片迁移失败", exception);
        }
    }

    private int rebuildBlogImageReferences(long blogId, String markdown) {
        jdbcTemplate.update("DELETE FROM blog_images WHERE blog_id = ?", blogId);
        if (markdown == null || markdown.isBlank()) {
            return 0;
        }
        LinkedHashSet<String> imageUrls = new LinkedHashSet<>();
        Matcher matcher = MARKDOWN_IMAGE_URL_PATTERN.matcher(markdown);
        while (matcher.find()) {
            String url = matcher.group(1);
            if (url != null && !url.isBlank()) {
                imageUrls.add(url.trim());
            }
        }
        int sortOrder = 0;
        for (String url : imageUrls) {
            jdbcTemplate.update(
                    "INSERT INTO blog_images (blog_id, file_asset_id, url, sort_order) VALUES (?, ?, ?, ?)",
                    blogId, resolveManagedAssetId(url), url, sortOrder++
            );
        }
        return imageUrls.size();
    }

    private Long resolveManagedAssetId(String url) {
        String managedPath = toCanonicalManagedPath(url);
        if (managedPath == null || !managedPath.startsWith(MANAGED_BLOG_IMAGE_PREFIX)) {
            return null;
        }
        String relativePath = managedPath.substring("/images/".length());
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM file_assets WHERE scope = ? AND relative_path = ?",
                    Long.class,
                    BLOG_IMAGE_SCOPE,
                    relativePath
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private AssetReference findAssetBySha256(String sha256) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, relative_path FROM file_assets WHERE scope = ? AND sha256 = ? ORDER BY id LIMIT 1",
                    (resultSet, rowNum) -> new AssetReference(
                            resultSet.getLong("id"),
                            uploadProperties.normalizedPublicPath() + "/" + resultSet.getString("relative_path")
                    ),
                    BLOG_IMAGE_SCOPE,
                    sha256
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private long insertFileAsset(
            String storedFilename,
            String relativePath,
            String originalName,
            String contentType,
            long fileSize,
            String sha256,
            ImageDimensions dimensions
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO file_assets (scope, stored_filename, relative_path, original_name, content_type, file_size, sha256, width, height)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, BLOG_IMAGE_SCOPE);
            statement.setString(2, storedFilename);
            statement.setString(3, relativePath);
            statement.setString(4, originalName);
            statement.setString(5, contentType);
            statement.setLong(6, fileSize);
            statement.setString(7, sha256);
            if (dimensions.width() == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, dimensions.width());
            }
            if (dimensions.height() == null) {
                statement.setNull(9, java.sql.Types.INTEGER);
            } else {
                statement.setInt(9, dimensions.height());
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("历史文章图片元数据保存失败");
        }
        return key.longValue();
    }

    private String toCanonicalManagedPath(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        try {
            String path = java.net.URI.create(url.trim()).getPath();
            return path != null && path.startsWith(MANAGED_BLOG_IMAGE_PREFIX) ? path : url;
        } catch (IllegalArgumentException exception) {
            return url;
        }
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String contentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> null;
        };
    }

    private String sha256(Path sourceFile) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(sourceFile)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, count);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private ImageDimensions readDimensions(Path sourceFile) {
        try {
            BufferedImage image = ImageIO.read(sourceFile.toFile());
            return image == null ? new ImageDimensions(null, null) : new ImageDimensions(image.getWidth(), image.getHeight());
        } catch (IOException exception) {
            return new ImageDimensions(null, null);
        }
    }

    private boolean equalsNullable(Object first, Object second) {
        return java.util.Objects.equals(first, second);
    }

    record MigrationResult(int scannedBlogCount, int updatedBlogCount, int migratedFileCount, int blogImageReferenceCount, int skippedFileCount) {
    }

    private record BlogRecord(long id, String coverUrl, String markdown, Long coverFileAssetId) {
    }

    private record AssetReference(long id, String publicPath) {
    }

    private record ImageDimensions(Integer width, Integer height) {
    }

    private static final class MigrationCounters {
        private int scannedBlogCount;
        private int updatedBlogCount;
        private int migratedFileCount;
        private int blogImageReferenceCount;
        private int skippedFileCount;

        private MigrationResult toResult() {
            return new MigrationResult(scannedBlogCount, updatedBlogCount, migratedFileCount, blogImageReferenceCount, skippedFileCount);
        }
    }
}
