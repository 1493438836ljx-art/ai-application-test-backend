package com.example.demo.workflow.entity;

/**
 * 兼容性状态枚举
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public enum CompatibilityStatus {
    /**
     * 兼容 - Skill未变更或参数完全匹配
     */
    COMPATIBLE,

    /**
     * 需要更新 - Skill有轻微变更，参数配置可能需要调整
     */
    NEEDS_UPDATE,

    /**
     * 不兼容 - Skill已删除或参数结构发生重大变化
     */
    INCOMPATIBLE,

    /**
     * 无效 - SkillId不存在或无法验证
     */
    INVALID
}
