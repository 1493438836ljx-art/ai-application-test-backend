package com.example.demo.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流节点实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("workflow_node")
public class WorkflowNodeEntity implements Serializable {

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
     * 节点UUID
     */
    @TableField("node_uuid")
    private String nodeUuid;

    /**
     * 节点类型编码
     */
    @TableField("type")
    private String type;

    /**
     * 节点类型ID
     */
    @TableField("type_id")
    private Long typeId;

    /**
     * 节点名称
     */
    @TableField("name")
    private String name;

    /**
     * 画布X坐标
     */
    @TableField("position_x")
    private Integer positionX;

    /**
     * 画布Y坐标
     */
    @TableField("position_y")
    private Integer positionY;

    /**
     * 输入端口定义
     */
    @TableField("input_ports")
    private String inputPorts;

    /**
     * 输出端口定义
     */
    @TableField("output_ports")
    private String outputPorts;

    /**
     * 输入参数定义
     */
    @TableField("input_params")
    private String inputParams;

    /**
     * 输出参数定义
     */
    @TableField("output_params")
    private String outputParams;

    /**
     * 节点配置参数
     */
    @TableField("config")
    private String config;

    /**
     * 父节点ID
     */
    @TableField("parent_node_id")
    private Long parentNodeId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updatedAt;
}
