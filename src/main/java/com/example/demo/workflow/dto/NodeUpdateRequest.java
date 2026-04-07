package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 节点更新请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Schema(description = "节点更新请求")
public class NodeUpdateRequest {

    @NotBlank(message = "节点UUID不能为空")
    @Schema(description = "节点UUID", example = "node-123")
    private String nodeUuid;

    @Size(max = 100, message = "节点名称长度不能超过100")
    @Schema(description = "节点名称", example = "开始节点")
    private String nodeName;

    @Schema(description = "X坐标", example = "100")
    private Integer positionX;

    @Schema(description = "Y坐标", example = "200")
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

    @Schema(description = "执行位置：CLIENT/SERVICE", example = "SERVICE")
    private String executionLocation;

    @Schema(description = "错误策略：STOP/SKIP/RETRY/ERROR_BRANCH", example = "STOP")
    private String errorStrategy;

    @Schema(description = "重试次数", example = "3")
    private Integer retryCount;

    @Schema(description = "重试间隔（毫秒）", example = "1000")
    private Integer retryInterval;

    @Schema(description = "错误分支节点ID")
    private Long errorBranchId;

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

    // ========== 节点配置 ==========

    @Schema(description = "节点配置（JSON格式）")
    private String config;
}
