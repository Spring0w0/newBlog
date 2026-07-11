package com.spring0w0.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端相册分组保存请求。 */
@Schema(description = "相册分组保存请求")
public record PictureWriteRequest(
        @Schema(description = "分组上传时间；创建时省略则使用服务端当前时间") LocalDateTime uploadedAt,
        @Size(max = 10000, message = "相册说明不能超过 10000 个字符") String description,
        @NotEmpty(message = "相册分组至少需要一张图片") @Size(max = 50, message = "相册分组最多包含 50 张图片")
        List<@NotBlank(message = "相册图片 URL 不能为空") @Size(max = 1000, message = "相册图片 URL 不能超过 1000 个字符") String> images
) {
}
