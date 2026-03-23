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
     * 循环节点ID
     */
    @TableField("loop_node_id")
    private Long loopNodeId;

    /**
     * 循环体节点ID
     */
    @TableField("body_node_id")
    private Long bodyNodeId;

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
