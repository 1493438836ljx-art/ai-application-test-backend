package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
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

    @Valid
    @Schema(description = "节点列表")
    private List<WorkflowResponse.NodeDTO> nodes = new ArrayList<>();

    @Valid
    @Schema(description = "连线列表")
    private List<WorkflowResponse.ConnectionDTO> connections = new ArrayList<>();

    @Valid
    @Schema(description = "关联列表")
    private List<WorkflowResponse.AssociationDTO> associations = new ArrayList<>();
}
