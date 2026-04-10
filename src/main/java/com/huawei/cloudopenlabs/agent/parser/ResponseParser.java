/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.dto.AgentPlan;
import com.huawei.cloudopenlabs.agent.exception.ResponseParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 响应解析器
 * <p>
 * 负责解析 AI 响应为 AgentPlan，具有容错能力
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>JSON 提取（支持代码块和直接 JSON）</li>
 *   <li>状态标准化（容错处理各种变体）</li>
 *   <li>查询/操作列表解析</li>
 *   <li>异常处理和错误反馈</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 */
@Slf4j
@Component
public class ResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * JSON 代码块提取正则
     * 匹配 ```json ... ``` 格式
     */
    private static final Pattern JSON_BLOCK_PATTERN =
            Pattern.compile("```json\\s*\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);

    /**
     * JSON 对象提取正则
     * 匹配最外层的 { ... }
     */
    private static final Pattern JSON_OBJECT_PATTERN =
            Pattern.compile("\\{[\\s\\S]*\\}", Pattern.MULTILINE);

    public ResponseParser(@Autowired ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    /**
     * 解析 AI 响应为 AgentPlan
     *
     * @param response AI 响应字符串
     * @return 解析后的 AgentPlan
     * @throws ResponseParseException 解析失败时抛出
     */
    public AgentPlan parse(String response) throws ResponseParseException {
        if (response == null || response.isBlank()) {
            throw new ResponseParseException("响应内容为空");
        }

        // 提取 JSON 内容
        String jsonContent = extractJsonContent(response);
        if (jsonContent == null || jsonContent.isBlank()) {
            throw new ResponseParseException("无法从响应中提取 JSON 内容", response);
        }

        // 解析 JSON
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            return parseAgentPlan(root);
        } catch (Exception e) {
            log.error("解析 JSON 失败: {}", jsonContent.substring(0, Math.min(200, jsonContent.length())), e);
            throw new ResponseParseException("JSON 解析失败: " + e.getMessage(), response, e);
        }
    }

    /**
     * 尝试解析，失败时返回 parse_error 状态的 AgentPlan
     *
     * @param response AI 响应字符串
     * @return 解析后的 AgentPlan（不会返回 null）
     */
    public AgentPlan parseOrNull(String response) {
        try {
            return parse(response);
        } catch (ResponseParseException e) {
            log.warn("响应解析失败，返回 parse_error 状态: {}", e.getMessage());
            return AgentPlan.builder()
                    .status(AgentPlan.STATUS_PARSE_ERROR)
                    .reasoning("响应格式解析失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 提取 JSON 内容
     * <p>
     * 支持以下格式：
     * <ul>
     *   <li>```json ... ``` 代码块</li>
     *   <li>直接 JSON 对象</li>
     * </ul>
     * </p>
     */
    private String extractJsonContent(String response) {
        // 1. 尝试从 ```json 代码块中提取
        Matcher blockMatcher = JSON_BLOCK_PATTERN.matcher(response);
        if (blockMatcher.find()) {
            String extracted = blockMatcher.group(1).trim();
            log.debug("从代码块中提取到 JSON，长度: {}", extracted.length());
            return extracted;
        }

        // 2. 尝试直接提取 JSON 对象
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(response);
        if (objectMatcher.find()) {
            String extracted = objectMatcher.group();
            log.debug("直接提取到 JSON 对象，长度: {}", extracted.length());
            return extracted;
        }

        log.debug("未能提取到 JSON 内容");
        return null;
    }

    /**
     * 解析 AgentPlan
     */
    private AgentPlan parseAgentPlan(JsonNode root) {
        AgentPlan.AgentPlanBuilder builder = AgentPlan.builder();

        // 解析状态（容错处理）
        String status = getTextIgnoreCase(root, "status");
        builder.status(normalizeStatus(status));

        // 解析推理和摘要
        builder.reasoning(getTextIgnoreCase(root, "reasoning", "thinking", "thought"));
        builder.summary(getTextIgnoreCase(root, "summary", "conclusion"));

        // 解析结果
        if (root.has("result")) {
            builder.result(root.get("result"));
        }

        // 解析查询列表
        JsonNode queriesNode = getNodeIgnoreCase(root, "queries", "query", "reads", "reads");
        if (queriesNode != null && queriesNode.isArray()) {
            builder.queries(parseQueries(queriesNode));
        }

        // 解析操作列表
        JsonNode actionsNode = getNodeIgnoreCase(root, "actions", "action", "writes", "write");
        if (actionsNode != null && actionsNode.isArray()) {
            builder.actions(parseActions(actionsNode));
        }

        return builder.build();
    }

    /**
     * 标准化状态值
     * <p>
     * 支持多种状态变体，统一转换为标准状态
     * </p>
     */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return AgentPlan.STATUS_PARSE_ERROR;
        }

        String normalized = status.trim().toLowerCase();

        switch (normalized) {
            // 查询状态变体
            case "query":
            case "queries":
            case "read":
            case "reads":
            case "get":
            case "fetch":
            case "retrieve":
                return AgentPlan.STATUS_QUERY;

            // 操作状态变体
            case "action":
            case "actions":
            case "write":
            case "writes":
            case "post":
            case "put":
            case "delete":
            case "update":
            case "modify":
                return AgentPlan.STATUS_ACTION;

            // 完成状态变体
            case "complete":
            case "completed":
            case "done":
            case "finished":
            case "success":
            case "end":
                return AgentPlan.STATUS_COMPLETE;

            // 默认为解析错误
            default:
                log.warn("未知的状态值: {}，将视为解析错误", status);
                return AgentPlan.STATUS_PARSE_ERROR;
        }
    }

    /**
     * 解析查询列表
     */
    private List<AgentPlan.Query> parseQueries(JsonNode queriesNode) {
        List<AgentPlan.Query> queries = new ArrayList<>();

        for (JsonNode queryNode : queriesNode) {
            try {
                AgentPlan.Query query = AgentPlan.Query.builder()
                        .id(getTextIgnoreCase(queryNode, "id", "query_id", "name"))
                        .path(getTextIgnoreCase(queryNode, "path", "url", "endpoint", "api"))
                        .method(getTextIgnoreCase(queryNode, "method", "http_method", "type", "GET"))
                        .description(getTextIgnoreCase(queryNode, "description", "desc", "note"))
                        .params(getNodeIgnoreCase(queryNode, "params", "parameters", "query"))
                        .build();

                // 验证必要字段
                if (query.getPath() != null && !query.getPath().isBlank()) {
                    queries.add(query);
                } else {
                    log.warn("跳过无效查询（缺少 path）: {}", queryNode);
                }
            } catch (Exception e) {
                log.warn("解析查询失败: {}", queryNode, e);
            }
        }

        return queries;
    }

    /**
     * 解析操作列表
     */
    private List<AgentPlan.Action> parseActions(JsonNode actionsNode) {
        List<AgentPlan.Action> actions = new ArrayList<>();

        for (JsonNode actionNode : actionsNode) {
            try {
                AgentPlan.Action action = AgentPlan.Action.builder()
                        .id(getTextIgnoreCase(actionNode, "id", "action_id", "name"))
                        .path(getTextIgnoreCase(actionNode, "path", "url", "endpoint", "api"))
                        .method(getTextIgnoreCase(actionNode, "method", "http_method", "type", "POST"))
                        .description(getTextIgnoreCase(actionNode, "description", "desc", "note"))
                        .body(getNodeIgnoreCase(actionNode, "body", "data", "payload", "content"))
                        .build();

                // 验证必要字段
                if (action.getPath() != null && !action.getPath().isBlank()) {
                    actions.add(action);
                } else {
                    log.warn("跳过无效操作（缺少 path）: {}", actionNode);
                }
            } catch (Exception e) {
                log.warn("解析操作失败: {}", actionNode, e);
            }
        }

        return actions;
    }

    /**
     * 获取文本字段（忽略大小写，支持多个候选字段名）
     */
    private String getTextIgnoreCase(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            // 精确匹配
            if (node.has(fieldName)) {
                JsonNode fieldNode = node.get(fieldName);
                if (fieldNode.isTextual()) {
                    return fieldNode.asText();
                } else if (!fieldNode.isNull()) {
                    return fieldNode.toString();
                }
            }

            // 忽略大小写匹配
            var iterator = node.fieldNames();
            while (iterator.hasNext()) {
                String fn = iterator.next();
                if (fn.equalsIgnoreCase(fieldName)) {
                    JsonNode fieldNode = node.get(fn);
                    if (fieldNode.isTextual()) {
                        return fieldNode.asText();
                    } else if (!fieldNode.isNull()) {
                        return fieldNode.toString();
                    }
                }
            }
        }

        return null;
    }

    /**
     * 获取节点（忽略大小写，支持多个候选字段名）
     */
    private JsonNode getNodeIgnoreCase(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            // 精确匹配
            if (node.has(fieldName)) {
                return node.get(fieldName);
            }

            // 忽略大小写匹配
            var iterator = node.fieldNames();
            while (iterator.hasNext()) {
                String fn = iterator.next();
                if (fn.equalsIgnoreCase(fieldName)) {
                    return node.get(fn);
                }
            }
        }

        return null;
    }

    /**
     * 验证 AgentPlan 是否有效
     *
     * @param plan 要验证的计划
     * @return 验证结果
     */
    public ValidationResult validate(AgentPlan plan) {
        if (plan == null) {
            return ValidationResult.invalid("计划为空");
        }

        if (plan.getStatus() == null || plan.getStatus().isBlank()) {
            return ValidationResult.invalid("状态为空");
        }

        // 查询状态必须有查询列表
        if (plan.isQuery() && !plan.hasQueries()) {
            return ValidationResult.invalid("查询状态但没有查询请求");
        }

        // 操作状态必须有操作列表
        if (plan.isAction() && !plan.hasActions()) {
            return ValidationResult.invalid("操作状态但没有操作请求");
        }

        return ValidationResult.valid();
    }

    /**
     * 验证结果
     */
    public record ValidationResult(boolean isValid, String errorMessage) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}
