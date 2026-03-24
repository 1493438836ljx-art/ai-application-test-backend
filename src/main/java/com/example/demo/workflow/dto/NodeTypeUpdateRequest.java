package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 节点类型更新请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "节点类型更新请求")
public class NodeTypeUpdateRequest {

    @Size(max = 100, message = "节点类型名称长度不能超过100")
    @Schema(description = "节点类型名称", example = "文本清洗")
    private String name;

    @Size(max = 50, message = "分类长度不能超过50")
    @Schema(description = "分类", example = "data-process")
    private String category;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "描述", example = "对文本进行清洗处理")
    private String description;

    @Size(max = 100, message = "图标长度不能超过100")
    @Schema(description = "图标", example = "clean-icon")
    private String icon;

    @Schema(description = "默认配置（JSON格式）", example = "{}")
    private String defaultConfig;

    @Schema(description = "默认输入端口（JSON格式）", example = "[{\"id\":\"input-1\",\"name\":\"输入\"}]")
    private String inputPorts;

    @Schema(description = "默认输出端口（JSON格式）", example = "[{\"id\":\"output-1\",\"name\":\"输出\"}]")
    private String outputPorts;

    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
