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

/**
 * 将控制器及参数校验异常转换为统一的 API 响应。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return badRequest(firstFieldErrorMessage(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException exception) {
        return badRequest(firstFieldErrorMessage(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return badRequest("请求体格式错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return response(ResultCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        return response(exception.getResultCode(), exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException exception) {
        return response(ResultCode.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return response(ResultCode.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled server exception", exception);
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
