package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 关于页面整体保存请求。
 */
@Schema(description = "关于页面保存请求")
public record AboutSaveRequest(
        @Schema(description = "页面标题")
        @NotBlank(message = "关于页标题不能为空")
        @Size(max = 200, message = "关于页标题不能超过 200 个字符") String title,

        @Schema(description = "页面简介")
        @NotBlank(message = "关于页简介不能为空")
        @Size(max = 2000, message = "关于页简介不能超过 2000 个字符") String description,

        @Schema(description = "Markdown 正文")
        @NotBlank(message = "关于页正文不能为空")
        @Size(max = 200000, message = "关于页正文不能超过 200000 个字符") String content
) {
}
