package com.huawei.cloudopenlabs.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关联响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Schema(description = "关联响应")
public class AssociationResponse {

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "容器节点UUID")
    private String containerNodeUuid;

    @Schema(description = "子节点UUID")
    private String bodyNodeUuid;

    @Schema(description = "关联类型：LOOP_BODY/BATCH_BODY/ASYNC_BODY")
    private String associationType;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
