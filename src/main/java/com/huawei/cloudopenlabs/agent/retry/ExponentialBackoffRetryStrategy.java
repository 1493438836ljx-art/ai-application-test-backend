/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 指数退避重试策略
 * <p>
 * 提供带有指数退避的重试机制，避免重试风暴
 * </p>
 *
 * <h3>特点：</h3>
 * <ul>
 *   <li>指数退避延迟：每次重试延迟时间翻倍</li>
 *   <li>最大延迟限制：防止延迟过长</li>
 *   <li>可配置参数：初始延迟、退避因子、最大延迟</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

    /**
     * 初始延迟（毫秒）
     */
    @Value("${agent.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    /**
     * 退避乘数
     */
    @Value("${agent.retry.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    /**
     * 最大延迟（毫秒）
     */
    @Value("${agent.retry.max-delay-ms:30000}")
    private long maxDelayMs;

    /**
     * 默认最大重试次数
     */
    @Value("${agent.retry.default-max-retries:3}")
    private int defaultMaxRetries;

    @Override
    public <T> T executeWithRetry(Supplier<T> action,
                                   Predicate<Exception> shouldRetry,
                                   int maxRetries) {
        Exception lastException = null;
        int attempts = Math.max(1, maxRetries);

        for (int attempt = 0; attempt <= attempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;

                // 检查是否应该重试
                if (!shouldRetry.test(e) || attempt == attempts) {
                    throw new RetryExhaustedException(
                            "操作失败，已达到最大Retry count: " + (attempt + 1),
                            e
                    );
                }

                long delay = calculateDelay(attempt);
                log.warn("Action failed, retrying in {}ms (attempt {}/{}): {}",
                        delay, attempt + 1, attempts, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RetryInterruptedException("重试被中断", ie);
                }
            }
        }

        throw new RetryExhaustedException("操作失败", lastException);
    }

    /**
     * 执行带重试的操作（使用默认最大重试次数）
     *
     * @param action 待执行的操作
     * @param <T>    返回值类型
     * @return 操作执行结果
     */
    public <T> T executeWithDefaultRetry(Supplier<T> action) {
        return executeWithRetry(action, e -> true, defaultMaxRetries);
    }

    /**
     * 执行带重试的操作（使用默认最大重试次数和自定义谓词）
     *
     * @param action      待执行的操作
     * @param shouldRetry 判断异常是否应重试的谓词
     * @param <T>         返回值类型
     * @return 操作执行结果
     */
    public <T> T executeWithDefaultRetry(Supplier<T> action, Predicate<Exception> shouldRetry) {
        return executeWithRetry(action, shouldRetry, defaultMaxRetries);
    }

    /**
     * 计算重试延迟
     */
    private long calculateDelay(int attempt) {
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt));
        return Math.min(delay, maxDelayMs);
    }

    /**
     * 获取当前配置的重试参数
     *
     * @return 重试配置信息
     */
    public RetryConfig getConfig() {
        return new RetryConfig(initialDelayMs, backoffMultiplier, maxDelayMs, defaultMaxRetries);
    }

    /**
     * 重试配置信息
     */
    public record RetryConfig(
            long initialDelayMs,
            double backoffMultiplier,
            long maxDelayMs,
            int defaultMaxRetries
    ) {
        @Override
        public String toString() {
            return String.format(
                    "RetryConfig{initial=%dms, multiplier=%.1f, max=%dms, retries=%d}",
                    initialDelayMs, backoffMultiplier, maxDelayMs, defaultMaxRetries
            );
        }
    }
}
