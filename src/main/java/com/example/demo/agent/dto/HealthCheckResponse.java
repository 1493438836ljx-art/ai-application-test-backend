package com.example.demo.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Claude Code API 健康检查响应 DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
public class HealthCheckResponse {

    /**
     * 服务状态
     */
    private String status;

    /**
     * 服务运行时间（秒）
     */
    @JsonProperty("uptime")
    private Double uptime;

    /**
     * 是否跳过权限检查（危险模式）
     */
    @JsonProperty("dangerouslySkipPermission")
    private Boolean dangerouslySkipPermission;

    /**
     * 运行模式
     */
    @JsonProperty("mode")
    private String mode;

    /**
     * Claude 启动标志
     */
    @JsonProperty("claudeFlags")
    private String claudeFlags;
}
