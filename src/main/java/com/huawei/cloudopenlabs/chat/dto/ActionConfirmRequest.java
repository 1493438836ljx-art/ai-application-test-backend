/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 操作确认请求
 * 当 AI 请求执行非查询操作时，需要用户确认
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ActionConfirmRequest {

    /**
     * 待确认操作ID
     */
    private String pendingActionId;

    /**
     * 是否确认执行
     */
    private Boolean confirmed;

    /**
     * 会话ID（用于恢复执行上下文）
     */
    private String conversationId;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 待确认的操作列表
     * （可选，如果前端需要修改操作内容）
     */
    private List<ActionItem> actions;

    /**
     * 操作项
     */
    @Data
    public static class ActionItem {
        private String id;
        private String method;
        private String path;
        private String description;
        private Map<String, Object> body;
    }
}
