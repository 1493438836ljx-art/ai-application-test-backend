package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 连线创建请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "连线创建请求")
public class ConnectionCreateRequest {

    @NotNull(message = "工作流ID不能为空")
    @Schema(description = "工作流ID", example = "1")
    private Long workflowId;

    @NotBlank(message = "连线UUID不能为空")
    @Schema(description = "连线UUID", example = "conn-123")
    private String connectionUuid;

    @NotNull(message = "源节点ID不能为空")
    @Schema(description = "源节点ID", example = "1")
    private Long sourceNodeId;

    @NotBlank(message = "源端口ID不能为空")
    @Schema(description = "源端口ID", example = "out-1")
    private String sourcePortId;

    @NotNull(message = "目标节点ID不能为空")
    @Schema(description = "目标节点ID", example = "2")
    private Long targetNodeId;

    @NotBlank(message = "目标端口ID不能为空")
    @Schema(description = "目标端口ID", example = "in-1")
    private String targetPortId;

    @Schema(description = "源参数索引")
    private Integer sourceParamIndex;

    @Schema(description = "目标参数索引")
    private Integer targetParamIndex;

    @Schema(description = "连线标签")
    private String label;
}
