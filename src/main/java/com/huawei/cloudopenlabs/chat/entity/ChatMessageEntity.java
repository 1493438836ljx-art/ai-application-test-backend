/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI消息实体类
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@TableName("chat_message")
public class ChatMessageEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("message_uuid")
    private String messageUuid;

    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    @TableField("content_type")
    private String contentType;

    @TableField("tokens")
    private Integer tokens;

    @TableField("model")
    private String model;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("metadata")
    private String metadata;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
