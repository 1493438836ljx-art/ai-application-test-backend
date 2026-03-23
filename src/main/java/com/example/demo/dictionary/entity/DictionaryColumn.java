package com.example.demo.dictionary.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 字段定义实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dictionary_column")
public class DictionaryColumn extends BaseEntity {

    /**
     * 关联的数据字典ID
     */
    @TableField("dictionary_id")
    private Long dictionaryId;

    /**
     * 字段Key（英文标识）
     */
    @TableField("column_key")
    private String columnKey;

    /**
     * 字段名称（中文显示名）
     */
    @TableField("column_label")
    private String columnLabel;

    /**
     * 字段类型：string/number/enum
     */
    @TableField("column_type")
    private String columnType;

    /**
     * 枚举选项（JSON数组格式，仅enum类型使用）
     */
    @TableField("enum_options")
    private String enumOptions;

    /**
     * 最小值（仅number类型使用）
     */
    @TableField("min_value")
    private BigDecimal minValue;

    /**
     * 最大值（仅number类型使用）
     */
    @TableField("max_value")
    private BigDecimal maxValue;

    /**
     * 字段排序序号
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
