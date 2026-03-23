package com.example.demo.workflow.dto;

import com.example.demo.workflow.entity.WorkflowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "工作流响应")
public class WorkflowResponse {

    @Schema(description = "工作流ID")
    private Long id;

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "工作流描述")
    private String description;

    @Schema(description = "是否已发布")
    private Boolean published;

    @Schema(description = "是否已运行")
    private Boolean hasRun;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "状态")
    private WorkflowStatus status;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新人")
    private String updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "节点列表")
    private List<NodeDTO> nodes;

    @Schema(description = "连线列表")
    private List<ConnectionDTO> connections;

    @Schema(description = "关联列表")
    private List<AssociationDTO> associations;

    /**
     * 节点DTO
     */
    @Data
    @Schema(description = "节点信息")
    public static class NodeDTO {
        @Schema(description = "节点ID")
        private Long id;

        @Schema(description = "节点UUID")
        private String nodeUuid;

        @Schema(description = "节点类型")
        private String type;

        @Schema(description = "节点名称")
        private String name;

        @Schema(description = "X坐标")
        private Integer positionX;

        @Schema(description = "Y坐标")
        private Integer positionY;

        @Schema(description = "输入端口")
        private String inputPorts;

        @Schema(description = "输出端口")
        private String outputPorts;

        @Schema(description = "输入参数")
        private String inputParams;

        @Schema(description = "输出参数")
        private String outputParams;

        @Schema(description = "配置")
        private String config;

        @Schema(description = "父节点ID")
        private Long parentNodeId;
    }

    /**
     * 连线DTO
     */
    @Data
    @Schema(description = "连线信息")
    public static class ConnectionDTO {
        @Schema(description = "连线ID")
        private Long id;

        @Schema(description = "连线UUID")
        private String connectionUuid;

        @Schema(description = "源节点ID")
        private Long sourceNodeId;

        @Schema(description = "源端口ID")
        private String sourcePortId;

        @Schema(description = "目标节点ID")
        private Long targetNodeId;

        @Schema(description = "目标端口ID")
        private String targetPortId;

        @Schema(description = "源参数索引")
        private Integer sourceParamIndex;

        @Schema(description = "目标参数索引")
        private Integer targetParamIndex;

        @Schema(description = "标签")
        private String label;
    }

    /**
     * 关联DTO
     */
    @Data
    @Schema(description = "关联信息")
    public static class AssociationDTO {
        @Schema(description = "关联ID")
        private Long id;

        @Schema(description = "循环节点ID")
        private Long loopNodeId;

        @Schema(description = "循环体节点ID")
        private Long bodyNodeId;

        @Schema(description = "关联类型")
        private String associationType;
    }
}
