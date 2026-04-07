package com.huawei.cloudopenlabs.chat.dto;

import lombok.Data;

/**
 * 发送消息响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ChatSendResponse {

    private String conversationId;

    private MessageDTO userMessage;

    private MessageDTO assistantMessage;

    /**
     * 消息DTO
     */
    @Data
    public static class MessageDTO {
        private Long id;

        private String messageUuid;

        private String role;

        private String content;

        private String contentType;

        private String createdAt;
    }
}
