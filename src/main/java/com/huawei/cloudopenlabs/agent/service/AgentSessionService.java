/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.service;

import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import com.huawei.cloudopenlabs.agent.mapper.AgentSessionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent会话管理服务
 * 负责多 round, 会话的创建、查询和更新
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Service
public class AgentSessionService {

    private final AgentSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    public AgentSessionService(AgentSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 日期时间模块
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 获取或创建会话
     *
     * @param workflowId     工作流ID
     * @param conversationId 会话ID（即 chat_conversation 的 conversation_uuid）
     * @return Agent会话实体
     */
    @Transactional
    public AgentSessionEntity getOrCreateSession(String workflowId, String conversationId) {
        // 如果 conversationId 为空，生成一个临时 UUID
        // 后续会在 Claude CLI 返回 sessionId 后更新
        String effectiveConversationId = conversationId;
        if (effectiveConversationId == null || effectiveConversationId.isEmpty()) {
            effectiveConversationId = UUID.randomUUID().toString();
            log.info("Generated temporary conversationId: {}", effectiveConversationId);
        }

        Optional<AgentSessionEntity> existingSession = sessionMapper.selectByConversationId(effectiveConversationId);

        if (existingSession.isPresent()) {
            AgentSessionEntity session = existingSession.get();
            // 如果会话已完成或出错，重置为活跃状态
            if (!"ACTIVE".equals(session.getStatus())) {
                session.setStatus("ACTIVE");
                session.setQueryResults(null);
                session.setActionResults(null);
                session.setLastReasoning(null);
                session.setRoundCount(0);
                session.setParseErrorCount(0);
                session.setStartTime(System.currentTimeMillis()); // 重置开始时间
                sessionMapper.updateById(session);
                log.info("Resetting session state to ACTIVE: conversationId={}", effectiveConversationId);
            }
            return session;
        }

        // 创建新会话
        AgentSessionEntity newSession = new AgentSessionEntity();
        newSession.setConversationId(effectiveConversationId);
        newSession.setWorkflowId(workflowId);
        newSession.setStatus("ACTIVE");
        newSession.setRoundCount(0);
        newSession.setParseErrorCount(0);
        newSession.setStartTime(System.currentTimeMillis()); // 设置开始时间
        sessionMapper.insert(newSession);

        log.info("Creating new session: conversationId={}, workflowId={}", effectiveConversationId, workflowId);
        return newSession;
    }

    /**
     * 根据会话ID查询
     *
     * @param conversationId 会话ID
     * @return Agent会话实体
     */
    public Optional<AgentSessionEntity> getByConversationId(String conversationId) {
        return sessionMapper.selectByConversationId(conversationId);
    }

    /**
     * 根据工作流ID查询活跃会话
     *
     * @param workflowId 工作流ID
     * @return Agent会话实体
     */
    public Optional<AgentSessionEntity> getActiveByWorkflowId(String workflowId) {
        return sessionMapper.selectActiveByWorkflowId(workflowId);
    }

    /**
     * 更新查询结果
     *
     * @param conversationId 会话ID
     * @param queryResults   查询结果Map
     */
    @Transactional
    public void updateQueryResults(String conversationId, Map<String, Object> queryResults) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot update query result: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();

        // 合并现有查询结果
        Map<String, Object> existingResults = parseJsonToMap(session.getQueryResults());
        existingResults.putAll(queryResults);

        session.setQueryResults(mapToJson(existingResults));
        session.setRoundCount(session.getRoundCount() + 1);
        sessionMapper.updateById(session);

        log.info("Updating query result: conversationId={}, roundCount={}", conversationId, session.getRoundCount());
    }

    /**
     * 更新操作结果
     *
     * @param conversationId 会话ID
     * @param actionResults   操作结果Map
     */
    @Transactional
    public void updateActionResults(String conversationId, Map<String, Object> actionResults) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot update action result: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();

        // 合并现有操作结果
        Map<String, Object> existingResults = parseJsonToMap(session.getActionResults());
        existingResults.putAll(actionResults);

        session.setActionResults(mapToJson(existingResults));
        session.setRoundCount(session.getRoundCount() + 1);
        sessionMapper.updateById(session);

        log.info("Updated action result: conversationId={}, roundCount={}", conversationId, session.getRoundCount());
    }

    /**
     * 更新最后推理内容
     *
     * @param conversationId 会话ID
     * @param reasoning      推理内容
     */
    @Transactional
    public void updateLastReasoning(String conversationId, String reasoning) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot update reasoning content: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setLastReasoning(reasoning);
        sessionMapper.updateById(session);
    }

    /**
     * 标记会话为完成状态
     *
     * @param conversationId 会话ID
     */
    @Transactional
    public void markAsCompleted(String conversationId) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot mark completed: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setStatus("COMPLETED");
        sessionMapper.updateById(session);

        log.info("Session marked as completed: conversationId={}", conversationId);
    }

    /**
     * 更新会话的 conversationId（当 Claude CLI 返回新的 sessionId 时使用）
     *
     * @param oldConversationId 旧的会话ID
     * @param newConversationId 新的会话ID
     * @return 更新后的会话实体，如果旧会话不存在则返回 null
     */
    @Transactional
    public AgentSessionEntity updateConversationId(String oldConversationId, String newConversationId) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(oldConversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot update conversationId: oldId={}", oldConversationId);
            return null;
        }

        AgentSessionEntity session = sessionOpt.get();
        String oldId = session.getConversationId();
        session.setConversationId(newConversationId);
        sessionMapper.updateById(session);

        log.info("Updated session conversationId: {} -> {}", oldId, newConversationId);
        return session;
    }

    /**
     * 标记会话为错误状态
     *
     * @param conversationId 会话ID
     * @param errorMessage   错误信息
     */
    @Transactional
    public void markAsError(String conversationId, String errorMessage) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot mark error: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setStatus("ERROR");
        // 将错误信息存储到 lastReasoning 字段
        session.setLastReasoning("ERROR: " + errorMessage);
        sessionMapper.updateById(session);

        log.info("Session marked as error: conversationId={}, error={}", conversationId, errorMessage);
    }

    /**
     * 更新轮次计数
     *
     * @param conversationId 会话ID
     * @param roundCount     轮次计数
     */
    @Transactional
    public void updateRoundCount(String conversationId, int roundCount) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot update round count: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setRoundCount(roundCount);
        sessionMapper.updateById(session);

        log.debug("Updated round count: conversationId={}, roundCount={}", conversationId, roundCount);
    }

    /**
     * 更新解析错误计数
     *
     * @param conversationId    会话ID
     * @param parseErrorCount   解析错误计数
     */
    @Transactional
    public void updateParseErrorCount(String conversationId, int parseErrorCount) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot update parse error count: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setParseErrorCount(parseErrorCount);
        sessionMapper.updateById(session);

        log.debug("Updated parse error count: conversationId={}, parseErrorCount={}", conversationId, parseErrorCount);
    }

    /**
     * 设置执行开始时间
     *
     * @param conversationId 会话ID
     * @param startTime      开始时间（时间戳，毫秒）
     */
    @Transactional
    public void setStartTime(String conversationId, Long startTime) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found, cannot set start time: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setStartTime(startTime);
        sessionMapper.updateById(session);

        log.debug("Set start time: conversationId={}, startTime={}", conversationId, startTime);
    }

    /**
     * 删除会话（物理删除）
     *
     * @param conversationId 会话ID
     */
    @Transactional
    public void deleteSession(String conversationId) {
        Optional<AgentSessionEntity> sessionOpt = sessionMapper.selectByConversationId(conversationId);
        if (sessionOpt.isPresent()) {
            sessionMapper.deleteById(sessionOpt.get().getId());
            log.info("Deleted session: conversationId={}", conversationId);
        }
    }

    // ========== 私有工具方法 ==========

    /**
     * 将Map转换为JSON字符串
     */
    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert Map to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 将JSON字符串转换为Map
     */
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to Map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
