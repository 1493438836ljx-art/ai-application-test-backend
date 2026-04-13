/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 发送消息请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class ChatSendRequest {

    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Map<String, Object> context;

    private String userId;
}
