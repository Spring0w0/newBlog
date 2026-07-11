package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端创建或更新文章的请求体。
 */
@Schema(description = "管理端文章保存请求")
public record BlogWriteRequest(
        @Schema(description = "文章标题", example = "我的第一篇文章")
        @NotBlank(message = "文章标题不能为空")
        @Size(max = 500, message = "文章标题不能超过 500 个字符") String title,

        @Schema(description = "公开访问使用的 slug，只允许小写字母、数字和连字符", example = "my-first-post")
        @NotBlank(message = "文章 slug 不能为空")
        @Size(min = 3, max = 120, message = "文章 slug 长度必须在 3 到 120 个字符之间")
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "文章 slug 只能包含小写字母、数字和连字符") String slug,

        @Schema(description = "Markdown 正文")
        @NotBlank(message = "文章正文不能为空") String markdown,

        @Schema(description = "文章摘要")
        @Size(max = 10000, message = "文章摘要不能超过 10000 个字符") String summary,

        @Schema(description = "文章标签", example = "[\"Java\", \"Spring Boot\"]")
        @Size(max = 20, message = "文章标签不能超过 20 个") List<
                @NotBlank(message = "文章标签不能为空")
                @Size(max = 50, message = "单个文章标签不能超过 50 个字符") String> tags,

        @Schema(description = "文章分类；为空时表示未分类", example = "后端")
        @Size(max = 100, message = "文章分类不能超过 100 个字符") String category,

        @Schema(description = "封面公开 URL；受管图片必须来自 blog-images 范围", example = "http://localhost:8080/images/blog-images/20260711-a1b2c3d4.png")
        @Size(max = 1000, message = "文章封面 URL 不能超过 1000 个字符") String cover,

        @Schema(description = "是否对公开访客隐藏", example = "false") Boolean hidden,

        @Schema(description = "发布时间，ISO-8601 格式", example = "2026-07-11T12:00:00") LocalDateTime publishedAt
) {
}
