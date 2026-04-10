/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 连线响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ConnectionResponse {

    private String id;

    private String connectionUuid;

    private String sourceNodeUuid;

    private String targetNodeUuid;

    private String sourcePort;

    private String targetPort;

    private String branchLabel;

    private Integer branchPriority;

    private LocalDateTime createdAt;
}
