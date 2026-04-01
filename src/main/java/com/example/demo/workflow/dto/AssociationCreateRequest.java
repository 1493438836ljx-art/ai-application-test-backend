package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 关联创建请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "关联创建请求")
public class AssociationCreateRequest {

    @NotBlank(message = "容器节点UUID不能为空")
    @Schema(description = "容器节点UUID（循环/批处理/异步节点）", example = "node-loop")
    private String containerNodeUuid;

    @NotBlank(message = "子节点UUID不能为空")
    @Schema(description = "子节点UUID（循环体/批处理体/异步体）", example = "node-loop-body")
    private String bodyNodeUuid;

    @Schema(description = "关联类型：LOOP_BODY/BATCH_BODY/ASYNC_BODY", example = "LOOP_BODY")
    private String associationType = "LOOP_BODY";
}
