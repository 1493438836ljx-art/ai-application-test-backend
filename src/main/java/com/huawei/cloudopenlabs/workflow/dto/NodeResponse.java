/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class NodeResponse {

    private String id;

    private String nodeUuid;

    private String nodeName;

    private String nodeType;

    private String nodeCategory;

    private Integer positionX;

    private Integer positionY;

    // ========== Skill引用 ==========

    private String skillId;

    private String skillSnapshot;

    // ========== 端口配置 ==========

    private String inputPorts;

    private String outputPorts;

    // ========== 参数配置 ==========

    private String inputParams;

    private String outputParams;

    // ========== 执行配置 ==========

    private String executionLocation;

    private String errorStrategy;

    private Integer retryCount;

    private Integer retryInterval;

    private String errorBranchId;

    // ========== 条件节点配置 ==========

    private String conditionType;

    private String conditions;

    // ========== 循环节点配置 ==========

    private String loopType;

    private String loopConfig;

    // ========== 批处理/异步/收集配置 ==========

    private String batchConfig;

    private String asyncConfig;

    private String collectConfig;

    // ========== 兼容性状态 ==========

    private String compatibilityStatus;

    // ========== 时间字段 ==========

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
