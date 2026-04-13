/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claude CLI 执行结果
 * <p>
 * 封装 CLI 执行的结果信息
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeExecutionResult {

    /**
     * 会话ID
     * 可能与请求的 sessionId 不同（新会话时由 CLI 生成）
     */
    private String sessionId;

    /**
     * 退出码
     * 0 表示成功
     */
    private int exitCode;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }

    /**
     * 创建成功结果
     */
    public static ClaudeExecutionResult success(String sessionId, long durationMs) {
        return new ClaudeExecutionResult(sessionId, 0, durationMs);
    }

    /**
     * 创建失败结果
     */
    public static ClaudeExecutionResult failure(int exitCode, long durationMs) {
        return new ClaudeExecutionResult(null, exitCode, durationMs);
    }
}
