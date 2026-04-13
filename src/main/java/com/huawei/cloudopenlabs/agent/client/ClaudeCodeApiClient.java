/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.client;

import com.huawei.cloudopenlabs.agent.config.ClaudeCodeProperties;
import com.huawei.cloudopenlabs.agent.dto.HealthCheckResponse;
import com.huawei.cloudopenlabs.agent.dto.TaskExecuteRequest;
import com.huawei.cloudopenlabs.agent.dto.TaskExecuteResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Claude Code API 客户端
 * <p>
 * 负责 Claude Code RESTful API 的底层调用
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "claude.code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClaudeCodeApiClient {

    private final ClaudeCodeProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param properties Claude Code 配置属性
     */
    public ClaudeCodeApiClient(ClaudeCodeProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        // 危险模式警告
        if (properties.getShowDangerModeWarning()) {
            log.warn("========================================");
            log.warn("⚠️  CLAUDE CODE AGENT - DANGEROUS MODE ⚠️");
            log.warn("This service uses --dangerously-skip-permissions flag");
            log.warn("Skipping all permission checks, can execute arbitrary system commands");
            log.warn("Please ensure this is only used in internal or controlled environments!");
            log.warn("========================================");
        }
    }

    /**
     * 健康检查
     *
     * @return 健康检查响应
     */
    public HealthCheckResponse healthCheck() {
        String url = properties.getBaseUrl() + "/health";
        log.debug("Executing health check, request URL: {}", url);

        try {
            ResponseEntity<HealthCheckResponse> response = restTemplate.getForEntity(url, HealthCheckResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            throw new RuntimeException("Claude Code API 连接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行任务（带 Skill 文件）
     *
     * @param taskContent  任务内容
     * @param configJson   配置 JSON 字符串（可选）
     * @param skillFile    Skill 文件字节数组（可选）
     * @param skillFileName Skill 文件名（可选）
     * @return 任务执行响应
     */
    public TaskExecuteResponse executeTask(String taskContent, String configJson, byte[] skillFile, String skillFileName) {
        return executeTask(taskContent, configJson, skillFile, skillFileName, null);
    }

    /**
     * 执行任务（带 Skill 文件和 sessionId）
     *
     * @param taskContent  任务内容
     * @param configJson   配置 JSON 字符串（可选）
     * @param skillFile    Skill 文件字节数组（可选）
     * @param skillFileName Skill 文件名（可选）
     * @param sessionId    会话ID（可选，用于多 round, ）
     * @return 任务执行响应
     */
        public TaskExecuteResponse executeTask(String taskContent, String configJson, byte[] skillFile, String skillFileName, String sessionId) {
        String url = properties.getBaseUrl() + "/api/task";
        log.debug("Executing task, request URL: {}, task content: {}, sessionId: {}", url, taskContent, sessionId);

        // 构建 multipart/form-data 请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 添加必填字段：taskContent
        body.add("taskContent", taskContent);

        // 添加可选字段：config
        if (configJson != null && !configJson.isBlank()) {
            body.add("config", configJson);
        }

        // 添加可选字段：skillFile
        if (skillFile != null && skillFile.length > 0 && skillFileName != null && !skillFileName.isBlank()) {
            ByteArrayResource resource = new ByteArrayResource(skillFile) {
                @Override
                public String getFilename() {
                    return skillFileName;
                }
            };
            body.add("skillFile", resource);
        }

        // 添加可选字段：sessionId（多轮会话支持）
        if (sessionId != null && !sessionId.isBlank()) {
            body.add("sessionId", sessionId);
            log.info("Using session ID: {}", sessionId);
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TaskExecuteResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    TaskExecuteResponse.class
            );

            TaskExecuteResponse responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("API 返回空响应");
            }

            // 检查业务执行结果
            if (!responseBody.getSuccess()) {
                log.error("Task execution failed: {}", responseBody.getError());
            } else {
                log.debug("Task executed successfully");
            }

            return responseBody;
        } catch (Exception e) {
            log.error("Task execution exception: {}", e.getMessage(), e);
            // 构造失败响应
            TaskExecuteResponse errorResponse = new TaskExecuteResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError(e.getMessage());
            errorResponse.setCode(-1);
            errorResponse.setTaskContent(taskContent);
            return errorResponse;
        }
    }

    /**
     * 执行任务（不带 Skill 文件）
     *
     * @param taskContent 任务内容
     * @param configJson  配置 JSON 字符串（可选）
     * @return 任务执行响应
     */
    public TaskExecuteResponse executeTask(String taskContent, String configJson) {
        return executeTask(taskContent, configJson, null, null, null);
    }

    /**
     * 执行任务（带 sessionId，     *
     * @param taskContent 任务内容
     * @param configJson  配置 JSON 字符串（可选）
     * @param sessionId  会话ID（可选）
     * @return 任务执行响应
     */
    public TaskExecuteResponse executeTask(String taskContent, String configJson, String sessionId) {
        return executeTask(taskContent, configJson, null, null, sessionId);
    }

    /**
     * 使用 TaskExecuteRequest 执行任务
     *
     * @param request 任务执行请求
     * @param skillFile Skill 文件字节数组（可选）
     * @return 任务执行响应
     */
    public TaskExecuteResponse executeTask(TaskExecuteRequest request, byte[] skillFile) {
        String configJson = request.getConfig();
        String skillFileName = request.getSkillFileName();
        String sessionId = request.getSessionId();
        return executeTask(request.getTaskContent(), configJson, skillFile, skillFileName, sessionId);
    }

    /**
     * 解析配置对象为 JSON 字符串
     *
     * @param config 配置对象
     * @return JSON 字符串
     */
    public String configToJson(Object config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (IOException e) {
            log.warn("Failed to convert config object to JSON: {}", e.getMessage());
            return null;
        }
    }
}
