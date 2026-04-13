/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.config.AgentContextProperties;
import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 上下文构建器
 * 负责构建和截断 Agent 执行上下文
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Component
public class ContextBuilder {

    private final AgentContextProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public ContextBuilder(AgentContextProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    /**
     * 构建初始上下文
     *
     * @param userMessage 用户消息
     * @param session     会话实体
     * @return 构建好的上下文
     */
    public String buildInitialContext(String userMessage, AgentSessionEntity session) {
        StringBuilder sb = new StringBuilder();

        // 强调使用中文回复
        sb.append("【重要】必须按要求格式输出，请务必使用中文进行所有回复和输出，包括 reasoning、summary 等字段内容。\n\n");

        sb.append("用户请求: ").append(userMessage).append("\n\n");
        sb.append("workflowId: ").append(session.getWorkflowId()).append("\n\n");

        // 添加之前的查询结果（带截断）
        if (session.getQueryResults() != null && !session.getQueryResults().equals("{}")) {
            String truncatedQueryResults = truncateJsonContent(session.getQueryResults(), "查询结果");
            sb.append("之前的查询结果:\n").append(truncatedQueryResults).append("\n\n");
        }

        // 添加之前的操作结果（带截断）
        if (session.getActionResults() != null && !session.getActionResults().equals("{}")) {
            String truncatedActionResults = truncateJsonContent(session.getActionResults(), "操作结果");
            sb.append("之前的操作结果:\n").append(truncatedActionResults).append("\n\n");
        }

        // 添加当前轮次信息
        sb.append("当前轮次: ").append(session.getRoundCount() + 1).append("\n");

        // 最终长度检查和截断
        String context = sb.toString();
        if (properties.isTruncationEnabled() && context.length() > properties.getMaxLength()) {
            context = emergencyTruncate(context, properties.getMaxLength());
            log.info("Context truncated: originalLength={}, truncatedLength={}", sb.length(), context.length());
        }

        return context;
    }

    /**
     * 构建带结果的上下文
     *
     * @param session    会话实体
     * @param newResults 新的结果
     * @param resultType  结果类型 (query/action)
     * @return 构建好的上下文
     */
    public String buildContextWithResults(AgentSessionEntity session, Map<String, Object> newResults, String resultType) {
        StringBuilder sb = new StringBuilder();

        // 强调使用中文回复
        sb.append("【重要】请务必使用中文进行所有回复和输出。\n\n");

        sb.append("本轮").append(resultType.equals("query") ? "查询" : "操作").append("结果:\n");
        try {
            String resultsJson = objectMapper.writeValueAsString(newResults);
            // 截断本轮结果
            resultsJson = truncateJsonString(resultsJson, properties.getMaxResultLength());
            sb.append(resultsJson).append("\n\n");
        } catch (JsonProcessingException e) {
            sb.append("结果序列化失败\n\n");
        }

        // 重新获取最新的 session 数据
        String queryResults = session.getQueryResults();
        String actionResults = session.getActionResults();

        if (queryResults != null && !queryResults.equals("{}")) {
            String truncated = truncateJsonContent(queryResults, "查询结果");
            sb.append("累计查询结果:\n").append(truncated).append("\n\n");
        }

        if (actionResults != null && !actionResults.equals("{}")) {
            String truncated = truncateJsonContent(actionResults, "操作结果");
            sb.append("累计操作结果:\n").append(truncated).append("\n\n");
        }

        // 最终长度检查和截断
        String context = sb.toString();
        if (properties.isTruncationEnabled() && context.length() > properties.getMaxLength()) {
            context = emergencyTruncate(context, properties.getMaxLength());
            log.info("Context with results truncated: originalLength={}, truncatedLength={}", sb.length(), context.length());
        }

        return context;
    }

    /**
     * 截断 JSON 内容字符串
     *
     * @param jsonContent JSON 内容
     * @param label       标签（用于日志）
     * @return 截断后的内容
     */
    private String truncateJsonContent(String jsonContent, String label) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return "";
        }

        int maxLength = properties.getMaxResultLength();
        if (jsonContent.length() <= maxLength) {
            return jsonContent;
        }

        log.debug("{} content too long, truncating: originalLength={}, maxLength={}", label, jsonContent.length(), maxLength);
        return jsonContent.substring(0, maxLength) + "\n...(内容已截断)";
    }

    /**
     * 截断 JSON 字符串
     *
     * @param json      JSON 字符串
     * @param maxLength 最大长度
     * @return 截断后的 JSON
     */
    private String truncateJsonString(String json, int maxLength) {
        if (json == null || json.length() <= maxLength) {
            return json;
        }
        return json.substring(0, maxLength) + "...(已截断)";
    }

    /**
     * 紧急截断（保留开头和结尾）
     * 当上下文整体超过限制时使用
     *
     * @param context   上下文内容
     * @param maxLength 最大长度
     * @return 截断后的上下文
     */
    private String emergencyTruncate(String context, int maxLength) {
        if (context.length() <= maxLength) {
            return context;
        }

        // 保留开头 40% 和结尾 40%，中间用省略号代替
        int headLength = (int) (maxLength * 0.4);
        int tailLength = (int) (maxLength * 0.4);
        String separator = "\n\n...(中间内容已省略以节省 Token)...\n\n";

        return context.substring(0, headLength) +
                separator +
                context.substring(context.length() - tailLength);
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return Map 对象
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON parsing failed: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
