package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 管理端图片上传成功后的公开可引用信息。
 */
@Schema(description = "图片上传结果")
public record ImageUploadVo(
        @Schema(description = "文件元数据 ID，用于后续删除", example = "42") Long fileId,
        @Schema(description = "可直接访问的图片绝对 URL", example = "http://localhost:8080/images/blog-images/20260711-a1b2c3.webp") String url,
        @Schema(description = "服务端生成的文件名", example = "20260711-a1b2c3.webp") String fileName,
        @Schema(description = "已清理路径信息的原始文件名", example = "cover.webp") String originalName,
        @Schema(description = "文件大小，单位为字节", example = "182304") long size,
        @Schema(description = "服务端确认的图片 MIME 类型", example = "image/webp") String contentType
) {
}
