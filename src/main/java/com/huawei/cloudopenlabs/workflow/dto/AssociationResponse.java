/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关联响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class AssociationResponse {

    private String id;

    private String containerNodeUuid;

    private String bodyNodeUuid;

    private String associationType;

    private LocalDateTime createdAt;
}
