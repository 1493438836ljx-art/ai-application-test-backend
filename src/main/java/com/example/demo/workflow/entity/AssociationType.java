package com.example.demo.workflow.entity;

/**
 * 关联类型枚举
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public enum AssociationType {
    /**
     * 循环关联（旧版兼容）
     */
    LOOP,

    /**
     * 循环体关联
     */
    LOOP_BODY,

    /**
     * 批处理体关联
     */
    BATCH_BODY,

    /**
     * 异步体关联
     */
    ASYNC_BODY,

    /**
     * 条件关联
     */
    CONDITION
}
