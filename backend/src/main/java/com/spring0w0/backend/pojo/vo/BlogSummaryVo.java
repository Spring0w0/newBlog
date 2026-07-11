package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 公开文章列表项 VO。
 */
@Schema(description = "公开文章摘要")
public record BlogSummaryVo(
        @Schema(description = "文章 URL 标识", example = "auto-tool") String slug,
        @Schema(description = "文章标题") String title,
        @Schema(description = "文章标签") List<String> tags,
        @Schema(description = "发布时间，ISO-8601 格式") String date,
        @Schema(description = "文章摘要") String summary,
        @Schema(description = "封面公开 URL") String cover,
        @Schema(description = "是否隐藏；公开接口始终为 false") boolean hidden,
        @Schema(description = "文章分类") String category
) {
}
