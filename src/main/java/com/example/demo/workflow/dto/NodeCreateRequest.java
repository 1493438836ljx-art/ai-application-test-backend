package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 节点创建请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "节点创建请求")
public class NodeCreateRequest {

    @NotNull(message = "工作流ID不能为空")
    @Schema(description = "工作流ID", example = "1")
    private Long workflowId;

    @NotBlank(message = "节点UUID不能为空")
    @Schema(description = "节点UUID", example = "node-123")
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

    @Schema(description = "输入端口（JSON格式）")
    private String inputPorts;

    @Schema(description = "输出端口（JSON格式）")
    private String outputPorts;

    @Schema(description = "输入参数（JSON格式）")
    private String inputParams;

    @Schema(description = "输出参数（JSON格式）")
    private String outputParams;

    @Schema(description = "配置（JSON格式）")
    private String config;

    @Schema(description = "父节点ID（用于循环体内节点）")
    private Long parentNodeId;
}
