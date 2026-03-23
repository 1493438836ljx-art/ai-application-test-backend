package com.example.demo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "对话响应")
public class ConversationDTO {

    @Schema(description = "对话ID")
    private Long id;

    @Schema(description = "对话UUID")
    private String conversationUuid;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "对话标题")
    private String title;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "消息数量")
    private Integer messageCount;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "消息列表")
    private List<MessageDTO> messages;

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

        @Schema(description = "Token数量")
        private Integer tokens;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}
