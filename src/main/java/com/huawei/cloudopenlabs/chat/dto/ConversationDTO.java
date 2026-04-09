package com.huawei.cloudopenlabs.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ConversationDTO {

    private String id;

    private String conversationUuid;

    private String userId;

    private String title;

    private String status;

    private Integer messageCount;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<MessageDTO> messages;

    /**
     * 消息DTO
     */
    @Data
    public static class MessageDTO {
        private String id;

        private String messageUuid;

        private String role;

        private String content;

        private String contentType;

        private Integer tokens;

        private LocalDateTime createdAt;
    }
}
