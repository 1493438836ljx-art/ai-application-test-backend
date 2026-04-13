/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Agent 统一日志管理器
 * <p>
 * 提供结构化的日志记录方法，确保日志格式统一，便于日志分析
 * </p>
 *
 * <h3>日志格式规范：</h3>
 * <pre>
 * [会话ID] [事件类型] key=value, key=value
 * </pre>
 *
 * @author GNEEC LIVE
 * @version 27.0.4.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class AgentLogger {

    private final TracingContext tracingContext;

    @Value("${agent.observability.log-truncate-length:200}")
    private int truncateLength;

    public AgentLogger(TracingContext tracingContext) {
        this.tracingContext = tracingContext;
    }

    /**
     * 记录请求开始
     */
    public void logRequestStart(String sessionId, String query) {
        log.info("[{}] [REQUEST_START] query={}, traceId={}",
                sessionId, truncate(query), tracingContext.getTraceId());
    }

    /**
     * 记录请求结束
     */
    public void logRequestEnd(String sessionId, long durationMs, int roundCount) {
        log.info("[{}] [REQUEST_END] duration={}ms, rounds={}, traceId={}",
                sessionId, durationMs, roundCount, tracingContext.getTraceId());
    }

    /**
     * 记录 CLI 执行
     */
    public void logCliExecution(String sessionId, String command, long durationMs, int exitCode) {
        log.info("[{}] [CLI_EXEC] command={}, duration={}ms, exitCode={}, traceId={}",
                sessionId, sanitize(command), durationMs, exitCode, tracingContext.getTraceId());
    }

    /**
     * 记录 CLI 输出
     */
    public void logCliOutput(String sessionId, String output, boolean isError) {
        String outputType = isError ? "STDERR" : "STDOUT";
        log.debug("[{}] [CLI_{}] output={}, traceId={}",
                sessionId, outputType, truncate(output), tracingContext.getTraceId());
    }

    /**
     * 记录查询执行
     */
    public void logQueryExecution(String sessionId, String query, boolean success) {
        log.info("[{}] [QUERY_EXEC] success={}, query={}, traceId={}",
                sessionId, success, truncate(query), tracingContext.getTraceId());
    }

    /**
     * 记录操作执行
     */
    public void logActionExecution(String sessionId, String action, boolean success) {
        log.info("[{}] [ACTION_EXEC] success={}, action={}, traceId={}",
                sessionId, success, truncate(action), tracingContext.getTraceId());
    }

    /**
     * 记录 SSE 事件
     */
    public void logSseEvent(String sessionId, String eventType) {
        log.debug("[{}] [SSE_EVENT] type={}, traceId={}",
                sessionId, eventType, tracingContext.getTraceId());
    }

    /**
     * 记录缓存操作
     */
    public void logCacheOperation(String sessionId, String operation, String key, boolean hit) {
        log.debug("[{}] [CACHE_{}] key={}, hit={}, traceId={}",
                sessionId, operation.toUpperCase(), key, hit, tracingContext.getTraceId());
    }

    /**
     * 记录重试操作
     */
    public void logRetry(String sessionId, String operation, int attempt, int maxAttempts, Throwable error) {
        log.warn("[{}] [RETRY] operation={}, attempt={}/{}, error={}, traceId={}",
                sessionId, operation, attempt, maxAttempts, error.getMessage(), tracingContext.getTraceId());
    }

    /**
     * 记录错误（带上下文）
     */
    public void logError(String sessionId, String phase, Throwable error) {
        log.error("[{}] [ERROR] phase={}, error={}, traceId={}",
                sessionId, phase, error.getMessage(), tracingContext.getTraceId(), error);
    }

    /**
     * 记录错误（带详情）
     */
    public void logError(String sessionId, String phase, String detail, Throwable error) {
        log.error("[{}] [ERROR] phase={}, detail={}, error={}, traceId={}",
                sessionId, phase, detail, error != null ? error.getMessage() : "null",
                tracingContext.getTraceId(), error);
    }

    /**
     * 记录性能警告
     */
    public void logPerformanceWarning(String sessionId, String operation, long durationMs, long thresholdMs) {
        log.warn("[{}] [PERF_WARN] operation={}, duration={}ms > threshold={}ms, traceId={}",
                sessionId, operation, durationMs, thresholdMs, tracingContext.getTraceId());
    }

    /**
     * 记录性能信息
     */
    public void logPerformance(String sessionId, String operation, long durationMs) {
        log.info("[{}] [PERF] operation={}, duration={}ms, traceId={}",
                sessionId, operation, durationMs, tracingContext.getTraceId());
    }

    /**
     * 记录会话创建
     */
    public void logSessionCreate(String sessionId) {
        log.info("[{}] [SESSION_CREATE] traceId={}",
                sessionId, tracingContext.getTraceId());
    }

    /**
     * 记录会话销毁
     */
    public void logSessionDestroy(String sessionId, String reason) {
        log.info("[{}] [SESSION_DESTROY] reason={}, traceId={}",
                sessionId, reason, tracingContext.getTraceId());
    }

    /**
     * 记录降级事件
     */
    public void logDegradation(String sessionId, String operation, String reason) {
        log.warn("[{}] [DEGRADATION] operation={}, reason={}, traceId={}",
                sessionId, operation, reason, tracingContext.getTraceId());
    }

    /**
     * 截断字符串
     */
    private String truncate(String str) {
        if (str == null) return "null";
        if (str.length() <= truncateLength) return str;
        return str.substring(0, truncateLength) + "...";
    }

    /**
     * 脱敏字符串（移除换行符等）
     */
    private String sanitize(String str) {
        if (str == null) return "null";
        return str
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
