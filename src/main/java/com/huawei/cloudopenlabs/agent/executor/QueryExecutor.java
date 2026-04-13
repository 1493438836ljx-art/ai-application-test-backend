/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.dto.AgentPlan;
import com.huawei.cloudopenlabs.agent.exception.QueryExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询执行器
 * <p>
 * 负责执行 AI 请求的查询操作，支持并行执行
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>并行执行多个独立查询</li>
 *   <li>结果收集和合并</li>
 *   <li>错误处理和容错</li>
 *   <li>路径变量替换</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class QueryExecutor {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * 默认查询超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 路径变量正则：匹配 ${var} 或 {var}
     */
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\$?\\{(\\w+)\\}");

    public QueryExecutor(@Autowired WebClient.Builder webClientBuilder,
                         @Autowired ObjectMapper objectMapper) {
        // 使用本地服务的 WebClient
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8080")
                .build();
        this.objectMapper = objectMapper;
        log.info("QueryExecutor 初始化完成");
    }

    /**
     * 执行查询列表（并行）
     *
     * @param queries    查询列表
     * @param workflowId 工作流ID（用于路径变量替换）
     * @return 查询结果 Map，key 为查询ID，value 为查询结果
     */
    public Map<String, Object> executeQueries(List<AgentPlan.Query> queries, String workflowId) {
        if (queries == null || queries.isEmpty()) {
            log.debug("查询列表为空，返回空结果");
            return Collections.emptyMap();
        }

        log.info("开始并行执行 {} 个查询请求", queries.size());

        // 并行执行所有查询
        Map<String, Object> results = new ConcurrentHashMap<>();

        List<Mono<Void>> queryMonos = new ArrayList<>();

        for (AgentPlan.Query query : queries) {
            Mono<Void> queryMono = executeQueryAsync(query, workflowId)
                    .doOnNext(result -> {
                        results.put(query.getId(), result);
                        log.debug("查询完成: id={}, path={}", query.getId(), query.getPath());
                    })
                    .doOnError(error -> {
                        log.error("查询失败: id={}, path={}, error={}",
                                query.getId(), query.getPath(), error.getMessage());
                        results.put(query.getId(), buildErrorResult(query, error));
                    })
                    .onErrorResume(e -> Mono.empty()) // 错误不中断其他查询
                    .then();

            queryMonos.add(queryMono);
        }

        // 等待所有查询完成
        Mono.when(queryMonos)
                .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS * queries.size()));

        log.info("Query execution completed: 成功={}, 失败={}",
                results.values().stream().filter(v -> !isErrorResult(v)).count(),
                results.values().stream().filter(this::isErrorResult).count());

        return results;
    }

    /**
     * 异步执行单个查询
     */
    private Mono<Object> executeQueryAsync(AgentPlan.Query query, String workflowId) {
        return Mono.fromCallable(() -> executeQuery(query, workflowId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行单个查询
     *
     * @param query      查询定义
     * @param workflowId 工作流ID
     * @return 查询结果
     */
    public Object executeQuery(AgentPlan.Query query, String workflowId) {
        String queryId = query.getId();
        String path = resolvePath(query.getPath(), workflowId);
        String method = query.getMethod() != null ? query.getMethod().toUpperCase() : "GET";

        log.info("Executing query: id={}, method={}, path={}", queryId, method, path);

        long startTime = System.currentTimeMillis();

        try {
            WebClient.RequestHeadersSpec<?> request = createRequest(path, method, query.getParams());

            Object result = request.retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

            long duration = System.currentTimeMillis() - startTime;
            log.debug("查询成功: id={}, duration={}ms", queryId, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("查询失败: id={}, path={}, duration={}ms, error={}",
                    queryId, path, duration, e.getMessage());

            throw new QueryExecutionException(queryId, path, "查询Execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * 创建 WebRequest
     */
    private WebClient.RequestHeadersSpec<?> createRequest(String path, String method, JsonNode params) {
        HttpMethod httpMethod = HttpMethod.valueOf(method);

        WebClient.RequestHeadersSpec<?> request = webClient.method(httpMethod)
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    // 对于 GET 请求，将参数添加到 URL
                    if (httpMethod == HttpMethod.GET && params != null && params.isObject()) {
                        params.fields().forEachRemaining(entry -> {
                            uriBuilder.queryParam(entry.getKey(),
                                    entry.getValue().asText());
                        });
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON);

        // 对于 POST/PUT 请求，设置请求体
        if ((httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) && params != null) {
            ((WebClient.RequestBodySpec) request).contentType(MediaType.APPLICATION_JSON);
            ((WebClient.RequestBodySpec) request).bodyValue(params);
        }

        return request;
    }

    /**
     * 解析路径（替换变量）
     *
     * @param path       原始路径
     * @param workflowId 工作流ID
     * @return 解析后的路径
     */
    // 包可见，便于测试
    String resolvePath(String path, String workflowId) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // 替换 ${workflowId} 或 {workflowId}
        Map<String, Object> variables = new HashMap<>();
        if (workflowId != null) {
            variables.put("workflowId", workflowId);
            variables.put("id", workflowId);
        }

        Matcher matcher = PATH_VAR_PATTERN.matcher(path);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            if (value != null) {
                matcher.appendReplacement(sb, value.toString());
            } else {
                // 变量未找到，保留原样
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 构建错误结果
     */
    // 包可见，便于测试
    Map<String, Object> buildErrorResult(AgentPlan.Query query, Throwable error) {
        Map<String, Object> errorResult = new LinkedHashMap<>();
        errorResult.put("error", true);
        errorResult.put("queryId", query.getId());
        errorResult.put("path", query.getPath());
        errorResult.put("message", error.getMessage());

        // 提取更详细的错误信息
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
     * 批量查询（带重试）
     *
     * @param queries    查询列表
     * @param workflowId 工作流ID
     * @param maxRetries 最大重试次数
     * @return 查询结果
     */
    public Map<String, Object> executeQueriesWithRetry(
            List<AgentPlan.Query> queries,
            String workflowId,
            int maxRetries) {

        Map<String, Object> results = new HashMap<>();

        for (AgentPlan.Query query : queries) {
            int retryCount = 0;
            Object result = null;
            Exception lastError = null;

            while (retryCount <= maxRetries) {
                try {
                    result = executeQuery(query, workflowId);
                    break;
                } catch (Exception e) {
                    lastError = e;
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        log.warn("查询失败，准备重试: id={}, retry={}/{}",
                                query.getId(), retryCount, maxRetries);
                        try {
                            Thread.sleep(1000 * retryCount); // 递增延迟
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            if (result != null) {
                results.put(query.getId(), result);
            } else if (lastError != null) {
                results.put(query.getId(), buildErrorResult(query, lastError));
            }
        }

        return results;
    }

    /**
     * 查询结果摘要
     */
    public static class QueryResultSummary {
        private final int totalQueries;
        private final int successCount;
        private final int errorCount;
        private final long totalDurationMs;

        public QueryResultSummary(int totalQueries, int successCount, int errorCount, long totalDurationMs) {
            this.totalQueries = totalQueries;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.totalDurationMs = totalDurationMs;
        }

        public int getTotalQueries() {
            return totalQueries;
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
            return String.format("QueryResultSummary{total=%d, success=%d, error=%d, duration=%dms}",
                    totalQueries, successCount, errorCount, totalDurationMs);
        }
    }

    /**
     * 生成查询结果摘要
     */
    public QueryResultSummary summarizeResults(Map<String, Object> results) {
        if (results == null || results.isEmpty()) {
            return new QueryResultSummary(0, 0, 0, 0);
        }

        int total = results.size();
        int errors = (int) results.values().stream().filter(this::isErrorResult).count();

        return new QueryResultSummary(total, total - errors, errors, 0);
    }
}
