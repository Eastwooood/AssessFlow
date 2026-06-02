package com.bless.assess.exception;

import com.bless.assess.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getErrorCode(), e.getMessage());
        return ApiResult.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null 
                ? e.getBindingResult().getFieldError().getDefaultMessage() 
                : "参数校验失败";
        return ApiResult.fail("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleBindException(BindException e) {
        String message = e.getFieldError() != null 
                ? e.getFieldError().getDefaultMessage() 
                : "参数绑定失败";
        return ApiResult.fail("BIND_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<?> handleException(Exception e) {
        log.error("系统内部异常", e);
        return ApiResult.fail("SYSTEM_ERROR", "系统繁忙，请稍后重试");
    }
}
