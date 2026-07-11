package com.spring0w0.backend.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;

/**
 * 运行期图片存储配置。目录位于工作区根目录的 uploads 下，不能指向源码目录。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private Path rootDir = Path.of("uploads");
    private String publicPath = "/images";
    @NotBlank(message = "必须配置 app.upload.public-base-url")
    private String publicBaseUrl;
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

    /**
     * 面向浏览器的后端公开基础地址。它必须由环境配置提供，不能从单次请求的 Host 头推导，
     * 以避免反向代理、内网地址或 localhost 被持久化到业务数据中。
     */
    public String normalizedPublicBaseUrl() {
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("必须配置 app.upload.public-base-url");
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalStateException("app.upload.public-base-url 必须以 http:// 或 https:// 开头");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String buildPublicUrl(String publicPath) {
        String normalizedPath = publicPath == null ? "" : publicPath.trim();
        if (normalizedPath.isBlank()) {
            throw new IllegalArgumentException("公开资源路径不能为空");
        }
        String path = normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
        return UriComponentsBuilder.fromUriString(normalizedPublicBaseUrl())
                .path(path)
                .toUriString();
    }
}
