package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "管理端相册分组")
public record AdminPictureVo(String id, String uploadedAt, String description, List<String> images) {
}
