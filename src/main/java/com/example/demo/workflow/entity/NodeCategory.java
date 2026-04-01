package com.example.demo.workflow.entity;

/**
 * 节点分类枚举
 *
 * @author AI Test Platform Team
 * @version 1.0.0
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
