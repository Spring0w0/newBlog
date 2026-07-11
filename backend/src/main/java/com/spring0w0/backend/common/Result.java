package com.spring0w0.backend.common;

/**
 * API 的统一响应结构。
 *
 * @param <T> 响应数据类型
 */
public record Result<T>(int code, String message, T data) {

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
