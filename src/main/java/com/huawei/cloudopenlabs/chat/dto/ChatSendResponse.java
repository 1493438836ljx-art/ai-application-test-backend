/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.dto;

import lombok.Data;

/**
 * 发送消息响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
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
        private String id;

        private String messageUuid;

        private String role;

        private String content;

        private String contentType;

        private String createdAt;
    }
}
