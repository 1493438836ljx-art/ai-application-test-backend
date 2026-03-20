package com.example.demo.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 统一请求类
 * <p>
 * 供外部调用的统一请求格式，包装了 Claude Code 任务执行的所有参数
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {

    /**
     * 任务内容（必填）
     * 发送给 Claude 执行的具体任务描述
     */
    private String taskContent;

    /**
     * Agent 配置（可选）
     */
    @Builder.Default
    private AgentConfig config = AgentConfig.builder().build();

    /**
     * Skill 文件字节数组（可选）
     * 用于上传自定义 Skill ZIP 文件
     */
    private byte[] skillFileBytes;

    /**
     * Skill 文件名（可选，与 skillFileBytes 配合使用）
     */
    private String skillFileName;
}
