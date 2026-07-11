package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 关于页面 VO。
 */
@Schema(description = "关于页面内容")
public record AboutVo(
        @Schema(description = "页面标题") String title,
        @Schema(description = "页面简介") String description,
        @Schema(description = "页面正文") String content
) {
}
