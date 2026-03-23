package com.example.demo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 发送消息请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "发送消息请求")
public class ChatSendRequest {

    @Schema(description = "对话UUID（为空则创建新对话）")
    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", required = true, example = "如何创建测评集？")
    private String message;

    @Schema(description = "上下文信息")
    private Map<String, Object> context;

    @Schema(description = "用户ID")
    private String userId;
}
