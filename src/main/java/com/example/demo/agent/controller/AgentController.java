package com.example.demo.agent.controller;

import com.example.demo.agent.dto.AgentConfig;
import com.example.demo.agent.dto.AgentRequest;
import com.example.demo.agent.dto.AgentResponse;
import com.example.demo.agent.framework.AgentExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "claude.code", name = "enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Agent 框架", description = "Claude Code Agent 统一调用接口")
public class AgentController {

    private final AgentExecutor agentExecutor;

    /**
     * Agent 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "Agent 健康检查", description = "检查 Claude Code Agent 服务是否可用")
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
    @Operation(summary = "执行 Agent 任务", description = "执行简单的 Agent 任务，仅提供任务内容")
    public AgentResponse executeSimple(
            @Parameter(description = "任务内容", required = true)
            @RequestParam @NotBlank String taskContent) {
        log.info("收到简单 Agent 任务请求: {}", taskContent);
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
    @Operation(summary = "执行 Agent 任务（带配置）", description = "执行 Agent 任务并配置超时和调试选项")
    public AgentResponse executeWithConfig(
            @Parameter(description = "任务内容", required = true)
            @RequestParam @NotBlank String taskContent,
            @Parameter(description = "超时时间（秒）", example = "120")
            @RequestParam(defaultValue = "120") Integer timeout,
            @Parameter(description = "是否开启调试")
            @RequestParam(defaultValue = "false") Boolean debug) {
        log.info("收到带配置的 Agent 任务请求: {}, timeout: {}, debug: {}", taskContent, timeout, debug);
        return agentExecutor.executeWithConfig(taskContent, timeout, debug);
    }

    /**
     * 执行 Agent 任务（完整请求）
     *
     * @param agentRequest Agent 请求
     * @return Agent 响应
     */
    @PostMapping("/execute/full")
    @Operation(summary = "执行 Agent 任务（完整）", description = "使用完整请求体执行 Agent 任务")
    public AgentResponse executeFull(@RequestBody AgentRequest agentRequest) {
        log.info("收到完整 Agent 任务请求: {}", agentRequest.getTaskContent());
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
    @Operation(summary = "执行 Agent 任务（带 Skill 文件）", description = "执行 Agent 任务并上传自定义 Skill 文件")
    public AgentResponse executeWithSkill(
            @Parameter(description = "任务内容", required = true)
            @RequestParam @NotBlank String taskContent,
            @Parameter(description = "超时时间（秒）", example = "120")
            @RequestParam(defaultValue = "120") Integer timeout,
            @Parameter(description = "是否开启调试")
            @RequestParam(defaultValue = "false") Boolean debug,
            @Parameter(description = "Skill 文件（ZIP 格式）")
            @RequestParam(required = false) MultipartFile skillFile) {

        log.info("收到带 Skill 的 Agent 任务请求: {}", taskContent);

        // 构造请求
        AgentRequest.AgentRequestBuilder builder = AgentRequest.builder()
                .taskContent(taskContent)
                .config(AgentConfig.builder().timeout(timeout).debug(debug).build());

        // 如果有 Skill 文件
        if (skillFile != null && !skillFile.isEmpty()) {
            try {
                builder.skillFileBytes(skillFile.getBytes());
                builder.skillFileName(skillFile.getOriginalFilename());
                log.info("已附加 Skill 文件: {}", skillFile.getOriginalFilename());
            } catch (Exception e) {
                log.error("读取 Skill 文件失败: {}", e.getMessage());
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
