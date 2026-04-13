/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.observability;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 指标采集器
 * <p>
 * 使用 Micrometer 采集各类指标，支持 Prometheus 集成
 * </p>
 *
 * <h3>指标类型：</h3>
 * <ul>
 *   <li>Counter - 请求计数（总数、成功、失败、超时）</li>
 *   <li>Timer - 执行耗时（请求、CLI、查询、操作）</li>
 *   <li>Gauge - 实时状态（活跃会话、活跃连接）</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.4.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class AgentMetrics {

    private final MeterRegistry meterRegistry;

    // 计数器
    private final Counter requestCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter timeoutCounter;
    private final Counter retryCounter;

    // 计时器
    private final Timer requestTimer;
    private final Timer cliExecutionTimer;
    private final Timer queryExecutionTimer;
    private final Timer actionExecutionTimer;

    // 仪表盘
    private final AtomicInteger activeSessions = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public AgentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化计数器
        this.requestCounter = Counter.builder("agent.requests.total")
                .description("Total number of agent requests")
                .register(meterRegistry);

        this.successCounter = Counter.builder("agent.requests.success")
                .description("Number of successful requests")
                .register(meterRegistry);

        this.failureCounter = Counter.builder("agent.requests.failure")
                .description("Number of failed requests")
                .tag("error_code", "unknown")
                .register(meterRegistry);

        this.timeoutCounter = Counter.builder("agent.requests.timeout")
                .description("Number of timed out requests")
                .register(meterRegistry);

        this.retryCounter = Counter.builder("agent.retry.attempts")
                .description("Number of retry attempts")
                .register(meterRegistry);

        // 初始化计时器
        this.requestTimer = Timer.builder("agent.request.duration")
                .description("Request duration")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMinutes(10))
                .register(meterRegistry);

        this.cliExecutionTimer = Timer.builder("agent.cli.duration")
                .description("CLI execution duration")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMinutes(5))
                .register(meterRegistry);

        this.queryExecutionTimer = Timer.builder("agent.query.duration")
                .description("Query execution duration")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(meterRegistry);

        this.actionExecutionTimer = Timer.builder("agent.action.duration")
                .description("Action execution duration")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(meterRegistry);

        // 注册仪表盘
        Gauge.builder("agent.sessions.active", activeSessions, AtomicInteger::get)
                .description("Number of active sessions")
                .register(meterRegistry);

        Gauge.builder("agent.connections.active", activeConnections, AtomicInteger::get)
                .description("Number of active SSE connections")
                .register(meterRegistry);

        log.info("Agent metrics collector initialized");
    }

    // ==================== 请求计数 ====================

    /**
     * 记录请求
     */
    public void recordRequest() {
        requestCounter.increment();
        activeSessions.incrementAndGet();
    }

    /**
     * 记录成功
     */
    public void recordSuccess() {
        successCounter.increment();
        activeSessions.decrementAndGet();
    }

    /**
     * 记录失败
     */
    public void recordFailure() {
        failureCounter.increment();
        activeSessions.decrementAndGet();
    }

    /**
     * 记录失败（带错误码）
     *
     * @param errorCode 错误码
     */
    public void recordFailure(String errorCode) {
        Counter.builder("agent.requests.failure")
                .description("Number of failed requests")
                .tag("error_code", errorCode)
                .register(meterRegistry)
                .increment();
        activeSessions.decrementAndGet();
    }

    /**
     * 记录超时
     */
    public void recordTimeout() {
        timeoutCounter.increment();
        activeSessions.decrementAndGet();
    }

    /**
     * 记录重试
     */
    public void recordRetry() {
        retryCounter.increment();
    }

    /**
     * 记录重试（带操作类型）
     *
     * @param operation 操作类型
     */
    public void recordRetry(String operation) {
        Counter.builder("agent.retry.attempts")
                .description("Number of retry attempts")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }

    // ==================== 耗时记录 ====================

    /**
     * 开始计时
     *
     * @return Timer采样对象
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 记录请求耗时
     *
     * @param sample Timer采样对象
     */
    public void recordRequestDuration(Timer.Sample sample) {
        sample.stop(requestTimer);
    }

    /**
     * 记录请求耗时（直接传入时间）
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordRequestDuration(long durationMs) {
        requestTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 CLI 执行耗时
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordCliDuration(long durationMs) {
        cliExecutionTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 CLI 执行耗时（带命令标签）
     *
     * @param durationMs 耗时（毫秒）
     * @param command    CLI 命令名称
     */
    public void recordCliDuration(long durationMs, String command) {
        Timer.builder("agent.cli.duration")
                .description("CLI execution duration")
                .tag("command", command)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录查询执行耗时
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordQueryDuration(long durationMs) {
        queryExecutionTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录查询执行耗时（带查询类型）
     *
     * @param durationMs 耗时（毫秒）
     * @param queryType  查询类型
     */
    public void recordQueryDuration(long durationMs, String queryType) {
        Timer.builder("agent.query.duration")
                .description("Query execution duration")
                .tag("query_type", queryType)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录操作执行耗时
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordActionDuration(long durationMs) {
        actionExecutionTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录操作执行耗时（带操作类型）
     *
     * @param durationMs 耗时（毫秒）
     * @param actionType 操作类型
     */
    public void recordActionDuration(long durationMs, String actionType) {
        Timer.builder("agent.action.duration")
                .description("Action execution duration")
                .tag("action_type", actionType)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ==================== 连接状态 ====================

    /**
     * 记录活跃连接增加
     */
    public void recordConnectionIncrement() {
        activeConnections.incrementAndGet();
    }

    /**
     * 记录活跃连接减少
     */
    public void recordConnectionDecrement() {
        activeConnections.decrementAndGet();
    }

    /**
     * 设置活跃连接数
     *
     * @param count 活跃连接数
     */
    public void setActiveConnections(int count) {
        activeConnections.set(count);
    }

    // ==================== 统计信息 ====================

    /**
     * 获取活跃会话数
     *
     * @return 活跃会话数
     */
    public int getActiveSessionCount() {
        return activeSessions.get();
    }

    /**
     * 获取活跃连接数
     *
     * @return 活跃连接数
     */
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }

    /**
     * 获取请求总数
     *
     * @return 请求总数
     */
    public double getRequestCount() {
        return requestCounter.count();
    }

    /**
     * 获取成功数
     *
     * @return 成功请求数
     */
    public double getSuccessCount() {
        return successCounter.count();
    }

    /**
     * 获取失败数
     *
     * @return 失败请求数
     */
    public double getFailureCount() {
        return failureCounter.count();
    }

    /**
     * 获取超时数
     *
     * @return 超时请求数
     */
    public double getTimeoutCount() {
        return timeoutCounter.count();
    }

    /**
     * 计算成功率
     *
     * @return 成功率（0.0~1.0）
     */
    public double getSuccessRate() {
        double total = requestCounter.count();
        if (total == 0) return 1.0;
        return successCounter.count() / total;
    }

    /**
     * 计算错误率
     *
     * @return 错误率（0.0~1.0）
     */
    public double getErrorRate() {
        double total = requestCounter.count();
        if (total == 0) return 0.0;
        return (failureCounter.count() + timeoutCounter.count()) / total;
    }
}
