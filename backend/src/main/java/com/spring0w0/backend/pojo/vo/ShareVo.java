package com.spring0w0.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 友链列表项 VO。
 */
@Schema(description = "友链列表项")
public record ShareVo(
        @Schema(description = "站点名称") String name,
        @Schema(description = "Logo 公开 URL") String logo,
        @Schema(description = "站点 URL") String url,
        @Schema(description = "站点说明") String description,
        @Schema(description = "友链标签") List<String> tags,
        @Schema(description = "星标数量") int stars
) {
}
