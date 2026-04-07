package com.huawei.cloudopenlabs.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 连线创建请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ConnectionCreateRequest {

    @NotNull(message = "工作流ID不能为空")
    private Long workflowId;

    @NotBlank(message = "连线UUID不能为空")
    private String connectionUuid;

    @NotNull(message = "源节点ID不能为空")
    private Long sourceNodeId;

    @NotBlank(message = "源端口ID不能为空")
    private String sourcePortId;

    @NotNull(message = "目标节点ID不能为空")
    private Long targetNodeId;

    @NotBlank(message = "目标端口ID不能为空")
    private String targetPortId;

    private Integer sourceParamIndex;

    private Integer targetParamIndex;

    private String label;
}
