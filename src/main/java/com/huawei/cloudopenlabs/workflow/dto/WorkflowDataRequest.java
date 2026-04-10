/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流数据请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class WorkflowDataRequest {

    @Valid
    private List<WorkflowResponse.NodeDTO> nodes = new ArrayList<>();

    @Valid
    private List<WorkflowResponse.ConnectionDTO> connections = new ArrayList<>();

    @Valid
    private List<WorkflowResponse.AssociationDTO> associations = new ArrayList<>();
}
