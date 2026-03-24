package com.example.demo.workflow.dto;

import com.example.demo.workflow.entity.WorkflowNodeTypeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点类型响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "节点类型响应")
public class NodeTypeResponse {

    @Schema(description = "节点类型ID")
    private Long id;

    @Schema(description = "节点类型编码")
    private String code;

    @Schema(description = "节点类型名称")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "颜色")
    private String color;

    @Schema(description = "默认配置")
    private String defaultConfig;

    @Schema(description = "默认输入端口")
    private String inputPorts;

    @Schema(description = "默认输出端口")
    private String outputPorts;

    @Schema(description = "排序顺序")
    private Integer sortOrder;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为响应DTO
     */
    public static NodeTypeResponse fromEntity(WorkflowNodeTypeEntity entity) {
        if (entity == null) {
            return null;
        }
        NodeTypeResponse response = new NodeTypeResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setCategory(entity.getCategory());
        response.setDescription(entity.getDescription());
        response.setIcon(entity.getIcon());
        response.setColor(entity.getColor());
        response.setDefaultConfig(entity.getDefaultConfig());
        response.setInputPorts(entity.getInputPorts());
        response.setOutputPorts(entity.getOutputPorts());
        response.setSortOrder(entity.getSortOrder());
        response.setEnabled(entity.getEnabled());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
