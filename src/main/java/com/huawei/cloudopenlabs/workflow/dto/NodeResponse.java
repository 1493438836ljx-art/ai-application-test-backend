package com.huawei.cloudopenlabs.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Schema(description = "节点响应")
public class NodeResponse {

    @Schema(description = "节点ID")
    private Long id;

    @Schema(description = "节点UUID")
    private String nodeUuid;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点分类：BASIC/LOGIC/EXECUTION")
    private String nodeCategory;

    @Schema(description = "X坐标")
    private Integer positionX;

    @Schema(description = "Y坐标")
    private Integer positionY;

    // ========== Skill引用 ==========

    @Schema(description = "引用的Skill ID")
    private String skillId;

    @Schema(description = "Skill快照（JSON格式）")
    private String skillSnapshot;

    // ========== 端口配置 ==========

    @Schema(description = "输入端口（JSON格式）")
    private String inputPorts;

    @Schema(description = "输出端口（JSON格式）")
    private String outputPorts;

    // ========== 参数配置 ==========

    @Schema(description = "输入参数（JSON格式）")
    private String inputParams;

    @Schema(description = "输出参数（JSON格式）")
    private String outputParams;

    // ========== 执行配置 ==========

    @Schema(description = "执行位置：CLIENT/SERVICE")
    private String executionLocation;

    @Schema(description = "错误策略：STOP/SKIP/RETRY/ERROR_BRANCH")
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

    @Schema(description = "兼容性状态：COMPATIBLE/NEEDS_UPDATE/INCOMPATIBLE/INVALID")
    private String compatibilityStatus;

    // ========== 时间字段 ==========

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
