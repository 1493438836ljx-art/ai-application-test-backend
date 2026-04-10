/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huawei.cloudopenlabs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI对话实体类
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_conversation")
public class ChatConversationEntity extends BaseEntity {

    @TableField("conversation_uuid")
    private String conversationUuid;

    @TableField("user_id")
    private String userId;

    @TableField("title")
    private String title;

    @TableField("status")
    private String status;

    @TableField("message_count")
    private Integer messageCount;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("metadata")
    private String metadata;
}
