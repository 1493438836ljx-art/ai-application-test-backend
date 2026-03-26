package com.example.demo.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claude Code 任务执行请求 DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecuteRequest {

    /**
     * 任务内容（必填）
     */
    private String taskContent;

    /**
     * 配置信息（可选，JSON 格式）
     */
    private String config;

    /**
     * Skill 文件名（可选）
     */
    private String skillFileName;

    /**
     * 会话ID（可选，用于多轮对话会话持久化）
     * 即 chat_conversation.conversation_uuid
     */
    private String sessionId;
}
