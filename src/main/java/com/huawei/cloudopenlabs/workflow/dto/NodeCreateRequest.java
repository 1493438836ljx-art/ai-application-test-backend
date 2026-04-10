/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 节点创建请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class NodeCreateRequest {

    @NotNull(message = "工作流ID不能为空")
    private String workflowId;

    @NotBlank(message = "节点UUID不能为空")
    private String nodeUuid;

    @NotBlank(message = "节点类型不能为空")
    private String type;

    @NotBlank(message = "节点名称不能为空")
    private String name;

    private Integer positionX = 0;

    private Integer positionY = 0;

    private String inputPorts;

    private String outputPorts;

    private String inputParams;

    private String outputParams;

    private String config;

    private String parentNodeId;

    private String nodeCategory;

    // ========== Skill 引用 ==========

    private String skillId;

    private String skillSnapshot;
}
