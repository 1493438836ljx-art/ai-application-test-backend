package com.example.demo.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 用于封装业务逻辑中发生的可预期异常，包含错误码和错误消息。
 * 配合{@link GlobalExceptionHandler}统一处理异常响应。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    /** 错误消息 */
    private final String message;

    /**
     * 使用错误码枚举构造业务异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 使用错误码枚举和自定义消息构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }

    /**
     * 使用错误码和消息构造业务异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 静态工厂方法 - 使用错误码枚举创建异常
     *
     * @param errorCode 错误码枚举
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    /**
     * 静态工厂方法 - 使用错误码枚举和自定义消息创建异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode, String message) {
        return new BusinessException(errorCode, message);
    }

    /**
     * 静态工厂方法 - 使用错误码和消息创建异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 业务异常实例
     */
    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }
}
