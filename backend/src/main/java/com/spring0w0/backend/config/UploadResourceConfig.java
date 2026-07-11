package com.spring0w0.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 将工作区 uploads 目录以只读静态资源形式映射到公开 URL。
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UploadProperties.class)
public class UploadResourceConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicPath = uploadProperties.normalizedPublicPath();
        registry.addResourceHandler(publicPath + "/**")
                .addResourceLocations(uploadProperties.normalizedRootDir().toUri().toString());
    }
}
