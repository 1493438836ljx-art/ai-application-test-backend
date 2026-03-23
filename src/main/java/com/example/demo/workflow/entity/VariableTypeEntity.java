package com.example.demo.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 变量类型实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_variable_type")
public class VariableTypeEntity extends BaseEntity {

    /**
     * 变量类型编码
     */
    @TableField("code")
    private String code;

    /**
     * 变量类型名称
     */
    @TableField("name")
    private String name;

    /**
     * 分类 (BASIC/COMPOSITE)
     */
    @TableField("category")
    private String category;

    /**
     * 元素类型（用于数组类型）
     */
    @TableField("element_type")
    private String elementType;

    /**
     * 文件类型（用于文件类型）
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 数据字典类型（用于Dictionary类型）
     */
    @TableField("dictionary_type")
    private String dictionaryType;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

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
}
