/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 操作确认响应
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@Builder
public class ActionConfirmResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 待确认操作ID
     */
    private String pendingActionId;

    /**
     * 执行结果（如果已执行）
     */
    private Map<String, Object> results;

    /**
     * 执行ID（如果是执行工作流的操作）
     */
    private Object executionId;

    /**
     * 待确认的操作摘要列表
     */
    private List<ActionSummary> actionSummary;

    /**
     * 操作摘要
     */
    @Data
    @Builder
    public static class ActionSummary {
        private String id;
        private String method;
        private String path;
        private String description;
        private String bodyPreview;
    }
}
