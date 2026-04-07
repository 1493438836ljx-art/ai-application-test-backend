package com.huawei.cloudopenlabs.workflow.dto;

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
 * @author GNEEC LIVE
 * @version 27.0.1.1
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

    @Size(max = 50, message = "分类长度不能超过50")
    @Schema(description = "工作流分类", example = "测试")
    private String category;

    @Schema(description = "触发类型", example = "MANUAL")
    private String triggerType;

    @Schema(description = "触发配置（JSON格式）", example = "{}")
    private String triggerConfig;

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

        // ========== Skill引用 ==========

        @Schema(description = "引用的Skill ID")
        private String skillId;

        @Schema(description = "Skill快照（JSON格式）")
        private String skillSnapshot;

        // ========== 执行配置 ==========

        @Schema(description = "执行位置：CLIENT/SERVICE", example = "SERVICE")
        private String executionLocation;

        @Schema(description = "错误策略：STOP/SKIP/RETRY/ERROR_BRANCH", example = "STOP")
        private String errorStrategy;

        @Schema(description = "重试次数", example = "3")
        private Integer retryCount;

        @Schema(description = "重试间隔（毫秒）", example = "1000")
        private Integer retryInterval;

        @Schema(description = "错误处理分支节点UUID")
        private String errorBranchNodeUuid;

        // ========== 条件节点配置 ==========

        @Schema(description = "条件类型：SIMPLE/MULTI", example = "SIMPLE")
        private String conditionType;

        @Schema(description = "条件配置（JSON格式）")
        private String conditions;

        // ========== 循环节点配置 ==========

        @Schema(description = "循环类型：COUNT/ARRAY/CONDITION", example = "COUNT")
        private String loopType;

        @Schema(description = "循环配置（JSON格式）")
        private String loopConfig;

        // ========== 批处理/异步/收集配置 ==========

        @Schema(description = "批处理配置（JSON格式）")
        private String batchConfig;

        @Schema(description = "异步处理配置（JSON格式）")
        private String asyncConfig;

        @Schema(description = "结果收集配置（JSON格式）")
        private String collectConfig;

        // ========== 节点分类 ==========

        @Schema(description = "节点分类：BASIC/LOGIC/EXECUTION", example = "BASIC")
        private String nodeCategory;
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

        @Schema(description = "分支标签（true/false/case1/case2/default）", example = "true")
        private String branchLabel;

        @Schema(description = "分支优先级", example = "1")
        private Integer branchPriority;
    }

    /**
     * 关联数据（容器与子节点关系）
     */
    @Data
    @Schema(description = "关联数据")
    public static class AssociationData {
        @NotBlank(message = "容器节点UUID不能为空")
        @Schema(description = "容器节点UUID（循环节点/批处理节点/异步节点）", example = "node-loop")
        private String containerNodeUuid;

        @NotBlank(message = "子节点UUID不能为空")
        @Schema(description = "子节点UUID（循环体/批处理体/异步体）", example = "node-loop-body")
        private String bodyNodeUuid;

        @Schema(description = "关联类型：LOOP_BODY/BATCH_BODY/ASYNC_BODY", example = "LOOP_BODY")
        private String associationType = "LOOP_BODY";
    }
}
