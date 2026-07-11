package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公开文章详情 VO。
 */
@Schema(description = "公开文章详情")
public record BlogDetailVo(
        @Schema(description = "文章 URL 标识", example = "auto-tool") String slug,
        @Schema(description = "Markdown 正文") String markdown,
        @Schema(description = "文章摘要配置") BlogSummaryVo config
) {
}
