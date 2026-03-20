package com.example.demo.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Claude Code 任务执行响应 DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
public class TaskExecuteResponse {

    /**
     * 执行是否成功
     */
    private Boolean success;

    /**
     * Claude 的响应内容
     */
    private String response;

    /**
     * 错误信息（执行失败时）
     */
    private String error;

    /**
     * 错误码（执行失败时）
     */
    private Integer code;

    /**
     * 原始任务内容
     */
    @JsonProperty("taskContent")
    private String taskContent;

    /**
     * 原始配置信息
     */
    private String config;

    /**
     * Skill 文件名
     */
    @JsonProperty("skillFile")
    private String skillFile;
}
