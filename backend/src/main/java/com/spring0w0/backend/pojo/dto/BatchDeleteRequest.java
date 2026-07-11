package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 管理端批量删除文章请求。
 */
@Schema(description = "批量删除文章请求")
public record BatchDeleteRequest(
        @Schema(description = "待删除文章的数据库 ID", example = "[12, 13]")
        @NotEmpty(message = "至少需要选择一篇文章")
        @Size(max = 100, message = "单次最多删除 100 篇文章") List<
                @Positive(message = "文章 ID 必须为正整数") Long> ids
) {
}
