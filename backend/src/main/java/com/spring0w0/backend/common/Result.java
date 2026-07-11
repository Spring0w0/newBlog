package com.spring0w0.backend.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 的统一响应结构。
 *
 * @param <T> 响应数据类型
 */
@Schema(description = "统一 API 响应结构")
public record Result<T>(
        @Schema(description = "业务状态码", example = "200") int code,
        @Schema(description = "响应提示", example = "Success") String message,
        @Schema(description = "响应数据；失败时为 null") T data
) {

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.code(), message, data);
    }

    public static <T> Result<T> success(ResultCode resultCode, T data) {
        return new Result<>(resultCode.code(), resultCode.message(), data);
    }

    public static <T> Result<T> failure(ResultCode resultCode) {
        return new Result<>(resultCode.code(), resultCode.message(), null);
    }

    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        return new Result<>(resultCode.code(), message, null);
    }
}
