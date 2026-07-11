package com.spring0w0.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

/**
 * 运行期图片存储配置。目录位于工作区根目录的 uploads 下，不能指向源码目录。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private Path rootDir = Path.of("uploads");
    private String publicPath = "/images";
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    public Path normalizedRootDir() {
        return rootDir.toAbsolutePath().normalize();
    }

    public String normalizedPublicPath() {
        String path = publicPath == null ? "/images" : publicPath.trim();
        if (path.isBlank()) {
            return "/images";
        }
        path = path.startsWith("/") ? path : "/" + path;
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
