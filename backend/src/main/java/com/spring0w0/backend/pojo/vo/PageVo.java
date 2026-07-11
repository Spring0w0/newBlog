package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 统一分页响应。
 */
@Schema(description = "分页结果")
public record PageVo<T>(
        @Schema(description = "当前页数据") List<T> items,
        @Schema(description = "当前页码，从 1 开始", example = "1") long page,
        @Schema(description = "每页数量", example = "20") long pageSize,
        @Schema(description = "总记录数", example = "36") long total,
        @Schema(description = "总页数", example = "2") long totalPages
) {
}
