package com.spring0w0.backend.exception;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.common.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 将控制器及参数校验异常转换为统一的 API 响应。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = firstFieldErrorMessage(exception.getBindingResult().getFieldErrors());
        log.warn("请求参数校验失败，参数信息：message={}", message);
        return badRequest(message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException exception) {
        String message = firstFieldErrorMessage(exception.getBindingResult().getFieldErrors());
        log.warn("请求参数绑定失败，参数信息：message={}", message);
        return badRequest(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        log.warn("请求参数约束校验失败，参数信息：message={}", exception.getMessage());
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        log.warn("请求体无法解析，请求参数：已省略原始请求体");
        return badRequest("请求体格式错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        log.warn("上传文件超过大小限制");
        return response(ResultCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<Result<Void>> handleMultipartException(Exception exception) {
        log.warn("上传请求参数不完整或格式错误，原始请求内容已省略");
        return badRequest("上传请求必须包含有效的文件字段");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        log.warn("请求资源不存在，请求参数：method={}，path={}", exception.getHttpMethod(), exception.getResourcePath());
        return response(ResultCode.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        log.warn("业务请求被拒绝，返回参数：code={}，message={}", exception.getResultCode().code(), exception.getMessage());
        return response(exception.getResultCode(), exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException exception) {
        log.warn("数据库唯一约束冲突");
        return response(ResultCode.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("数据库完整性约束冲突");
        return response(ResultCode.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        log.error("发生未处理的服务端异常", exception);
        return response(ResultCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        String resolvedMessage = message == null || message.isBlank() ? ResultCode.BAD_REQUEST.message() : message;
        return response(ResultCode.BAD_REQUEST, resolvedMessage);
    }

    private ResponseEntity<Result<Void>> response(ResultCode resultCode) {
        return ResponseEntity.status(resultCode.code()).body(Result.failure(resultCode));
    }

    private ResponseEntity<Result<Void>> response(ResultCode resultCode, String message) {
        return ResponseEntity.status(resultCode.code()).body(Result.failure(resultCode, message));
    }

    private String firstFieldErrorMessage(Iterable<FieldError> fieldErrors) {
        for (FieldError fieldError : fieldErrors) {
            if (fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
                return fieldError.getDefaultMessage();
            }
            return "%s 参数不合法".formatted(fieldError.getField());
        }
        return "请求参数不合法";
    }
}
