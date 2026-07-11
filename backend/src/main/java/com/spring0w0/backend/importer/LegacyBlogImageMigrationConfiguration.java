package com.spring0w0.backend.importer;

import com.spring0w0.backend.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;

/**
 * 仅在显式开启时，将历史文章目录中的图片迁移为后端受管图片。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.legacy-blog-image-migration", name = "enabled", havingValue = "true")
public class LegacyBlogImageMigrationConfiguration {

    @Bean
    ApplicationRunner legacyBlogImageMigrationRunner(
            JdbcTemplate jdbcTemplate,
            UploadProperties uploadProperties,
            CacheManager cacheManager,
            TransactionTemplate transactionTemplate,
            @Value("${app.legacy-blog-image-migration.source-root:}") String sourceRoot
    ) {
        return arguments -> {
            if (sourceRoot.isBlank()) {
                throw new IllegalStateException("启用历史文章图片迁移时必须设置 app.legacy-blog-image-migration.source-root");
            }

            Path sourcePath = Path.of(sourceRoot).toAbsolutePath().normalize();
            LegacyBlogImageMigrator migrator = new LegacyBlogImageMigrator(jdbcTemplate, uploadProperties);
            LegacyBlogImageMigrator.MigrationResult result = transactionTemplate.execute(status -> migrator.migrate(sourcePath));
            evictBlogCaches(cacheManager);
            log.info("历史文章图片迁移完成，返回参数：扫描文章数={}，更新文章数={}，迁移文件数={}，正文图片引用数={}，跳过文件数={}",
                    result.scannedBlogCount(), result.updatedBlogCount(), result.migratedFileCount(), result.blogImageReferenceCount(), result.skippedFileCount());
        };
    }

    private void evictBlogCaches(CacheManager cacheManager) {
        for (String cacheName : new String[]{"publishedBlogs", "publishedBlog", "blogCategories"}) {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
            }
        }
    }
}
