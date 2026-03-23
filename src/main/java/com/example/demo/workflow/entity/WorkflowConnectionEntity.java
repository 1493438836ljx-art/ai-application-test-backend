package com.example.demo.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流连线实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("workflow_connection")
public class WorkflowConnectionEntity implements Serializable {

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
     * 连线UUID
     */
    @TableField("connection_uuid")
    private String connectionUuid;

    /**
     * 源节点ID
     */
    @TableField("source_node_id")
    private Long sourceNodeId;

    /**
     * 源端口ID
     */
    @TableField("source_port_id")
    private String sourcePortId;

    /**
     * 目标节点ID
     */
    @TableField("target_node_id")
    private Long targetNodeId;

    /**
     * 目标端口ID
     */
    @TableField("target_port_id")
    private String targetPortId;

    /**
     * 源参数索引
     */
    @TableField("source_param_index")
    private Integer sourceParamIndex;

    /**
     * 目标参数索引
     */
    @TableField("target_param_index")
    private Integer targetParamIndex;

    /**
     * 连线标签
     */
    @TableField("label")
    private String label;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
