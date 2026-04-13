/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流连线实体类
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@TableName("workflow_connection")
public class WorkflowConnectionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 所属工作流ID
     */
    @TableField("workflow_id")
    private String workflowId;

    /**
     * 连线UUID
     */
    @TableField("connection_uuid")
    private String connectionUuid;

    /**
     * 源节点ID
     */
    @TableField("source_node_id")
    private String sourceNodeId;

    /**
     * 源端口ID
     */
    @TableField("source_port_id")
    private String sourcePortId;

    /**
     * 目标节点ID
     */
    @TableField("target_node_id")
    private String targetNodeId;

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
     * 分支标签（true/false/case1/case2/default）
     */
    @TableField("branch_label")
    private String branchLabel;

    /**
     * 分支优先级
     */
    @TableField("branch_priority")
    private Integer branchPriority;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
