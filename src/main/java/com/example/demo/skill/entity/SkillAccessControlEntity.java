package com.example.demo.skill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * Skill access control entity class
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("skill_access_control")
public class SkillAccessControlEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key, UUID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * Associated Skill ID
     */
    @TableField("skill_id")
    private String skillId;

    /**
     * Target type: USER/PROJECT
     */
    @TableField("target_type")
    private String targetType;

    /**
     * Target ID
     */
    @TableField("target_id")
    private String targetId;
}
