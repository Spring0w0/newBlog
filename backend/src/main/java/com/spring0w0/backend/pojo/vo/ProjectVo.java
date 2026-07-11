package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 项目列表项 VO。
 */
@Schema(description = "项目列表项")
public record ProjectVo(
        @Schema(description = "项目名称") String name,
        @Schema(description = "项目年份") Integer year,
        @Schema(description = "项目说明") String description,
        @Schema(description = "项目图片公开 URL") String image,
        @Schema(description = "项目访问 URL") String url,
        @Schema(description = "项目标签") List<String> tags,
        @Schema(description = "GitHub URL") String github,
        @Schema(description = "NPM URL") String npm
) {
}
