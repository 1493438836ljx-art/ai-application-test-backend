package com.example.demo.dictionary.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 数据字典实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_dictionary")
public class DataDictionary extends BaseEntity {

    /**
     * 数据字典名称
     */
    @TableField("name")
    private String name;

    /**
     * 字典描述
     */
    @TableField("description")
    private String description;

    /**
     * 逻辑删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 关联的字段定义列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DictionaryColumn> columns;
}
