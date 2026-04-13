/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huawei.cloudopenlabs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 快捷问题实体类
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
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
