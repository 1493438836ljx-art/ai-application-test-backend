package com.example.demo.skill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * Skill参数实体类（合并入参和出参）
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("skill_parameter")
public class SkillParameterEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，UUID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 关联的Skill ID
     */
    @TableField("skill_id")
    private String skillId;

    /**
     * 参数方向：INPUT/OUTPUT
     */
    @TableField("param_direction")
    private String paramDirection;

    /**
     * 参数顺序（从1开始）
     */
    @TableField("param_order")
    private Integer paramOrder;

    /**
     * 参数类型
     */
    @TableField("param_type")
    private String paramType;

    /**
     * 参数名称
     */
    @TableField("param_name")
    private String paramName;

    /**
     * 默认值（仅入参使用）
     */
    @TableField("default_value")
    private String defaultValue;

    /**
     * 参数描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否必填
     */
    @TableField("required")
    private Boolean required;
}
