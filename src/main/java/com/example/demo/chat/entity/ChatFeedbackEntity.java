package com.example.demo.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI反馈实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("chat_feedback")
public class ChatFeedbackEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("message_id")
    private Long messageId;

    @TableField("user_id")
    private String userId;

    @TableField("rating")
    private Integer rating;

    @TableField("feedback_type")
    private String feedbackType;

    @TableField("comment")
    private String comment;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
