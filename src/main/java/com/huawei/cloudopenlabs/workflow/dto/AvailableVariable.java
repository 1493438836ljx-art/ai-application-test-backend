/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import lombok.Data;

/**
 * 可用变量DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class AvailableVariable {

    private String nodeUuid;

    private String nodeName;

    private String paramName;

    private String paramType;

    private String description;
}
