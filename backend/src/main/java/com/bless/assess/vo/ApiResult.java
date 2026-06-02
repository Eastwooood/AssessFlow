package com.bless.assess.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    
    private int code;
    private String message;
    private T data;
    
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }
    
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "success", null);
    }
    
    public static <T> ApiResult<T> fail(String errorCode, String message) {
        return new ApiResult<>(-1, message, null);
    }
}
