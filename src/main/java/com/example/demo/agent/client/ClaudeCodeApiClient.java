package com.example.demo.agent.client;

import com.example.demo.agent.config.ClaudeCodeProperties;
import com.example.demo.agent.dto.HealthCheckResponse;
import com.example.demo.agent.dto.TaskExecuteRequest;
import com.example.demo.agent.dto.TaskExecuteResponse;
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
import java.nio.charset.StandardCharsets;

/**
 * Claude Code API 客户端
 * <p>
 * 负责 Claude Code RESTful API 的底层调用
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
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
            log.warn("此服务使用 --dangerously-skip-permissions 标志");
            log.warn("跳过所有权限检查，可执行任意系统命令");
            log.warn("请确保仅在内网或受控环境中使用！");
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
        log.debug("执行健康检查，请求 URL: {}", url);

        try {
            ResponseEntity<HealthCheckResponse> response = restTemplate.getForEntity(url, HealthCheckResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("健康检查失败: {}", e.getMessage());
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
        String url = properties.getBaseUrl() + "/api/task";
        log.debug("执行任务，请求 URL: {}, 任务内容: {}", url, taskContent);

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
                log.error("任务执行失败: {}", responseBody.getError());
            } else {
                log.debug("任务执行成功");
            }

            return responseBody;
        } catch (Exception e) {
            log.error("任务执行异常: {}", e.getMessage(), e);
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
        return executeTask(taskContent, configJson, null, null);
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
        return executeTask(request.getTaskContent(), configJson, skillFile, skillFileName);
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
            log.warn("配置对象转换为 JSON 失败: {}", e.getMessage());
            return null;
        }
    }
}
