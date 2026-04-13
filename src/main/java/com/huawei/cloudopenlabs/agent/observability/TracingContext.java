/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 分布式追踪上下文
 * <p>
 * 管理请求的追踪信息，支持分布式链路追踪
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.4.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class TracingContext {

    private static final ThreadLocal<TraceInfo> TRACE_HOLDER = new ThreadLocal<>();

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";

    /**
     * 开始追踪
     *
     * @param traceId 追踪ID（可选，为空则自动生成）
     * @return 追踪信息
     */
    public TraceInfo startTrace(String traceId) {
        String actualTraceId = traceId != null ? traceId : generateTraceId();
        TraceInfo info = new TraceInfo(actualTraceId, generateSpanId(), System.currentTimeMillis());
        TRACE_HOLDER.set(info);
        log.debug("开始追踪: traceId={}", actualTraceId);
        return info;
    }

    /**
     * 获取当前追踪信息
     */
    public TraceInfo getCurrentTrace() {
        return TRACE_HOLDER.get();
    }

    /**
     * 获取追踪ID
     */
    public String getTraceId() {
        TraceInfo info = getCurrentTrace();
        return info != null ? info.traceId() : "no-trace";
    }

    /**
     * 获取SpanID
     */
    public String getSpanId() {
        TraceInfo info = getCurrentTrace();
        return info != null ? info.spanId() : "no-span";
    }

    /**
     * 结束追踪
     */
    public void endTrace() {
        TraceInfo info = TRACE_HOLDER.get();
        if (info != null) {
            log.debug("结束追踪: traceId={}, duration={}ms",
                    info.traceId(), info.duration());
        }
        TRACE_HOLDER.remove();
    }

    /**
     * 创建子追踪
     *
     * @param operation 操作名称
     * @return 子追踪信息
     */
    public TraceInfo createChildSpan(String operation) {
        TraceInfo parent = getCurrentTrace();
        if (parent == null) {
            log.warn("无父追踪信息，创建新追踪");
            return startTrace(null);
        }

        String spanId = generateSpanId();
        TraceInfo childInfo = new TraceInfo(
                parent.traceId(),
                spanId,
                System.currentTimeMillis(),
                operation
        );
        TRACE_HOLDER.set(childInfo);
        log.debug("创建子追踪: traceId={}, spanId={}, operation={}",
                parent.traceId(), spanId, operation);
        return childInfo;
    }

    /**
     * 检查是否有活跃追踪
     */
    public boolean hasActiveTrace() {
        return TRACE_HOLDER.get() != null;
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * 生成SpanID
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 追踪信息记录
     */
    public record TraceInfo(
            String traceId,
            String spanId,
            long startTime,
            String operation
    ) {
        public TraceInfo(String traceId, String spanId, long startTime) {
            this(traceId, spanId, startTime, null);
        }

        public long duration() {
            return System.currentTimeMillis() - startTime;
        }
    }
}
