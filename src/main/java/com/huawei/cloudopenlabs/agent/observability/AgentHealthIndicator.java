/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.observability;

import com.huawei.cloudopenlabs.agent.cache.CacheStatistics;
import com.huawei.cloudopenlabs.agent.cache.SkillCacheManager;
import com.huawei.cloudopenlabs.agent.sse.SseHeartbeatManager;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Agent 健康检查指示器
 * <p>
 * 提供自定义的健康检查，监控 Agent 各组件状态
 * </p>
 *
 * <h3>检查项目：</h3>
 * <ul>
 *   <li>活跃会话数</li>
 *   <li>活跃连接数</li>
 *   <li>缓存命中率</li>
 *   <li>线程池状态</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.4.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class AgentHealthIndicator implements HealthIndicator {

    private final AgentMetrics metrics;
    private final SkillCacheManager cacheManager;
    private final SseHeartbeatManager heartbeatManager;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Value("${agent.health.max-response-time-ms:5000}")
    private long maxResponseTimeMs;

    @Value("${agent.health.queue-usage-threshold:0.8}")
    private double queueUsageThreshold;

    @Value("${agent.health.cache-hit-rate-threshold:0.5}")
    private double cacheHitRateThreshold;

    public AgentHealthIndicator(
            AgentMetrics metrics,
            SkillCacheManager cacheManager,
            SseHeartbeatManager heartbeatManager,
            @Qualifier("agentTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.metrics = metrics;
        this.cacheManager = cacheManager;
        this.heartbeatManager = heartbeatManager;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            // 检查活跃会话数
            int activeSessions = metrics.getActiveSessionCount();
            builder.withDetail("activeSessions", activeSessions);

            // 检查活跃连接数
            int activeConnections = heartbeatManager.getActiveConnectionCount();
            builder.withDetail("activeConnections", activeConnections);

            // 检查缓存状态
            CacheStatistics cacheStats = cacheManager.getStats();
            double cacheHitRate = cacheStats.hitRate();
            builder.withDetail("cacheHitRate", String.format("%.2f%%", cacheHitRate * 100));
            builder.withDetail("cacheHits", cacheStats.hitCount());
            builder.withDetail("cacheMisses", cacheStats.missCount());

            // 检查线程池状态
            ThreadPoolStats threadStats = getThreadPoolStats();
            builder.withDetail("threadPoolActive", threadStats.activeCount());
            builder.withDetail("threadPoolMax", threadStats.maxPoolSize());
            builder.withDetail("threadPoolQueue", threadStats.queueSize());
            builder.withDetail("threadPoolUsage", String.format("%.2f%%", threadStats.threadUsageRate() * 100));

            // 检查请求统计
            builder.withDetail("totalRequests", (long) metrics.getRequestCount());
            builder.withDetail("successRate", String.format("%.2f%%", metrics.getSuccessRate() * 100));
            builder.withDetail("errorRate", String.format("%.2f%%", metrics.getErrorRate() * 100));

            // 判断健康状态
            if (threadStats.queueUsageRate() > queueUsageThreshold) {
                builder.status(Status.DOWN)
                        .withDetail("reason", "Thread pool queue nearly full: " + threadStats.queueUsageRate());
                log.warn("Health check abnormal: thread pool queue near capacity");
            } else if (cacheHitRate < cacheHitRateThreshold && cacheStats.requestCount() > 10) {
                builder.status(Status.OUT_OF_SERVICE)
                        .withDetail("reason", "Low cache hit rate: " + cacheHitRate);
                log.warn("Health check warning: low cache hit rate");
            } else {
                builder.withDetail("status", "healthy");
            }

        } catch (Exception e) {
            log.error("Health check exception", e);
            builder.status(Status.DOWN)
                    .withException(e)
                    .withDetail("error", e.getMessage());
        }

        return builder.build();
    }

    /**
     * 获取线程池统计信息
     */
    private ThreadPoolStats getThreadPoolStats() {
        if (taskExecutor == null) {
            return new ThreadPoolStats(0, 0, 0, 0, 0);
        }

        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
        return new ThreadPoolStats(
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
        );
    }

    /**
     * 执行轻量级健康检查（用于内部监控）
     *
     * @return 健康时返回true，否则返回false
     */
    public boolean isHealthy() {
        try {
            ThreadPoolStats stats = getThreadPoolStats();
            return !stats.isBusy(queueUsageThreshold);
        } catch (Exception e) {
            return false;
        }
    }
}
