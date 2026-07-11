package com.spring0w0.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 全局配置。
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI newBlogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NewBlog API")
                        .version("1.0.0")
                        .description("NewBlog 前后端联调接口文档。除登录与公开读取接口外，后续管理接口均需要 Bearer JWT。"))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("在 Authorization 请求头中填写：Bearer <accessToken>")
                ));
    }
}
