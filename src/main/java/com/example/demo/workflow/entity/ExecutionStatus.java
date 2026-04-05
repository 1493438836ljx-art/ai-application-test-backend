package com.example.demo.workflow.entity;

/**
 * 执行状态枚举
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public enum ExecutionStatus {
    /**
     * 等待中
     */
    PENDING(false),

    /**
     * 运行中
     */
    RUNNING(false),

    /**
     * 成功
     */
    SUCCESS(true),

    /**
     * 失败
     */
    FAILED(true),

    /**
     * 已中止
     */
    ABORTED(true),

    /**
     * 部分成功
     */
    PARTIAL_SUCCESS(true),

    /**
     * 超时
     */
    TIMEOUT(true);

    private final boolean terminal;

    ExecutionStatus(boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * 是否为终止状态
     */
    public boolean isTerminal() {
        return terminal;
    }
}
