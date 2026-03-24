package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流创建请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "工作流创建请求")
public class WorkflowCreateRequest {

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 100, message = "工作流名称长度不能超过100")
    @Schema(description = "工作流名称", example = "AI文本测试工作流")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "工作流描述", example = "用于测试AI文本生成能力的工作流")
    private String description;

    @Schema(description = "创建人", example = "admin")
    private String createdBy;

    @Valid
    @Schema(description = "节点列表")
    private List<NodeData> nodes = new ArrayList<>();

    @Valid
    @Schema(description = "连线列表")
    private List<ConnectionData> connections = new ArrayList<>();

    @Valid
    @Schema(description = "关联列表（循环与循环体关系）")
    private List<AssociationData> associations = new ArrayList<>();

    /**
     * 节点数据
     */
    @Data
    @Schema(description = "节点数据")
    public static class NodeData {
        @Schema(description = "节点UUID（前端生成）", example = "node-start")
        private String nodeUuid;

        @NotBlank(message = "节点类型不能为空")
        @Schema(description = "节点类型", example = "start")
        private String type;

        @NotBlank(message = "节点名称不能为空")
        @Schema(description = "节点名称", example = "开始节点")
        private String name;

        @Schema(description = "X坐标", example = "100")
        private Integer positionX = 0;

        @Schema(description = "Y坐标", example = "200")
        private Integer positionY = 0;

        @Schema(description = "输入端口（JSON格式）", example = "[]")
        private String inputPorts = "[]";

        @Schema(description = "输出端口（JSON格式）", example = "[]")
        private String outputPorts = "[]";

        @Schema(description = "输入参数（JSON格式，包含配置的参数值）", example = "[]")
        private String inputParams = "[]";

        @Schema(description = "输出参数（JSON格式）", example = "[]")
        private String outputParams = "[]";

        @Schema(description = "节点配置（JSON格式，包含节点特定的配置参数）", example = "{}")
        private String config = "{}";

        @Schema(description = "父节点UUID（用于循环体内节点）", example = "node-loop")
        private String parentNodeUuid;
    }

    /**
     * 连线数据
     */
    @Data
    @Schema(description = "连线数据")
    public static class ConnectionData {
        @Schema(description = "连线UUID（前端生成）", example = "conn-1")
        private String connectionUuid;

        @NotBlank(message = "源节点UUID不能为空")
        @Schema(description = "源节点UUID", example = "node-start")
        private String sourceNodeUuid;

        @NotBlank(message = "源端口ID不能为空")
        @Schema(description = "源端口ID", example = "output-1")
        private String sourcePortId;

        @NotBlank(message = "目标节点UUID不能为空")
        @Schema(description = "目标节点UUID", example = "node-text-clean")
        private String targetNodeUuid;

        @NotBlank(message = "目标端口ID不能为空")
        @Schema(description = "目标端口ID", example = "input-1")
        private String targetPortId;

        @Schema(description = "源参数索引")
        private Integer sourceParamIndex;

        @Schema(description = "目标参数索引")
        private Integer targetParamIndex;

        @Schema(description = "连线标签")
        private String label;
    }

    /**
     * 关联数据（循环与循环体关系）
     */
    @Data
    @Schema(description = "关联数据")
    public static class AssociationData {
        @NotBlank(message = "循环节点UUID不能为空")
        @Schema(description = "循环节点UUID", example = "node-loop")
        private String loopNodeUuid;

        @NotBlank(message = "循环体节点UUID不能为空")
        @Schema(description = "循环体节点UUID", example = "node-loop-body")
        private String bodyNodeUuid;

        @Schema(description = "关联类型", example = "LOOP")
        private String associationType = "LOOP";
    }
}
