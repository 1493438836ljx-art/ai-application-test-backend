/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.entity;

/**
 * 错误处理策略枚举
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public enum ErrorStrategy {
    /**
     * 停止执行 - 节点执行失败时停止整个工作流
     */
    STOP,

    /**
     * 跳过 - 节点执行失败时跳过该节点继续执行
     */
    SKIP,

    /**
     * 重试 - 节点执行失败时按配置进行重试
     */
    RETRY,

    /**
     * 错误分支 - 节点执行失败时转向错误处理分支
     */
    ERROR_BRANCH
}
