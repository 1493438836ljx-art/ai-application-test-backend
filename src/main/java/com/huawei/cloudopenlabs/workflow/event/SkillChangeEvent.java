/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.event;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 变更事件
 * 当 Skill 发生变更时发布此事件，通知工作流模块更新相关节点的兼容性状态
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Getter
public class SkillChangeEvent extends ApplicationEvent {

    /**
     * 变更的 Skill ID
     */
    private final String skillId;

    /**
     * 变更类型
     */
    private final ChangeType changeType;

    /**
     * 变更前的 Skill 快照（可选）
     */
    private final SkillSnapshot oldSnapshot;

    /**
     * 变更后的 Skill 快照（可选）
     */
    private final SkillSnapshot newSnapshot;

    /**
     * 变更时间
     */
    private final LocalDateTime changedAt;

    /**
     * 变更来源（操作用户）
     */
    private final String changedBy;

    /**
     * 构造函数
     *
     * @param source     事件源
     * @param skillId    Skill ID
     * @param changeType 变更类型
     * @param oldSnapshot 变更前快照
     * @param newSnapshot 变更后快照
     * @param changedBy  变更来源
     */
    public SkillChangeEvent(Object source, String skillId, ChangeType changeType,
                            SkillSnapshot oldSnapshot, SkillSnapshot newSnapshot,
                            String changedBy) {
        super(source);
        this.skillId = skillId;
        this.changeType = changeType;
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
        this.changedAt = LocalDateTime.now();
        this.changedBy = changedBy;
    }

    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        /**
         * 新增 Skill
         */
        CREATED,

        /**
         * 更新 Skill（参数、配置等）
         */
        UPDATED,

        /**
         * 禁用 Skill
         */
        DISABLED,

        /**
         * 启用 Skill
         */
        ENABLED,

        /**
         * 删除 Skill
         */
        DELETED,

        /**
         * 版本升级
         */
        VERSION_UPGRADED
    }

    /**
     * Skill 快照
     * 用于记录变更前后的 Skill 状态
     */
    @Data
    @Builder
    public static class SkillSnapshot {
        /**
         * Skill ID
         */
        private String skillId;

        /**
         * Skill 名称
         */
        private String name;

        /**
         * Skill 版本
         */
        private String version;

        /**
         * 是否启用
         */
        private boolean enabled;

        /**
         * 输入参数定义
         */
        private List<ParamDefinition> inputParams;

        /**
         * 输出参数定义
         */
        private List<ParamDefinition> outputParams;
    }

    /**
     * 参数定义
     */
    @Data
    @Builder
    public static class ParamDefinition {
        /**
         * 参数名称
         */
        private String name;

        /**
         * 参数类型
         */
        private String type;

        /**
         * 是否必填
         */
        private boolean required;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 参数描述
         */
        private String description;
    }
}
