/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.service;

import com.huawei.cloudopenlabs.chat.dto.ActionConfirmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 待确认操作管理服务
 * <p>
 * 管理 AI 请求执行的非查询操作，等待用户确认后再执行
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Service
public class PendingActionService {

    /**
     * 待确认操作存储
     * Key: pendingActionId, Value: 待确认操作信息
     */
    private final ConcurrentHashMap<String, PendingAction> pendingActions = new ConcurrentHashMap<>();

    /**
     * 待确认操作超时时间（毫秒）
     * 默认 5 分钟
     */
    private static final long PENDING_ACTION_TIMEOUT_MS = 5 * 60 * 1000;

    /**
     * 存储待确认操作
     *
     * @param pendingActionId  待确认操作ID
     * @param conversationId   会话ID
     * @param workflowId       工作流ID
     * @param actions          操作列表
     * @param emitter          SSE 发射器
     * @param objectMapper     JSON 对象映射器
     */
    public void storePendingAction(String pendingActionId, String conversationId, String workflowId,
                                   List<Map<String, Object>> actions, SseEmitter emitter,
                                   com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        PendingAction pending = new PendingAction();
        pending.setPendingActionId(pendingActionId);
        pending.setConversationId(conversationId);
        pending.setWorkflowId(workflowId);
        pending.setActions(actions);
        pending.setEmitter(emitter);
        pending.setObjectMapper(objectMapper);
        pending.setCreatedAt(System.currentTimeMillis());

        pendingActions.put(pendingActionId, pending);
        log.info("Stored pending action: pendingActionId={}, conversationId={}, actionCount={}",
                pendingActionId, conversationId, actions.size());
    }

    /**
     * 获取待确认操作
     *
     * @param pendingActionId 待确认操作ID
     * @return 待确认操作，如果不存在或已过期则返回 null
     */
    public PendingAction getPendingAction(String pendingActionId) {
        PendingAction pending = pendingActions.get(pendingActionId);
        if (pending == null) {
            return null;
        }

        // 检查是否超时
        if (System.currentTimeMillis() - pending.getCreatedAt() > PENDING_ACTION_TIMEOUT_MS) {
            log.warn("Pending action expired: pendingActionId={}", pendingActionId);
            pendingActions.remove(pendingActionId);
            return null;
        }

        return pending;
    }

    /**
     * 移除待确认操作
     *
     * @param pendingActionId 待确认操作ID
     */
    public void removePendingAction(String pendingActionId) {
        pendingActions.remove(pendingActionId);
        log.info("Removed pending action: pendingActionId={}", pendingActionId);
    }

    /**
     * 检查待确认操作是否存在
     *
     * @param pendingActionId 待确认操作ID
     * @return 是否存在
     */
    public boolean exists(String pendingActionId) {
        return pendingActionId != null && pendingActions.containsKey(pendingActionId);
    }

    /**
     * 生成待确认操作ID
     *
     * @return UUID 格式的 ID
     */
    public String generatePendingActionId() {
        return "pending-" + UUID.randomUUID().toString();
    }

    /**
     * 生成操作摘要列表
     *
     * @param actions 操作列表
     * @return 摘要列表
     */
    public List<ActionConfirmResponse.ActionSummary> generateActionSummary(List<Map<String, Object>> actions) {
        List<ActionConfirmResponse.ActionSummary> summaryList = new ArrayList<>();

        for (Map<String, Object> action : actions) {
            String bodyPreview = null;
            Object body = action.get("body");
            if (body != null) {
                try {
                    String bodyStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
                    bodyPreview = bodyStr.length() > 200 ? bodyStr.substring(0, 200) + "..." : bodyStr;
                } catch (Exception e) {
                    bodyPreview = body.toString();
                }
            }

            summaryList.add(ActionConfirmResponse.ActionSummary.builder()
                    .id((String) action.get("id"))
                    .method((String) action.get("method"))
                    .path((String) action.get("path"))
                    .description((String) action.get("description"))
                    .bodyPreview(bodyPreview)
                    .build());
        }

        return summaryList;
    }

    /**
     * 通过 SSE 发送确认请求
     *
     * @param emitter          SSE 发射器
     * @param pendingActionId  待确认操作ID
     * @param actions          操作列表
     * @param objectMapper     JSON 对象映射器
     */
    public void sendConfirmationRequest(SseEmitter emitter, String pendingActionId,
                                        List<Map<String, Object>> actions,
                                        com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        try {
            Map<String, Object> confirmData = new HashMap<>();
            confirmData.put("type", "confirmation_required");
            confirmData.put("pendingActionId", pendingActionId);
            confirmData.put("actionSummary", generateActionSummary(actions));
            confirmData.put("message", "即将执行 " + actions.size() + " 个修改操作，请确认是否继续？");

            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(confirmData)));
            log.info("Confirmation request sent: pendingActionId={}", pendingActionId);
        } catch (IOException e) {
            log.error("Failed to send confirmation request: {}", e.getMessage());
        }
    }

    /**
     * 通过 SSE 发送操作结果
     *
     * @param emitter      SSE 发射器
     * @param success      是否成功
     * @param message      消息
     * @param results      执行结果
     * @param objectMapper JSON 对象映射器
     */
    public void sendActionResult(SseEmitter emitter, boolean success, String message,
                                 Map<String, Object> results,
                                 com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        try {
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("type", success ? "action_result" : "action_error");
            resultData.put("success", success);
            resultData.put("message", message);
            if (results != null) {
                resultData.put("results", results);
            }

            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(resultData)));
            log.info("Action result sent: success={}", success);
        } catch (IOException e) {
            log.error("Failed to send action result: {}", e.getMessage());
        }
    }

    /**
     * 清理过期的待确认操作
     */
    public void cleanupExpiredActions() {
        long now = System.currentTimeMillis();
        List<String> expiredIds = new ArrayList<>();

        for (Map.Entry<String, PendingAction> entry : pendingActions.entrySet()) {
            if (now - entry.getValue().getCreatedAt() > PENDING_ACTION_TIMEOUT_MS) {
                expiredIds.add(entry.getKey());
            }
        }

        for (String id : expiredIds) {
            PendingAction pending = pendingActions.remove(id);
            if (pending != null && pending.getEmitter() != null) {
                try {
                    Map<String, Object> timeoutData = new HashMap<>();
                    timeoutData.put("type", "action_timeout");
                    timeoutData.put("message", "确认超时，操作已自动取消");

                    pending.getEmitter().send(SseEmitter.event().name("message")
                            .data(pending.getObjectMapper().writeValueAsString(timeoutData)));
                } catch (Exception e) {
                    log.error("Failed to send timeout notification: {}", e.getMessage());
                }
            }
            log.info("Cleaned up expired pending action: {}", id);
        }
    }

    /**
     * 待确认操作实体类
     */
    @lombok.Data
    public static class PendingAction {
        private String pendingActionId;
        private String conversationId;
        private String workflowId;
        private List<Map<String, Object>> actions;
        private SseEmitter emitter;
        private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
        private long createdAt;
    }
}
