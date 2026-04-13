/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.kafka.service;

import com.huawei.cloudopenlabs.workflow.kafka.dto.SkillExecutionKafkaRequest;
import com.huawei.cloudopenlabs.workflow.kafka.dto.SkillExecutionKafkaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Skill执行Kafka服务
 * 实现request-reply模式：发送执行请求到Kafka，阻塞等待Executor的响应
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Service
@Slf4j
public class SkillKafkaService {

    private final KafkaTemplate<String, SkillExecutionKafkaRequest> kafkaTemplate;

    /** 待响应的请求映射：requestId -> CompletableFuture */
    private final ConcurrentHashMap<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    private final String requestTopic;
    private final String responseTopic;
    private final long requestTimeoutMs;

    public SkillKafkaService(
            KafkaTemplate<String, SkillExecutionKafkaRequest> kafkaTemplate,
            @Value("${skill-executor.kafka.request-topic}") String requestTopic,
            @Value("${skill-executor.kafka.response-topic}") String responseTopic,
            @Value("${skill-executor.kafka.request-timeout-ms:300000}") long requestTimeoutMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.requestTopic = requestTopic;
        this.responseTopic = responseTopic;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    /**
     * 发送执行请求并等待响应
     *
     * @param kafkaRequest Kafka请求消息
     * @return 执行响应
     * @throws TimeoutException 等待响应超时
     * @throws Exception 发送失败或其他异常
     */
    public SkillExecutionKafkaResponse sendAndReceive(SkillExecutionKafkaRequest kafkaRequest)
            throws Exception {

        String requestId = kafkaRequest.getRequestId();
        CompletableFuture<SkillExecutionKafkaResponse> future = new CompletableFuture<>();

        // 注册待响应请求
        pendingRequests.put(requestId, new PendingRequest(future, System.currentTimeMillis()));

        try {
            // 发送消息到Kafka
            CompletableFuture<SendResult<String, SkillExecutionKafkaRequest>> sendFuture =
                    kafkaTemplate.send(requestTopic, requestId, kafkaRequest);

            // 等待发送确认
            sendFuture.get(10, TimeUnit.SECONDS);

            log.info("已发送Skill执行请求: requestId={}, nodeUuid={}", requestId, kafkaRequest.getNodeUuid());

            // 阻塞等待响应
            return future.get(requestTimeoutMs, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            pendingRequests.remove(requestId);
            log.error("Skill执行请求超时: requestId={}, nodeUuid={}, timeoutMs={}",
                    requestId, kafkaRequest.getNodeUuid(), requestTimeoutMs);
            throw new TimeoutException("SkillExecution timeout: requestId=" + requestId);

        } catch (Exception e) {
            pendingRequests.remove(requestId);
            log.error("Skill执行请求失败: requestId={}, error={}", requestId, e.getMessage());
            throw e;
        }
    }

    /**
     * 接收Executor的执行响应
     * 通过Kafka Consumer监听响应topic
     */
    @org.springframework.kafka.annotation.KafkaListener(
            topics = "${skill-executor.kafka.response-topic}",
            containerFactory = "skillResponseKafkaListenerContainerFactory"
    )
    public void handleResponse(SkillExecutionKafkaResponse response) {
        String requestId = response.getRequestId();
        log.info("Received skill execution response: requestId={}, success={}", requestId, response.isSuccess());

        PendingRequest pending = pendingRequests.remove(requestId);
        if (pending != null) {
            pending.getFuture().complete(response);
        } else {
            log.warn("收到过期的Skill执行响应（无匹配的pending request）: requestId={}", requestId);
        }
    }

    /**
     * 定时清理超时的待响应请求，防止内存泄漏
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupStaleRequests() {
        long now = System.currentTimeMillis();
        long staleThreshold = requestTimeoutMs + 30_000;

        for (Map.Entry<String, PendingRequest> entry : pendingRequests.entrySet()) {
            long age = now - entry.getValue().getCreatedAt();
            if (age > staleThreshold) {
                PendingRequest removed = pendingRequests.remove(entry.getKey());
                if (removed != null) {
                    removed.getFuture().completeExceptionally(
                            new TimeoutException("Skill执行请求已过期: requestId=" + entry.getKey()));
                    log.warn("清理过期的Skill执行请求: requestId={}, ageMs={}", entry.getKey(), age);
                }
            }
        }
    }

    /**
     * 获取当前待响应请求数量（用于监控）
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * 待响应请求包装
     */
    private static class PendingRequest {
        private final CompletableFuture<SkillExecutionKafkaResponse> future;
        private final long createdAt;

        PendingRequest(CompletableFuture<SkillExecutionKafkaResponse> future, long createdAt) {
            this.future = future;
            this.createdAt = createdAt;
        }

        CompletableFuture<SkillExecutionKafkaResponse> getFuture() {
            return future;
        }

        long getCreatedAt() {
            return createdAt;
        }
    }
}
