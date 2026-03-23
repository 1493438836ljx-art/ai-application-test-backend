package com.example.demo.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 快捷问题实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_quick_question")
public class ChatQuickQuestionEntity extends BaseEntity {

    @TableField("icon")
    private String icon;

    @TableField("text")
    private String text;

    @TableField("category")
    private String category;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("enabled")
    private Boolean enabled;
}
