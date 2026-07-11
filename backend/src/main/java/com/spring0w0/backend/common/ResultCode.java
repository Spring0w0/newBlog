package com.spring0w0.backend.common;

/**
 * API 响应使用的标准状态码和默认提示。
 */
public enum ResultCode {
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "请求参数不合法"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    FILE_TOO_LARGE(413, "上传文件超出大小限制"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的文件类型"),
    FILE_IN_USE(422, "文件仍被业务数据引用，不能删除"),
    LOGIN_FAILED(401, "用户名或密码错误"),
    ACCOUNT_DISABLED(403, "账号已被禁用"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
