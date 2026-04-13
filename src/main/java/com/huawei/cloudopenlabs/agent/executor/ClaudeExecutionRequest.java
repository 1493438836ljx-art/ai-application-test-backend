/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Claude CLI 执行请求
 * <p>
 * 封装调用 Claude CLI 所需的所有参数
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeExecutionRequest {

    /**
     * 会话ID
     * 用于多轮对话的会话持久化
     * 新会话时为 null 或空
     */
    private String sessionId;

    /**
     * 是否恢复已有会话
     * true: 使用 --resume 参数
     * false: 使用 --session-id 参数
     */
    private boolean resume;

    /**
     * 输入内容
     * 通过 stdin 传递给 Claude CLI
     */
    private String input;

    /**
     * Skill 目录路径
     * 使用 --add-dir 参数添加
     */
    private String skillDir;

    /**
     * 额外的环境变量
     */
    private Map<String, String> environment;

    /**
     * 工作流ID（用于上下文）
     */
    private String workflowId;

    /**
     * 创建新会话请求
     */
    public static ClaudeExecutionRequest newSession(String input, String skillDir) {
        return ClaudeExecutionRequest.builder()
                .input(input)
                .skillDir(skillDir)
                .resume(false)
                .build();
    }

    /**
     * 创建恢复会话请求
     */
    public static ClaudeExecutionRequest resumeSession(String sessionId, String input) {
        return ClaudeExecutionRequest.builder()
                .sessionId(sessionId)
                .input(input)
                .resume(true)
                .build();
    }
}
