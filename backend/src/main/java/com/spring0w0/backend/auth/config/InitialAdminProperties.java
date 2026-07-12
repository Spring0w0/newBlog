package com.spring0w0.backend.auth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 首个管理员仅在 users 表为空时创建；生产环境必须由环境变量提供，不使用固定默认密码。
 */
@Validated
@ConfigurationProperties(prefix = "app.security.initial-admin")
public record InitialAdminProperties(
        @NotBlank String username,
        @NotBlank String password
) {
}
