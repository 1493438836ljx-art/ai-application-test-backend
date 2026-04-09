package com.huawei.cloudopenlabs.workflow.dto;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeTypeEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点类型响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class NodeTypeResponse {

    private String id;

    private String code;

    private String name;

    private String category;

    private String description;

    private String icon;

    private String color;

    private String defaultConfig;

    private String inputPorts;

    private String outputPorts;

    private Integer sortOrder;

    private Boolean enabled;

    private LocalDateTime createdAt;

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
