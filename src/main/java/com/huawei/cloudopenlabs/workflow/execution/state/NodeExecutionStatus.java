/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.state;

/**
 * 节点执行状态枚举
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
public enum NodeExecutionStatus {

    /**
     * 待执行
     */
    PENDING("待执行", false),

    /**
     * 执行中
     */
    RUNNING("执行中", false),

    /**
     * 成功
     */
    SUCCESS("成功", true),

    /**
     * 失败
     */
    FAILED("失败", true),

    /**
     * 已跳过
     */
    SKIPPED("已跳过", true),

    /**
     * 超时
     */
    TIMEOUT("超时", true),

    /**
     * 部分成功（用于批处理等场景）
     */
    PARTIAL_SUCCESS("部分成功", true);

    private final String description;
    private final boolean terminal;

    NodeExecutionStatus(String description, boolean terminal) {
        this.description = description;
        this.terminal = terminal;
    }

    /**
     * 是否为终止状态
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * 获取状态描述
     */
    public String getDescription() {
        return description;
    }
}
