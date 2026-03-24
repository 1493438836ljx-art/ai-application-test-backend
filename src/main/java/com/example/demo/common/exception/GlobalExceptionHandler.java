package com.example.demo.common.exception;

import com.example.demo.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode() + ": " + ex.getMessage()));
    }

    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败: {} - {}", request.getRequestURI(), errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "参数校验失败: " + errorMessage));
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("约束违反: {} - {}", request.getRequestURI(), errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "参数校验失败: " + errorMessage));
    }

    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("缺少请求参数: {} - {}", request.getRequestURI(), ex.getParameterName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "缺少必填参数: " + ex.getParameterName()));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("参数 '%s' 类型不正确，期望类型: %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");

        log.warn("参数类型不匹配: {} - {}", request.getRequestURI(), message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("请求体解析失败: {} - {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "请求体格式不正确，请检查JSON格式"));
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("资源未找到: {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "请求的资源不存在"));
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("运行时异常: {} - {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误: " + ex.getMessage()));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex, HttpServletRequest request) {
        log.error("未处理的异常: {} - {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }
}
