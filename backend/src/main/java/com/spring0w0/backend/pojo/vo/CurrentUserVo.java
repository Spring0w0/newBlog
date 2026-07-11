package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 当前已认证用户的脱敏信息。 */
@Schema(description = "当前认证用户")
public record CurrentUserVo(
        @Schema(description = "用户数据库 ID", example = "1") Long id,
        @Schema(description = "用户名", example = "admin") String username,
        @Schema(description = "角色", example = "ADMIN") String role
) {
}
