package com.spring0w0.backend.pojo.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/** 站点设置整体保存后的标准化结果。 */
@Schema(description = "站点设置与卡片样式保存结果")
public record SiteSettingsVo(
        @Schema(description = "标准化后的站点配置") JsonNode config,
        @Schema(description = "标准化后的卡片样式") JsonNode cardStyles
) {
}
