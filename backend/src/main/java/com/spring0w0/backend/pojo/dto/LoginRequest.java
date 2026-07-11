package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录接口的入参 DTO。
 */
@Schema(description = "账号密码登录请求")
public record LoginRequest(
        @Schema(description = "管理员用户名", example = "admin")
        @NotBlank(message = "用户名不能为空") String username,
        @Schema(description = "管理员密码", format = "password", example = "admin")
        @NotBlank(message = "密码不能为空") String password
) {
}
