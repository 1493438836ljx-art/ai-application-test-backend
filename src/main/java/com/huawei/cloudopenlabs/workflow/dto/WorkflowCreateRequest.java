/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流创建请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class WorkflowCreateRequest {

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 100, message = "工作流名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    private String createdBy;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    private String triggerType;

    private String triggerConfig;

    @Valid
    private List<NodeData> nodes = new ArrayList<>();

    @Valid
    private List<ConnectionData> connections = new ArrayList<>();

    @Valid
    private List<AssociationData> associations = new ArrayList<>();

    /**
     * 节点数据
     */
    @Data
    public static class NodeData {
        private String nodeUuid;

        @NotBlank(message = "节点类型不能为空")
        private String type;

        @NotBlank(message = "节点名称不能为空")
        private String name;

        private Integer positionX = 0;

        private Integer positionY = 0;

        private String inputPorts = "[]";

        private String outputPorts = "[]";

        private String inputParams = "[]";

        private String outputParams = "[]";

        private String config = "{}";

        private String parentNodeUuid;

        // ========== Skill引用 ==========

        private String skillId;

        private String skillSnapshot;

        // ========== 执行配置 ==========

        private String executionLocation;

        private String errorStrategy;

        private Integer retryCount;

        private Integer retryInterval;

        private String errorBranchNodeUuid;

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

        // ========== 节点分类 ==========

        private String nodeCategory;
    }

    /**
     * 连线数据
     */
    @Data
    public static class ConnectionData {
        private String connectionUuid;

        @NotBlank(message = "源节点UUID不能为空")
        private String sourceNodeUuid;

        @NotBlank(message = "源端口ID不能为空")
        private String sourcePortId;

        @NotBlank(message = "目标节点UUID不能为空")
        private String targetNodeUuid;

        @NotBlank(message = "目标端口ID不能为空")
        private String targetPortId;

        private Integer sourceParamIndex;

        private Integer targetParamIndex;

        private String label;

        private String branchLabel;

        private Integer branchPriority;
    }

    /**
     * 关联数据（容器与子节点关系）
     */
    @Data
    public static class AssociationData {
        @NotBlank(message = "容器节点UUID不能为空")
        private String containerNodeUuid;

        @NotBlank(message = "子节点UUID不能为空")
        private String bodyNodeUuid;

        private String associationType = "LOOP_BODY";
    }
}
