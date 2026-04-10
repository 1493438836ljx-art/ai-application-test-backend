/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class WorkflowResponse {

    private String id;

    private String name;

    private String description;

    private Boolean published;

    private Boolean hasRun;

    private Integer version;

    private WorkflowStatus status;

    private String triggerType;

    private String triggerConfig;

    private LocalDateTime publishedAt;

    private String publishedBy;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private List<NodeDTO> nodes;

    private List<ConnectionDTO> connections;

    private List<AssociationDTO> associations;

    /**
     * 节点DTO
     */
    @Data
    public static class NodeDTO {
        private String id;

        private String nodeUuid;

        private String type;

        private String name;

        private Integer positionX;

        private Integer positionY;

        private String inputPorts;

        private String outputPorts;

        private String inputParams;

        private String outputParams;

        private String config;

        private String parentNodeId;

        private String parentNodeUuid;

        // ========== Skill引用 ==========

        private String skillId;

        private String skillSnapshot;

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

        private String nodeCategory;
    }

    /**
     * 连线DTO
     */
    @Data
    public static class ConnectionDTO {
        private String id;

        private String connectionUuid;

        private String sourceNodeId;

        private String sourceNodeUuid;

        private String sourcePortId;

        private String targetNodeId;

        private String targetNodeUuid;

        private String targetPortId;

        private Integer sourceParamIndex;

        private Integer targetParamIndex;

        private String label;

        private String branchLabel;

        private Integer branchPriority;
    }

    /**
     * 关联DTO
     */
    @Data
    public static class AssociationDTO {
        private String id;

        private String containerNodeId;

        private String containerNodeUuid;

        private String bodyNodeId;

        private String bodyNodeUuid;

        private String associationType;
    }
}
