/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * SSE 心跳管理器
 * <p>
 * 为 SSE 连接提供心跳机制，防止连接因超时断开
 * </p>
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>定时发送心跳事件</li>
 *   <li>追踪活跃连接</li>
 *   <li>自动清理断开的连接</li>
 *   <li>连接状态监控</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class SseHeartbeatManager {

    private final ScheduledExecutorService heartbeatExecutor;

    /**
     * 活跃的 SSE 连接
     */
    private final ConcurrentHashMap<String, SseConnection> activeConnections = new ConcurrentHashMap<>();

    /**
     * 心跳间隔（毫秒）
     */
    @Value("${agent.sse.heartbeat-interval-ms:30000}")
    private long heartbeatIntervalMs;

    /**
     * 心跳超时次数阈值（超过此次数判定连接断开）
     */
    @Value("${agent.sse.heartbeat-timeout-count:3}")
    private int heartbeatTimeoutCount;

    public SseHeartbeatManager() {
        this.heartbeatExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        log.info("SSE 心跳管理器初始化完成");
    }

    /**
     * 注册带心跳的 SSE 连接
     *
     * @param sessionId 会话ID
     * @param timeout   连接超时时间（毫秒）
     * @return SseEmitter
     */
    public SseEmitter registerConnection(String sessionId, long timeout) {
        // 创建 Emitter
        SseEmitter emitter = new SseEmitter(timeout);

        // 创建连接信息
        SseConnection connection = new SseConnection(sessionId, emitter);
        activeConnections.put(sessionId, connection);

        // 启动心跳任务
        ScheduledFuture<?> heartbeatFuture = heartbeatExecutor.scheduleAtFixedRate(
                () -> sendHeartbeat(sessionId),
                heartbeatIntervalMs,
                heartbeatIntervalMs,
                TimeUnit.MILLISECONDS
        );
        connection.setHeartbeatFuture(heartbeatFuture);

        // 设置回调
        emitter.onCompletion(() -> cleanupConnection(sessionId, "completed"));
        emitter.onTimeout(() -> cleanupConnection(sessionId, "timeout"));
        emitter.onError(e -> cleanupConnection(sessionId, "error: " + e.getMessage()));

        log.info("注册 SSE 连接: sessionId={}, activeConnections={}",
                sessionId, activeConnections.size());

        return emitter;
    }

    /**
     * 发送心跳事件
     */
    private void sendHeartbeat(String sessionId) {
        SseConnection connection = activeConnections.get(sessionId);
        if (connection == null) {
            return;
        }

        try {
            SseEmitter emitter = connection.getEmitter();

            // 发送心跳事件
            emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(buildHeartbeatData()));

            // 重置失败计数
            connection.resetFailedCount();
            connection.setLastHeartbeatTime(System.currentTimeMillis());

            log.debug("发送心跳成功: sessionId={}", sessionId);

        } catch (IOException e) {
            // 发送失败
            int failedCount = connection.incrementFailedCount();

            log.warn("发送心跳失败: sessionId={}, failedCount={}",
                    sessionId, failedCount);

            if (failedCount >= heartbeatTimeoutCount) {
                log.error("心跳连续失败，清理连接: sessionId={}", sessionId);
                cleanupConnection(sessionId, "heartbeat_failed");
            }
        }
    }

    /**
     * 构建心跳数据
     */
    private String buildHeartbeatData() {
        return String.format(
                "{\"type\":\"heartbeat\",\"timestamp\":%d}",
                System.currentTimeMillis()
        );
    }

    /**
     * 清理连接
     */
    private void cleanupConnection(String sessionId, String reason) {
        SseConnection connection = activeConnections.remove(sessionId);
        if (connection != null) {
            connection.cancelHeartbeat();
            log.info("清理 SSE 连接: sessionId={}, reason={}, activeConnections={}",
                    sessionId, reason, activeConnections.size());
        }
    }

    /**
     * 发送数据事件
     *
     * @param sessionId 会话ID
     * @param eventName 事件名称
     * @param data      数据
     * @return 是否发送成功
     */
    public boolean sendEvent(String sessionId, String eventName, Object data) {
        SseConnection connection = activeConnections.get(sessionId);
        if (connection == null) {
            log.warn("连接不存在: sessionId={}", sessionId);
            return false;
        }

        try {
            connection.getEmitter().send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException e) {
            log.error("发送事件失败: sessionId={}, event={}", sessionId, eventName, e);
            cleanupConnection(sessionId, "send_failed");
            return false;
        }
    }

    /**
     * 发送错误事件
     *
     * @param sessionId 会话ID
     * @param errorCode 错误码
     * @param message   错误消息
     */
    public void sendError(String sessionId, String errorCode, String message) {
        SseConnection connection = activeConnections.get(sessionId);
        if (connection == null) {
            return;
        }

        try {
            connection.getEmitter().send(SseEmitter.event()
                    .name("error")
                    .data(String.format(
                            "{\"code\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                            errorCode, message, System.currentTimeMillis()
                    )));
        } catch (IOException e) {
            log.error("发送错误事件失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 完成连接
     *
     * @param sessionId 会话ID
     */
    public void completeConnection(String sessionId) {
        SseConnection connection = activeConnections.get(sessionId);
        if (connection != null) {
            try {
                connection.getEmitter().complete();
            } catch (Exception e) {
                log.debug("完成连接异常: sessionId={}", sessionId, e);
            }
            cleanupConnection(sessionId, "completed");
        }
    }

    /**
     * 检查连接是否活跃
     */
    public boolean isActive(String sessionId) {
        return activeConnections.containsKey(sessionId);
    }

    /**
     * 获取活跃连接数
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    /**
     * 获取连接统计信息
     */
    public ConnectionStats getStats() {
        return new ConnectionStats(
                activeConnections.size(),
                activeConnections.values().stream()
                        .mapToLong(SseConnection::getDurationMs)
                        .sum(),
                activeConnections.values().stream()
                        .mapToInt(SseConnection::getFailedCount)
                        .max()
                        .orElse(0)
        );
    }

    /**
     * 关闭所有连接
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭 SSE 心跳管理器，活跃连接数: {}", activeConnections.size());

        heartbeatExecutor.shutdown();

        activeConnections.forEach((sessionId, connection) -> {
            try {
                connection.cancelHeartbeat();
                connection.getEmitter().complete();
            } catch (Exception e) {
                log.debug("关闭连接异常: sessionId={}", sessionId, e);
            }
        });

        activeConnections.clear();

        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("SSE 心跳管理器已关闭");
    }

    /**
     * SSE 连接信息
     */
    private static class SseConnection {
        private final String sessionId;
        private final SseEmitter emitter;
        private final long createTime;
        private ScheduledFuture<?> heartbeatFuture;
        private volatile int failedCount = 0;
        private volatile long lastHeartbeatTime;

        public SseConnection(String sessionId, SseEmitter emitter) {
            this.sessionId = sessionId;
            this.emitter = emitter;
            this.createTime = System.currentTimeMillis();
            this.lastHeartbeatTime = createTime;
        }

        public SseEmitter getEmitter() {
            return emitter;
        }

        public void setHeartbeatFuture(ScheduledFuture<?> future) {
            this.heartbeatFuture = future;
        }

        public void cancelHeartbeat() {
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(false);
            }
        }

        public int incrementFailedCount() {
            return ++failedCount;
        }

        public void resetFailedCount() {
            failedCount = 0;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public void setLastHeartbeatTime(long time) {
            this.lastHeartbeatTime = time;
        }

        public long getDurationMs() {
            return System.currentTimeMillis() - createTime;
        }
    }

    /**
     * 连接统计信息
     */
    public record ConnectionStats(
            int activeCount,
            long totalDurationMs,
            int maxFailedCount
    ) {
        @Override
        public String toString() {
            return String.format(
                    "ConnectionStats{active=%d, totalDuration=%dms, maxFailed=%d}",
                    activeCount, totalDurationMs, maxFailedCount
            );
        }
    }
}
