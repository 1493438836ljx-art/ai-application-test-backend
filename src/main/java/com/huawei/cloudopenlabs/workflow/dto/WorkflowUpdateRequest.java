/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工作流更新请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class WorkflowUpdateRequest {

    @Size(max = 100, message = "工作流名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    private String triggerType;

    private String triggerConfig;

    private String updatedBy;
}
