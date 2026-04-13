/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.executor.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Skill执行请求
 * 包含执行所需的所有信息，不依赖数据库连接
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillExecutionRequest {

    // ========== Skill基本信息 ==========

    /**
     * Skill ID
     */
    private String skillId;

    /**
     * Skill名称
     */
    private String skillName;

    /**
     * Skill描述
     */
    private String description;

    // ========== 执行配置 ==========

    /**
     * 执行类型: AUTOMATED / AI
     */
    private String executionType;

    /**
     * 执行位置: SERVICE / CLIENT
     */
    private String executionLocation;

    /**
     * 超时时间（毫秒）
     */
    private Long timeoutMs;

    // ========== 执行套件信息 ==========

    /**
     * 执行套件文件路径（服务端执行时使用）
     */
    private String suitePath;

    /**
     * 执行套件文件名
     */
    private String suiteFilename;

    /**
     * 执行套件内容（远程执行时使用，ZIP格式的字节数组）
     */
    private byte[] suiteContent;

    /**
     * 入口脚本文件名（如 main.py）
     */
    private String entryScript;

    // ========== 参数定义 ==========

    /**
     * 输入参数定义
     */
    private List<SkillParameterDef> inputParameters;

    /**
     * 输出参数定义
     */
    private List<SkillParameterDef> outputParameters;

    // ========== 执行输入 ==========

    /**
     * 实际输入参数值
     */
    private Map<String, Object> inputs;

    /**
     * 执行上下文信息（如工作流ID、节点ID等，用于日志追踪）
     */
    private Map<String, String> context;

    /**
     * Skill参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillParameterDef {
        /**
         * 参数名
         */
        private String name;

        /**
         * 参数类型
         */
        private String type;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 是否必填
         */
        private Boolean required;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 实际值（执行时填充）
         */
        private Object value;
    }
}
