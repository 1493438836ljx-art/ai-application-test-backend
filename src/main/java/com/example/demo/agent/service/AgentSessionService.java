package com.example.demo.agent.service;

import com.example.demo.agent.entity.AgentSessionEntity;
import com.example.demo.agent.mapper.AgentSessionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent会话管理服务
 * 负责多轮对话会话的创建、查询和更新
 *
 * @author AI Test Platform Team
 * @version 1.0.0
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
    public AgentSessionEntity getOrCreateSession(Long workflowId, String conversationId) {
        // 如果 conversationId 为空，生成一个临时 UUID
        // 后续会在 Claude CLI 返回 sessionId 后更新
        String effectiveConversationId = conversationId;
        if (effectiveConversationId == null || effectiveConversationId.isEmpty()) {
            effectiveConversationId = UUID.randomUUID().toString();
            log.info("生成临时 conversationId: {}", effectiveConversationId);
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
                sessionMapper.updateById(session);
                log.info("重置会话状态为ACTIVE: conversationId={}", effectiveConversationId);
            }
            return session;
        }

        // 创建新会话
        AgentSessionEntity newSession = new AgentSessionEntity();
        newSession.setConversationId(effectiveConversationId);
        newSession.setWorkflowId(workflowId);
        newSession.setStatus("ACTIVE");
        newSession.setRoundCount(0);
        sessionMapper.insert(newSession);

        log.info("创建新会话: conversationId={}, workflowId={}", effectiveConversationId, workflowId);
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
    public Optional<AgentSessionEntity> getActiveByWorkflowId(Long workflowId) {
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
            log.warn("会话不存在，无法更新查询结果: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();

        // 合并现有查询结果
        Map<String, Object> existingResults = parseJsonToMap(session.getQueryResults());
        existingResults.putAll(queryResults);

        session.setQueryResults(mapToJson(existingResults));
        session.setRoundCount(session.getRoundCount() + 1);
        sessionMapper.updateById(session);

        log.info("更新查询结果: conversationId={}, roundCount={}", conversationId, session.getRoundCount());
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
            log.warn("会话不存在，无法更新操作结果: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();

        // 合并现有操作结果
        Map<String, Object> existingResults = parseJsonToMap(session.getActionResults());
        existingResults.putAll(actionResults);

        session.setActionResults(mapToJson(existingResults));
        session.setRoundCount(session.getRoundCount() + 1);
        sessionMapper.updateById(session);

        log.info("更新操作结果: conversationId={}, roundCount={}", conversationId, session.getRoundCount());
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
            log.warn("会话不存在，无法更新推理内容: conversationId={}", conversationId);
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
            log.warn("会话不存在，无法标记完成: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setStatus("COMPLETED");
        sessionMapper.updateById(session);

        log.info("会话标记为完成: conversationId={}", conversationId);
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
            log.warn("会话不存在，无法更新 conversationId: oldId={}", oldConversationId);
            return null;
        }

        AgentSessionEntity session = sessionOpt.get();
        String oldId = session.getConversationId();
        session.setConversationId(newConversationId);
        sessionMapper.updateById(session);

        log.info("更新会话 conversationId: {} -> {}", oldId, newConversationId);
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
            log.warn("会话不存在，无法标记错误: conversationId={}", conversationId);
            return;
        }

        AgentSessionEntity session = sessionOpt.get();
        session.setStatus("ERROR");
        // 将错误信息存储到 lastReasoning 字段
        session.setLastReasoning("ERROR: " + errorMessage);
        sessionMapper.updateById(session);

        log.info("会话标记为错误: conversationId={}, error={}", conversationId, errorMessage);
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
            log.info("删除会话: conversationId={}", conversationId);
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
            log.error("Map转JSON失败: {}", e.getMessage());
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
            log.error("JSON转Map失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
