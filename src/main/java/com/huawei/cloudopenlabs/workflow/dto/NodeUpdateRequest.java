package com.huawei.cloudopenlabs.workflow.dto;

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
public class NodeUpdateRequest {

    @NotBlank(message = "节点UUID不能为空")
    private String nodeUuid;

    @Size(max = 100, message = "节点名称长度不能超过100")
    private String nodeName;

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

    // ========== 节点配置 ==========

    private String config;
}
