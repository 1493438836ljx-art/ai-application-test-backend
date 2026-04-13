/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Claude Code 配置属性类
 * <p>
 * 用于从 application.yml 读取 Claude Code 相关配置
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "claude.code")
public class ClaudeCodeProperties {

    /**
     * Claude Code API 基础 URL（旧架构使用）
     * 默认为 http://localhost:3000
     */
    private String baseUrl = "http://localhost:3000";

    /**
     * Connection timeout时间（秒）
     */
    private Integer connectTimeout = 10;

    /**
     * Read timeout时间（秒）
     * 注意：任务执行接口本身的超时为 120 秒，此值应大于 120
     */
    private Integer readTimeout = 130;

    /**
     * 是否启用 Claude Code Agent
     */
    private Boolean enabled = true;

    /**
     * 是否启用危险模式提示（用于记录日志警告）
     */
    private Boolean showDangerModeWarning = true;

    // ==================== 新架构配置 ====================

    /**
     * 是否使用新架构（直接调用 CLI）
     * true: 使用 ClaudeCliExecutor 直接调用 CLI
     * false: 使用旧的 Node.js 中间层
     */
    private Boolean useNewArchitecture = true;

    /**
     * Claude CLI 工作目录
     * 默认为当前工作目录
     */
    private String workingDirectory = System.getProperty("user.dir");

    /**
     * 是否启用详细模式
     */
    private Boolean verbose = true;

    /**
     * 是否跳过权限检查（仅限开发环境）
     * 生产环境必须设置为 false
     */
    private Boolean dangerouslySkipPermissions = false;

    /**
     * CLI 执行超时时间（毫秒）
     * 默认 5 分钟
     */
    private Long cliTimeoutMs = 300000L;

    /**
     * Skill 文件路径
     */
    private String skillFilePath = "skills/workflow-assistant.zip";

    /**
     * Skill 缓存目录
     */
    private String skillCacheDir = "uploads/skills";

    /**
     * 流读取线程数
     */
    private Integer streamReaderThreads = 4;
}
