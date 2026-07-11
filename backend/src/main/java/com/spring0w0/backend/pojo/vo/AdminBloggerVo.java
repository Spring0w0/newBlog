package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理端博主")
public record AdminBloggerVo(Long id, String name, String avatar, String url, String description, int stars, String status) {
}
