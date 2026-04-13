/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.exception;

/**
 * Claude CLI 执行异常
 * <p>
 * 当 Claude CLI 执行失败时抛出
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
public class ClaudeCliException extends RuntimeException {

    /**
     * 退出码
     */
    private Integer exitCode;

    public ClaudeCliException(String message) {
        super(message);
    }

    public ClaudeCliException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClaudeCliException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public ClaudeCliException(String message, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public Integer getExitCode() {
        return exitCode;
    }
}
