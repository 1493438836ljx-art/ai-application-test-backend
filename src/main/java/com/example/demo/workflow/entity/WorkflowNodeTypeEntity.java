package com.example.demo.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 节点类型实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("workflow_node_type")
public class WorkflowNodeTypeEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 节点类型编码
     */
    @TableField("code")
    private String code;

    /**
     * 节点类型名称
     */
    @TableField("name")
    private String name;

    /**
     * 分类
     */
    @TableField("category")
    private String category;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 颜色
     * 注意：数据库表暂无此列，使用exist=false跳过持久化
     */
    @TableField(value = "color", exist = false)
    private String color;

    /**
     * 默认配置
     */
    @TableField("default_config")
    private String defaultConfig;

    /**
     * 默认输入端口
     */
    @TableField("input_ports")
    private String inputPorts;

    /**
     * 默认输出端口
     */
    @TableField("output_ports")
    private String outputPorts;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
