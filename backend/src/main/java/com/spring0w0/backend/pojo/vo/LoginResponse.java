package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录接口的出参 VO。
 */
@Schema(description = "登录成功响应")
public record LoginResponse(
        @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.signature") String accessToken
) {
}
