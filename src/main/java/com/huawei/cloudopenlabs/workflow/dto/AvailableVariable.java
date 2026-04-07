package com.huawei.cloudopenlabs.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可用变量DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Schema(description = "可用变量")
public class AvailableVariable {

    @Schema(description = "节点UUID")
    private String nodeUuid;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "参数名称")
    private String paramName;

    @Schema(description = "参数类型")
    private String paramType;

    @Schema(description = "参数描述")
    private String description;
}
