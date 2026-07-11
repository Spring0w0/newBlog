package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 博主列表项 VO。
 */
@Schema(description = "博主列表项")
public record BloggerVo(
        @Schema(description = "博主名称") String name,
        @Schema(description = "头像公开 URL") String avatar,
        @Schema(description = "站点 URL") String url,
        @Schema(description = "简介") String description,
        @Schema(description = "星标数量") int stars,
        @Schema(description = "连接状态") String status
) {
}
