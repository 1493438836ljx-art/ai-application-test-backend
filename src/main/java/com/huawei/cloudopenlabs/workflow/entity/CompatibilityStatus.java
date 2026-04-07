package com.huawei.cloudopenlabs.workflow.entity;

/**
 * 兼容性状态枚举
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
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
