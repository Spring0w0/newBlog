package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 管理端全量保存文章分类及其排序的请求体。
 */
@Schema(description = "文章分类保存请求")
public record CategorySaveRequest(
        @Schema(description = "按展示顺序排列的分类名称", example = "[\"后端\", \"前端\"]")
        @NotNull(message = "分类列表不能为空")
        @Size(max = 100, message = "分类数量不能超过 100 个") List<
                @NotBlank(message = "分类名称不能为空")
                @Size(max = 100, message = "分类名称不能超过 100 个字符") String> categories
) {
}
