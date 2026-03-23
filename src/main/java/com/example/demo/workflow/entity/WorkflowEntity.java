package com.example.demo.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow")
public class WorkflowEntity extends BaseEntity {

    /**
     * 工作流名称
     */
    @TableField("name")
    private String name;

    /**
     * 工作流描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否已发布
     */
    @TableField("published")
    private Boolean published;

    /**
     * 是否已运行
     */
    @TableField("has_run")
    private Boolean hasRun;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
