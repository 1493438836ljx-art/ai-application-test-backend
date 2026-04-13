/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 节点类型更新请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class NodeTypeUpdateRequest {

    @Size(max = 100, message = "节点类型名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @Size(max = 100, message = "图标长度不能超过100")
    private String icon;

    private String defaultConfig;

    private String inputPorts;

    private String outputPorts;

    private Integer sortOrder;

    private Boolean enabled;
}
