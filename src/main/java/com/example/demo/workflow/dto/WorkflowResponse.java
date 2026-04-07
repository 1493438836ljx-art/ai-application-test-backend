package com.example.demo.workflow.dto;

import com.example.demo.workflow.entity.WorkflowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "触发配置")
    private String triggerConfig;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "发布人")
    private String publishedBy;

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

        @Schema(description = "父节点UUID")
        private String parentNodeUuid;

        // ========== Skill引用 ==========

        @Schema(description = "引用的Skill ID")
        private String skillId;

        @Schema(description = "Skill快照（JSON格式）")
        private String skillSnapshot;

        // ========== 执行配置 ==========

        @Schema(description = "执行位置")
        private String executionLocation;

        @Schema(description = "错误策略")
        private String errorStrategy;

        @Schema(description = "重试次数")
        private Integer retryCount;

        @Schema(description = "重试间隔（毫秒）")
        private Integer retryInterval;

        @Schema(description = "错误分支节点ID")
        private Long errorBranchId;

        // ========== 条件节点配置 ==========

        @Schema(description = "条件类型")
        private String conditionType;

        @Schema(description = "条件配置（JSON格式）")
        private String conditions;

        // ========== 循环节点配置 ==========

        @Schema(description = "循环类型")
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

        // ========== 兼容性状态 ==========

        @Schema(description = "兼容性状态")
        private String compatibilityStatus;

        @Schema(description = "节点分类")
        private String nodeCategory;
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

        @Schema(description = "源节点ID（数据库ID，查询时返回）")
        private Long sourceNodeId;

        @Schema(description = "源节点UUID（前端保存时使用）")
        private String sourceNodeUuid;

        @Schema(description = "源端口ID")
        private String sourcePortId;

        @Schema(description = "目标节点ID（数据库ID，查询时返回）")
        private Long targetNodeId;

        @Schema(description = "目标节点UUID（前端保存时使用）")
        private String targetNodeUuid;

        @Schema(description = "目标端口ID")
        private String targetPortId;

        @Schema(description = "源参数索引")
        private Integer sourceParamIndex;

        @Schema(description = "目标参数索引")
        private Integer targetParamIndex;

        @Schema(description = "标签")
        private String label;

        @Schema(description = "分支标签")
        private String branchLabel;

        @Schema(description = "分支优先级")
        private Integer branchPriority;
    }

    /**
     * 关联DTO
     */
    @Data
    @Schema(description = "关联信息")
    public static class AssociationDTO {
        @Schema(description = "关联ID")
        private Long id;

        @Schema(description = "容器节点ID（数据库ID）")
        private Long containerNodeId;

        @Schema(description = "容器节点UUID（前端使用）")
        private String containerNodeUuid;

        @Schema(description = "子节点ID（数据库ID）")
        private Long bodyNodeId;

        @Schema(description = "子节点UUID（前端使用）")
        private String bodyNodeUuid;

        @Schema(description = "关联类型：LOOP_BODY/BATCH_BODY/ASYNC_BODY")
        private String associationType;
    }
}
