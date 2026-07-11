package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 相册分组 VO。
 */
@Schema(description = "相册图片分组")
public record PictureVo(
        @Schema(description = "图片分组标识") String id,
        @Schema(description = "上传时间，ISO-8601 格式") String uploadedAt,
        @Schema(description = "图片说明") String description,
        @Schema(description = "图片公开 URL 列表") List<String> images
) {
}
