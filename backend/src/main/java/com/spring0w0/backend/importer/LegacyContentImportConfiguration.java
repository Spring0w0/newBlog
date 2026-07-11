package com.spring0w0.backend.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;

/**
 * 仅在显式设置 app.legacy-import.enabled=true 时执行的静态内容导入工具。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.legacy-import", name = "enabled", havingValue = "true")
public class LegacyContentImportConfiguration {

    @Bean
    ApplicationRunner legacyContentImportRunner(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            @Value("${app.legacy-import.source-root:}") String sourceRoot
    ) {
        return arguments -> {
            if (sourceRoot.isBlank()) {
                throw new IllegalStateException("启用旧内容导入时必须设置 app.legacy-import.source-root");
            }

            Path sourcePath = Path.of(sourceRoot).toAbsolutePath().normalize();
            LegacyContentImporter importer = new LegacyContentImporter(jdbcTemplate, objectMapper);
            transactionTemplate.executeWithoutResult(status -> importer.importAll(sourcePath));
            log.info("历史前端内容导入完成，请求参数：sourcePath={}", sourcePath);
        };
    }
}
