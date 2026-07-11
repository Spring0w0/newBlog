package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理端博主保存请求。 */
@Schema(description = "博主保存请求")
public record BloggerWriteRequest(
        @NotBlank(message = "博主名称不能为空") @Size(max = 200, message = "博主名称不能超过 200 个字符") String name,
        @NotBlank(message = "博主头像不能为空") @Size(max = 1000, message = "博主头像 URL 不能超过 1000 个字符") String avatar,
        @NotBlank(message = "博主站点 URL 不能为空") @Size(max = 1000, message = "博主站点 URL 不能超过 1000 个字符") String url,
        @NotBlank(message = "博主简介不能为空") @Size(max = 10000, message = "博主简介不能超过 10000 个字符") String description,
        @Min(value = 0, message = "博主星级不能小于 0") @Max(value = 5, message = "博主星级不能超过 5") Integer stars,
        @Pattern(regexp = "recent|disconnected", message = "博主状态只能是 recent 或 disconnected") String status
) {
}
