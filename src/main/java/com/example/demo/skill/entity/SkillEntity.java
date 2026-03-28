package com.example.demo.skill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Skill entity class
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("skill")
public class SkillEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key, UUID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * Skill name, globally unique
     */
    @TableField("name")
    private String name;

    /**
     * Skill description
     */
    @TableField("description")
    private String description;

    /**
     * Executable suite file path
     */
    @TableField("suite_path")
    private String suitePath;

    /**
     * Executable suite file name (with extension)
     */
    @TableField("suite_filename")
    private String suiteFilename;

    /**
     * Execution type: AUTOMATED/AI
     */
    @TableField("execution_type")
    private String executionType;

    /**
     * Category: SYSTEM/USER
     */
    @TableField("category")
    private String category;

    /**
     * Access control: PUBLIC/PRIVATE/WHITELIST/PROJECT
     */
    @TableField("access_type")
    private String accessType;

    /**
     * Is container
     */
    @TableField("is_container")
    private Boolean isContainer;

    /**
     * Allow add input parameters dynamically
     */
    @TableField("allow_add_input_params")
    private Boolean allowAddInputParams;

    /**
     * Allow add output parameters dynamically
     */
    @TableField("allow_add_output_params")
    private Boolean allowAddOutputParams;

    /**
     * Status: PUBLISHED/DRAFT
     */
    @TableField("status")
    private String status;

    /**
     * Creator
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * Updater
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * Created at
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Updated at
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * Logical deletion flag
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    /**
     * Deletion time (for aging cleanup)
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
