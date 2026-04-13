/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 关联创建请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class AssociationCreateRequest {

    @NotBlank(message = "容器节点UUID不能为空")
    private String containerNodeUuid;

    @NotBlank(message = "子节点UUID不能为空")
    private String bodyNodeUuid;

    private String associationType = "LOOP_BODY";
}
