package com.example.demo.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流关联实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("workflow_association")
public class WorkflowAssociationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属工作流ID
     */
    @TableField("workflow_id")
    private Long workflowId;

    /**
     * 容器节点ID（原loop_node_id）
     */
    @TableField("container_node_id")
    private Long containerNodeId;

    /**
     * 容器节点UUID
     */
    @TableField("container_node_uuid")
    private String containerNodeUuid;

    /**
     * 子节点ID（原body_node_id）
     */
    @TableField("body_node_id")
    private Long bodyNodeId;

    /**
     * 子节点UUID
     */
    @TableField("body_node_uuid")
    private String bodyNodeUuid;

    /**
     * 关联类型
     */
    @TableField("association_type")
    private String associationType;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
