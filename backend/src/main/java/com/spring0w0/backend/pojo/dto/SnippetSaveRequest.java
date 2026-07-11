package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 代码片段整体保存请求。
 */
@Schema(description = "代码片段列表保存请求")
public record SnippetSaveRequest(
        @Schema(description = "按展示顺序排列的片段列表")
        @NotEmpty(message = "至少需要保留一条代码片段")
        @Size(max = 200, message = "代码片段数量不能超过 200 条") List<
                @NotBlank(message = "代码片段不能为空")
                @Size(max = 5000, message = "单条代码片段不能超过 5000 个字符") String> items
) {
}
