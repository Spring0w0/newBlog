package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 管理端友链保存请求。 */
@Schema(description = "友链保存请求")
public record ShareWriteRequest(
        @NotBlank(message = "友链名称不能为空") @Size(max = 200, message = "友链名称不能超过 200 个字符") String name,
        @NotBlank(message = "友链 Logo 不能为空") @Size(max = 1000, message = "友链 Logo URL 不能超过 1000 个字符") String logo,
        @NotBlank(message = "友链 URL 不能为空") @Size(max = 1000, message = "友链 URL 不能超过 1000 个字符") String url,
        @NotBlank(message = "友链说明不能为空") @Size(max = 10000, message = "友链说明不能超过 10000 个字符") String description,
        @Size(max = 20, message = "友链标签不能超过 20 个") List<@NotBlank(message = "友链标签不能为空") @Size(max = 50, message = "友链标签不能超过 50 个字符") String> tags,
        @Min(value = 0, message = "友链星级不能小于 0") @Max(value = 5, message = "友链星级不能超过 5") Integer stars
) {
}
