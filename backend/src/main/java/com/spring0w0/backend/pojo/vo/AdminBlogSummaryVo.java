package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 管理端文章列表项，不包含 Markdown 正文。
 */
@Schema(description = "管理端文章列表项")
public record AdminBlogSummaryVo(
        @Schema(description = "文章数据库 ID", example = "12") Long id,
        @Schema(description = "文章 slug", example = "my-first-post") String slug,
        @Schema(description = "文章标题") String title,
        @Schema(description = "文章标签") List<String> tags,
        @Schema(description = "发布时间，ISO-8601 格式") String publishedAt,
        @Schema(description = "文章摘要") String summary,
        @Schema(description = "封面公开 URL") String cover,
        @Schema(description = "是否隐藏") boolean hidden,
        @Schema(description = "文章分类") String category
) {
}
