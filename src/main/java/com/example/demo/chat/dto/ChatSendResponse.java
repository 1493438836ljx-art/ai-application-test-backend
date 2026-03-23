package com.example.demo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送消息响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "发送消息响应")
public class ChatSendResponse {

    @Schema(description = "对话UUID")
    private String conversationId;

    @Schema(description = "用户消息")
    private MessageDTO userMessage;

    @Schema(description = "AI助手消息")
    private MessageDTO assistantMessage;

    /**
     * 消息DTO
     */
    @Data
    @Schema(description = "消息信息")
    public static class MessageDTO {
        @Schema(description = "消息ID")
        private Long id;

        @Schema(description = "消息UUID")
        private String messageUuid;

        @Schema(description = "角色")
        private String role;

        @Schema(description = "消息内容")
        private String content;

        @Schema(description = "内容类型")
        private String contentType;

        @Schema(description = "创建时间")
        private String createdAt;
    }
}
