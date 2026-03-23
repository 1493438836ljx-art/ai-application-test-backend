package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作流数据请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "工作流数据请求")
public class WorkflowDataRequest {

    @Schema(description = "节点列表")
    private List<WorkflowResponse.NodeDTO> nodes;

    @Schema(description = "连线列表")
    private List<WorkflowResponse.ConnectionDTO> connections;

    @Schema(description = "关联列表")
    private List<WorkflowResponse.AssociationDTO> associations;
}
