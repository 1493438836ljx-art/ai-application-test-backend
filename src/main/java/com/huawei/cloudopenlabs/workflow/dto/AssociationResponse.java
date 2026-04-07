package com.huawei.cloudopenlabs.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关联响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class AssociationResponse {

    private Long id;

    private String containerNodeUuid;

    private String bodyNodeUuid;

    private String associationType;

    private LocalDateTime createdAt;
}
