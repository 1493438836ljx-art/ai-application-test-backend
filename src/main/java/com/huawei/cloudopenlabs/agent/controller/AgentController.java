/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.controller;

import com.huawei.cloudopenlabs.agent.dto.AgentConfig;
import com.huawei.cloudopenlabs.agent.dto.AgentRequest;
import com.huawei.cloudopenlabs.agent.dto.AgentResponse;
import com.huawei.cloudopenlabs.agent.framework.AgentExecutor;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Agent 框架 RESTful API 控制器
 * <p>
 * 提供统一的 Agent 调用接口供外部使用
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "claude.code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentController {

    private final AgentExecutor agentExecutor;

    /**
     * Agent 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean healthy = agentExecutor.checkHealth();
        if (healthy) {
            return ResponseEntity.ok("Claude Code Agent is healthy");
        } else {
            return ResponseEntity.status(503).body("Claude Code Agent is unhealthy");
        }
    }

    /**
     * 简单执行 Agent 任务
     *
     * @param taskContent 任务内容
     * @return Agent 响应
     */
    @PostMapping("/execute")
    public AgentResponse executeSimple(
            @RequestParam @NotBlank String taskContent) {
        log.info("Received simple Agent task request: {}", taskContent);
        return agentExecutor.executeSimple(taskContent);
    }

    /**
     * 执行 Agent 任务（带配置）
     *
     * @param taskContent 任务内容
     * @param timeout     超时时间（秒），默认 120
     * @param debug       是否开启调试，默认 false
     * @return Agent 响应
     */
    @PostMapping("/execute/config")
    public AgentResponse executeWithConfig(
            @RequestParam @NotBlank String taskContent,
            @RequestParam(defaultValue = "120") Integer timeout,
            @RequestParam(defaultValue = "false") Boolean debug) {
        log.info("Received configured Agent task request: {}, timeout: {}, debug: {}", taskContent, timeout, debug);
        return agentExecutor.executeWithConfig(taskContent, timeout, debug);
    }

    /**
     * 执行 Agent 任务（完整请求）
     *
     * @param agentRequest Agent 请求
     * @return Agent 响应
     */
    @PostMapping("/execute/full")
    public AgentResponse executeFull(@RequestBody AgentRequest agentRequest) {
        log.info("Received full Agent task request: {}", agentRequest.getTaskContent());
        return agentExecutor.execute(agentRequest);
    }

    /**
     * 执行 Agent 任务（带 Skill 文件）
     *
     * @param taskContent  任务内容
     * @param timeout       超时时间（秒）
     * @param debug         是否开启调试
     * @param skillFile     Skill 文件（ZIP 格式）
     * @return Agent 响应
     */
    @PostMapping(value = "/execute/skill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AgentResponse executeWithSkill(
            @RequestParam @NotBlank String taskContent,
            @RequestParam(defaultValue = "120") Integer timeout,
            @RequestParam(defaultValue = "false") Boolean debug,
            @RequestParam(required = false) MultipartFile skillFile) {

        log.info("Received Agent task request with skill: {}", taskContent);

        // 构造请求
        AgentRequest.AgentRequestBuilder builder = AgentRequest.builder()
                .taskContent(taskContent)
                .config(AgentConfig.builder().timeout(timeout).debug(debug).build());

        // 如果有 Skill 文件
        if (skillFile != null && !skillFile.isEmpty()) {
            try {
                builder.skillFileBytes(skillFile.getBytes());
                builder.skillFileName(skillFile.getOriginalFilename());
                log.info("Skill file attached: {}", skillFile.getOriginalFilename());
            } catch (Exception e) {
                log.error("Failed to read skill file: {}", e.getMessage());
                return AgentResponse.builder()
                        .success(false)
                        .error("读取 Skill 文件失败: " + e.getMessage())
                        .errorCode(-3)
                        .originalTaskContent(taskContent)
                        .executionTimeMs(0L)
                        .build();
            }
        }

        return agentExecutor.execute(builder.build());
    }
}
