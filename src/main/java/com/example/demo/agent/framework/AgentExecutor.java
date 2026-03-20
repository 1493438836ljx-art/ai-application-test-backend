package com.example.demo.agent.framework;

import com.example.demo.agent.client.ClaudeCodeApiClient;
import com.example.demo.agent.dto.AgentConfig;
import com.example.demo.agent.dto.AgentRequest;
import com.example.demo.agent.dto.AgentResponse;
import com.example.demo.agent.dto.TaskExecuteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Agent 框架统一执行器
 * <p>
 * 提供统一的 Agent 调用接口，内部封装 Claude Code API 调用逻辑
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "claude.code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentExecutor {

    private final ClaudeCodeApiClient apiClient;

    /**
     * 构造函数
     *
     * @param apiClient Claude Code API 客户端
     */
    public AgentExecutor(ClaudeCodeApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * 执行 Agent 任务（核心接口）
     *
     * @param request Agent 请求
     * @return Agent 响应
     */
    public AgentResponse execute(AgentRequest request) {
        return execute(request, null);
    }

    /**
     * 执行 Agent 任务（带回调）
     *
     * @param request Agent 请求
     * @param callback 执行回调
     * @return Agent 响应
     */
    public AgentResponse execute(AgentRequest request, AgentCallback callback) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始执行 Agent 任务: {}", request.getTaskContent());

            // 请求前回调
            if (callback != null) {
                callback.beforeExecute(request);
            }

            // 转换配置
            String configJson = request.getConfig() != null
                    ? request.getConfig().toJsonString()
                    : null;

            // 调用 Claude Code API
            TaskExecuteResponse apiResponse = apiClient.executeTask(
                    request.getTaskContent(),
                    configJson,
                    request.getSkillFileBytes(),
                    request.getSkillFileName()
            );

            long executionTime = System.currentTimeMillis() - startTime;

            // 构造响应
            AgentResponse response = AgentResponse.builder()
                    .success(apiResponse.getSuccess())
                    .response(apiResponse.getResponse())
                    .error(apiResponse.getError())
                    .errorCode(apiResponse.getCode())
                    .originalTaskContent(apiResponse.getTaskContent())
                    .executionTimeMs(executionTime)
                    .build();

            log.info("Agent 任务执行完成，耗时: {}ms，成功: {}", executionTime, response.getSuccess());

            // 请求后回调
            if (callback != null) {
                callback.afterExecute(request, response);
            }

            return response;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("Agent 任务执行异常: {}", e.getMessage(), e);

            AgentResponse response = AgentResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .errorCode(-2)
                    .originalTaskContent(request.getTaskContent())
                    .executionTimeMs(executionTime)
                    .build();

            if (callback != null) {
                callback.onError(request, response, e);
            }

            return response;
        }
    }

    /**
     * 简化接口：仅执行任务
     *
     * @param taskContent 任务内容
     * @return Agent 响应
     */
    public AgentResponse executeSimple(String taskContent) {
        return execute(AgentRequest.builder()
                .taskContent(taskContent)
                .build());
    }

    /**
     * 简化接口：执行任务并配置
     *
     * @param taskContent 任务内容
     * @param timeout     超时时间（秒）
     * @param debug       是否开启调试
     * @return Agent 响应
     */
    public AgentResponse executeWithConfig(String taskContent, Integer timeout, Boolean debug) {
        AgentConfig config = AgentConfig.builder()
                .timeout(timeout)
                .debug(debug)
                .build();
        return execute(AgentRequest.builder()
                .taskContent(taskContent)
                .config(config)
                .build());
    }

    /**
     * 异步执行 Agent 任务
     *
     * @param request Agent 请求
     * @param callback 执行回调
     */
    public void executeAsync(AgentRequest request, AgentCallback callback) {
        new Thread(() -> execute(request, callback)).start();
    }

    /**
     * 检查 Claude Code 服务健康状态
     *
     * @return true 表示服务正常，false 表示服务异常
     */
    public boolean checkHealth() {
        try {
            apiClient.healthCheck();
            return true;
        } catch (Exception e) {
            log.error("Claude Code 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Agent 执行回调接口
     */
    public interface AgentCallback {

        /**
         * 执行前回调（可选）
         *
         * @param request Agent 请求
         */
        default void beforeExecute(AgentRequest request) {
            log.debug("Agent 任务即将执行: {}", request.getTaskContent());
        }

        /**
         * 执行后回调（可选）
         *
         * @param request  Agent 请求
         * @param response Agent 响应
         */
        default void afterExecute(AgentRequest request, AgentResponse response) {
            log.debug("Agent 任务执行完成，成功: {}", response.getSuccess());
        }

        /**
         * 异常回调（可选）
         *
         * @param request  Agent 请求
         * @param response Agent 响应
         * @param e        异常
         */
        default void onError(AgentRequest request, AgentResponse response, Exception e) {
            log.error("Agent 任务执行异常: {}", e.getMessage());
        }
    }
}
