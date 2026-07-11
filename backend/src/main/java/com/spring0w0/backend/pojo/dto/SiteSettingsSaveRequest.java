package com.spring0w0.backend.pojo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 配置弹窗一次性保存站点设置与卡片样式的请求。 */
@Schema(description = "站点设置与卡片样式整体保存请求")
public record SiteSettingsSaveRequest(
        @NotNull(message = "站点配置不能为空") @Schema(description = "与公开站点配置接口相同的完整 JSON 对象") JsonNode config,
        @NotNull(message = "卡片样式不能为空") @Schema(description = "以卡片 key 为属性名的完整样式对象") JsonNode cardStyles
) {
}
