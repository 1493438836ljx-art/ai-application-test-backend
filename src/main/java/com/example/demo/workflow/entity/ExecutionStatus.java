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
    PENDING,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 成功
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILED,

    /**
     * 已中止
     */
    ABORTED
}
