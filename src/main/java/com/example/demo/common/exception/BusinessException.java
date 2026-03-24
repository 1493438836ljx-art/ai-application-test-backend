package com.example.demo.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = 400;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 400;
    }

    public BusinessException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = 400;
    }

    /**
     * 资源未找到异常
     */
    public static BusinessException notFound(String resource, Object id) {
        return new BusinessException("RESOURCE_NOT_FOUND",
                String.format("%s不存在: %s", resource, id), 404);
    }

    /**
     * 参数无效异常
     */
    public static BusinessException invalidParam(String message) {
        return new BusinessException("INVALID_PARAMETER", message, 400);
    }

    /**
     * 操作不允许异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message, 403);
    }

    /**
     * 冲突异常
     */
    public static BusinessException conflict(String message) {
        return new BusinessException("CONFLICT", message, 409);
    }
}
