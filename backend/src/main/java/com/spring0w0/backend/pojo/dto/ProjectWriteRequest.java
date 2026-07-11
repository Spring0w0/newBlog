package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 管理端项目保存请求。 */
@Schema(description = "项目保存请求")
public record ProjectWriteRequest(
        @NotBlank(message = "项目名称不能为空") @Size(max = 200, message = "项目名称不能超过 200 个字符") String name,
        @Min(value = 1970, message = "项目年份不能早于 1970") @Max(value = 2100, message = "项目年份不能晚于 2100") Integer year,
        @NotBlank(message = "项目说明不能为空") @Size(max = 10000, message = "项目说明不能超过 10000 个字符") String description,
        @NotBlank(message = "项目图片不能为空") @Size(max = 1000, message = "项目图片 URL 不能超过 1000 个字符") String image,
        @NotBlank(message = "项目访问 URL 不能为空") @Size(max = 1000, message = "项目访问 URL 不能超过 1000 个字符") String url,
        @Size(max = 20, message = "项目标签不能超过 20 个") List<@NotBlank(message = "项目标签不能为空") @Size(max = 50, message = "项目标签不能超过 50 个字符") String> tags,
        @Size(max = 1000, message = "GitHub URL 不能超过 1000 个字符") String github,
        @Size(max = 1000, message = "NPM URL 不能超过 1000 个字符") String npm
) {
}
