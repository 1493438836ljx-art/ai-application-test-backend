package com.huawei.cloudopenlabs.agent.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Agent 统一错误响应
 * <p>
 * 提供用户友好的错误信息，包含错误码、消息、详情、时间戳和追踪ID
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentErrorResponse {

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误消息（用户友好）
     */
    private String message;

    /**
     * 错误详情（可选）
     */
    private String detail;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 追踪ID（用于日志关联）
     */
    private String traceId;

    /**
     * 是否为降级响应
     */
    private Boolean degraded;

    public AgentErrorResponse(String code, String message) {
        this(code, message, null, System.currentTimeMillis(), generateTraceId(), null);
    }

    public AgentErrorResponse(String code, String message, String detail) {
        this(code, message, detail, System.currentTimeMillis(), generateTraceId(), null);
    }

    /**
     * 生成追踪ID
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 创建降级响应
     */
    public static AgentErrorResponse degraded(String message) {
        AgentErrorResponse response = new AgentErrorResponse(
                AgentErrorCode.INTERNAL_ERROR.getCode(),
                message,
                null,
                System.currentTimeMillis(),
                generateTraceId(),
                true
        );
        return response;
    }

    /**
     * 从错误码创建响应
     */
    public static AgentErrorResponse from(AgentErrorCode errorCode) {
        return errorCode.toResponse();
    }

    /**
     * 从错误码创建响应（带详情）
     */
    public static AgentErrorResponse from(AgentErrorCode errorCode, String detail) {
        return errorCode.toResponse(detail);
    }

    /**
     * 从异常创建响应
     */
    public static AgentErrorResponse from(Throwable e) {
        // 根据异常类型选择错误码
        AgentErrorCode errorCode = mapExceptionToCode(e);
        return new AgentErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                e.getMessage(),
                System.currentTimeMillis(),
                generateTraceId(),
                null
        );
    }

    /**
     * 将异常映射到错误码
     */
    private static AgentErrorCode mapExceptionToCode(Throwable e) {
        String className = e.getClass().getSimpleName();

        if (className.contains("Timeout") || className.contains("TimeoutException")) {
            return AgentErrorCode.TIMEOUT;
        }
        if (className.contains("Network") || className.contains("Connection")) {
            return AgentErrorCode.NETWORK_ERROR;
        }
        if (className.contains("RateLimit") || className.contains("TooManyRequests")) {
            return AgentErrorCode.RATE_LIMIT_EXCEEDED;
        }
        if (className.contains("Invalid") || className.contains("Validation")) {
            return AgentErrorCode.INVALID_REQUEST;
        }

        return AgentErrorCode.INTERNAL_ERROR;
    }

    /**
     * 转换为 SSE 事件字符串
     */
    public String toSseEvent() {
        return String.format(
                "event: error\ndata: {\"code\":\"%s\",\"message\":\"%s\",\"traceId\":\"%s\"}\n\n",
                code, escapeJson(message), traceId
        );
    }

    /**
     * 转换为 SSE 事件字符串（带详情）
     */
    public String toSseEventWithDetail() {
        String detailPart = detail != null ? ",\"detail\":\"" + escapeJson(detail) + "\"" : "";
        return String.format(
                "event: error\ndata: {\"code\":\"%s\",\"message\":\"%s\",\"traceId\":\"%s\"%s}\n\n",
                code, escapeJson(message), traceId, detailPart
        );
    }

    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 转换为用户友好的消息
     */
    public String toUserFriendlyMessage() {
        if (degraded != null && degraded) {
            return "服务暂时降级运行：" + message;
        }

        // 根据错误码提供更友好的提示
        AgentErrorCode errorCode = AgentErrorCode.fromCode(code);

        return switch (errorCode) {
            case TIMEOUT -> "操作超时，请稍后重试";
            case NETWORK_ERROR -> "网络连接异常，请检查网络后重试";
            case RATE_LIMIT_EXCEEDED -> "请求过于频繁，请稍后重试";
            case CONCURRENT_LIMIT -> "系统繁忙，请稍后重试";
            case SESSION_BUSY -> "会话正在处理中，请等待完成后再试";
            case SESSION_NOT_FOUND, SESSION_EXPIRED -> "会话已失效，请重新开始";
            default -> message;
        };
    }
}
