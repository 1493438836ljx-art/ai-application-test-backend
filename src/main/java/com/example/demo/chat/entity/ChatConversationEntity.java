package com.example.demo.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI对话实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
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
