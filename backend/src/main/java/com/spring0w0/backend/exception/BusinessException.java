package com.spring0w0.backend.exception;

import com.spring0w0.backend.common.ResultCode;

/**
 * 表示可预期的业务失败，交由全局异常处理器转换为统一响应。
 */
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.message());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
