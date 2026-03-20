package com.example.demo.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Claude Code API 配置属性类
 * <p>
 * 用于从 application.yml 读取 Claude Code API 的连接配置信息
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "claude.code")
public class ClaudeCodeProperties {

    /**
     * Claude Code API 基础 URL
     * 默认为 http://localhost:3000
     */
    private String baseUrl = "http://localhost:3000";

    /**
     * 连接超时时间（秒）
     */
    private Integer connectTimeout = 10;

    /**
     * 读取超时时间（秒）
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
}
