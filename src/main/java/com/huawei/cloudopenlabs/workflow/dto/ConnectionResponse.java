package com.huawei.cloudopenlabs.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 连线响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Schema(description = "连线响应")
public class ConnectionResponse {

    @Schema(description = "连线ID")
    private Long id;

    @Schema(description = "连线UUID")
    private String connectionUuid;

    @Schema(description = "源节点UUID")
    private String sourceNodeUuid;

    @Schema(description = "目标节点UUID")
    private String targetNodeUuid;

    @Schema(description = "源端口ID")
    private String sourcePort;

    @Schema(description = "目标端口ID")
    private String targetPort;

    @Schema(description = "分支标签（true/false/case1/case2/default）")
    private String branchLabel;

    @Schema(description = "分支优先级")
    private Integer branchPriority;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
