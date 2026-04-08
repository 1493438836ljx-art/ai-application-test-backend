package com.huawei.cloudopenlabs.agent.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.dto.AgentPlan;
import com.huawei.cloudopenlabs.agent.exception.ActionExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * 操作执行器
 * <p>
 * 负责执行 AI 请求的操作（写操作）， * 特点：顺序执行，保证事务一致性
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>顺序执行操作（保证事务一致性）</li>
 *   <li>请求体构建</li>
 *   <li>权限校验</li>
 *   <li>错误处理和事务回滚</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 */
@Slf4j
@Component
public class ActionExecutor {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * 默认操作超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 允许的 HTTP 方法
     */
    private static final Set<String> ALLOWED_METHODS = Set.of(
            "POST", "PUT", "PATCH", "DELETE"
    );

    public ActionExecutor(@Autowired WebClient.Builder webClientBuilder,
                          @Autowired ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8080")
                .build();
        this.objectMapper = objectMapper;
        log.info("ActionExecutor 初始化完成");
    }

    /**
     * 执行操作列表（顺序执行，保证事务一致性）
     *
     * @param actions    操作列表
     * @param workflowId 工作流ID
     * @return 操作结果 Map，key 为操作ID，value 为执行结果
     */
    @Transactional
    public Map<String, Object> executeActions(List<AgentPlan.Action> actions, Long workflowId) {
        if (actions == null || actions.isEmpty()) {
            log.debug("操作列表为空，返回空结果");
            return Collections.emptyMap();
        }

        log.info("开始顺序执行 {} 个操作请求", actions.size());

        Map<String, Object> results = new LinkedHashMap<>();
        List<String> executedActions = new ArrayList<>();

        for (AgentPlan.Action action : actions) {
            try {
                Object result = executeAction(action, workflowId);
                results.put(action.getId(), result);
                executedActions.add(action.getId());
                log.info("操作成功: id={}, path={}", action.getId(), action.getPath());

            } catch (Exception e) {
                log.error("操作失败: id={}, path={}, error={}",
                        action.getId(), action.getPath(), e.getMessage());

                // 记录失败结果
                results.put(action.getId(), buildErrorResult(action, e));

                // 操作失败时抛出异常，触发事务回滚
                throw new ActionExecutionException(
                        action.getId(),
                        action.getPath(),
                        "操作执行失败: " + e.getMessage(),
                        e
                );
            }
        }

        log.info("操作执行完成: 成功={}", executedActions.size());
        return results;
    }

    /**
     * 执行单个操作
     *
     * @param action     操作定义
     * @param workflowId 工作流ID
     * @return 执行结果
     */
    public Object executeAction(AgentPlan.Action action, Long workflowId) {
        String actionId = action.getId();
        String path = resolvePath(action.getPath(), workflowId);
        String method = validateAndNormalizeMethod(action.getMethod());
        JsonNode body = action.getBody();

        log.info("执行操作: id={}, method={}, path={}", actionId, method, path);

        // 验证路径
        validatePath(path);

        long startTime = System.currentTimeMillis();

        try {
            WebClient.RequestHeadersSpec<?> request = createActionRequest(path, method, body);

            Object result = request.retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

            long duration = System.currentTimeMillis() - startTime;
            log.debug("操作成功: id={}, duration={}ms", actionId, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("操作失败: id={}, path={}, duration={}ms, error={}",
                    actionId, path, duration, e.getMessage());

            throw new ActionExecutionException(actionId, path, "操作执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建操作请求
     */
    private WebClient.RequestHeadersSpec<?> createActionRequest(String path, String method, JsonNode body) {
        HttpMethod httpMethod = HttpMethod.valueOf(method);

        WebClient.RequestBodySpec request = webClient.method(httpMethod)
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON);

        // 对于有请求体的方法，添加请求体
        if (body != null && !body.isNull()) {
            return request.bodyValue(body);
        }

        return request;
    }

    /**
     * 验证并标准化 HTTP 方法
     */
    private String validateAndNormalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "POST"; // 默认 POST
        }

        String normalized = method.toUpperCase().trim();

        if (!ALLOWED_METHODS.contains(normalized)) {
            throw new ActionExecutionException(
                    null, null,
                    "不允许的操作方法: " + method + "，允许的方法: " + ALLOWED_METHODS
            );
        }

        return normalized;
    }

    /**
     * 验证路径安全性
     */
    private void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new ActionExecutionException(null, null, "操作路径不能为空");
        }

        // 检查路径遍历攻击
        if (path.contains("..") || path.contains("//")) {
            throw new ActionExecutionException(null, path, "路径包含非法字符");
        }

        // 只允许 /api/ 开头的路径
        if (!path.startsWith("/api/")) {
            throw new ActionExecutionException(null, path, "只允许访问 /api/ 开头的路径");
        }
    }

    /**
     * 解析路径（替换变量）
     *
     * @param path       原始路径
     * @param workflowId 工作流ID
     * @return 解析后的路径
     */
    // 包可见，便于测试
    String resolvePath(String path, Long workflowId) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // 注意顺序：先替换 ${var} 格式，再替换 {var} 格式
        if (workflowId != null) {
            String idStr = workflowId.toString();
            // 先替换 ${...} 格式
            path = path.replace("${workflowId}", idStr);
            path = path.replace("${id}", idStr);
            // 再替换 {...} 格式
            path = path.replace("{workflowId}", idStr);
            path = path.replace("{id}", idStr);
        }

        return path;
    }

    /**
     * 构建错误结果
     */
    // 包可见，便于测试
    Map<String, Object> buildErrorResult(AgentPlan.Action action, Throwable error) {
        Map<String, Object> errorResult = new LinkedHashMap<>();
        errorResult.put("error", true);
        errorResult.put("actionId", action.getId());
        errorResult.put("path", action.getPath());
        errorResult.put("message", error.getMessage());

        if (error.getCause() != null) {
            errorResult.put("cause", error.getCause().getMessage());
        }

        return errorResult;
    }

    /**
     * 判断是否为错误结果
     */
    // 包可见，便于测试
    boolean isErrorResult(Object result) {
        if (result instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) result;
            return Boolean.TRUE.equals(map.get("error"));
        }
        return false;
    }

    /**
     * 批量操作（不抛出异常，记录失败）
     *
     * @param actions    操作列表
     * @param workflowId 工作流ID
     * @return 操作结果（包含成功的和失败的）
     */
    public Map<String, Object> executeActionsWithoutTransaction(List<AgentPlan.Action> actions, Long workflowId) {
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("开始执行 {} 个操作（无事务模式）", actions.size());

        Map<String, Object> results = new LinkedHashMap<>();

        for (AgentPlan.Action action : actions) {
            try {
                Object result = executeAction(action, workflowId);
                results.put(action.getId(), result);
            } catch (Exception e) {
                log.warn("操作失败（无事务模式）: id={}, error={}", action.getId(), e.getMessage());
                results.put(action.getId(), buildErrorResult(action, e));
            }
        }

        return results;
    }

    /**
     * 操作结果摘要
     */
    public static class ActionResultSummary {
        private final int totalActions;
        private final int successCount;
        private final int errorCount;
        private final long totalDurationMs;

        public ActionResultSummary(int totalActions, int successCount, int errorCount, long totalDurationMs) {
            this.totalActions = totalActions;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.totalDurationMs = totalDurationMs;
        }

        public int getTotalActions() {
            return totalActions;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public long getTotalDurationMs() {
            return totalDurationMs;
        }

        public boolean isAllSuccess() {
            return errorCount == 0;
        }

        @Override
        public String toString() {
            return String.format("ActionResultSummary{total=%d, success=%d, error=%d, duration=%dms}",
                    totalActions, successCount, errorCount, totalDurationMs);
        }
    }

    /**
     * 生成操作结果摘要
     */
    public ActionResultSummary summarizeResults(Map<String, Object> results) {
        if (results == null || results.isEmpty()) {
            return new ActionResultSummary(0, 0, 0, 0);
        }

        int total = results.size();
        int errors = (int) results.values().stream().filter(this::isErrorResult).count();

        return new ActionResultSummary(total, total - errors, errors, 0);
    }
}
