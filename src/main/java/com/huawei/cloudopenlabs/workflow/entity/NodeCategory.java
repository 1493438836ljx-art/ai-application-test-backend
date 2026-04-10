/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.entity;

/**
 * 节点分类枚举
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public enum NodeCategory {
    /**
     * 基础节点（开始、结束、Skill等）
     */
    BASIC,

    /**
     * 逻辑节点（条件、循环节点等）
     */
    LOGIC,

    /**
     * 执行节点（并行、批处理等）
     */
    EXECUTION
}
