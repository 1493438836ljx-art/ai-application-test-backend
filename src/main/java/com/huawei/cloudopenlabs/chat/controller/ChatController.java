/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.controller;

import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.framework.AgentExecutor;
import com.huawei.cloudopenlabs.chat.dto.*;
import com.huawei.cloudopenlabs.chat.entity.ConversationStatus;
import com.huawei.cloudopenlabs.chat.service.ChatService;
import com.huawei.cloudopenlabs.chat.service.PendingActionService;
import com.huawei.cloudopenlabs.workflow.service.WorkflowService;
import com.huawei.cloudopenlabs.workflow.service.WorkflowExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI聊天控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final PendingActionService pendingActionService;
    private final WorkflowService workflowService;
    private final WorkflowExecutionService workflowExecutionService;
    private final ObjectMapper objectMapper;
    private final AgentExecutor agentExecutor;

    /**
     * 发送消息
     *
     * @param request 发送请求
     * @return 发送响应
     */
    @PostMapping("/send")
    public ResponseEntity<ChatSendResponse> sendMessage(@Valid @RequestBody ChatSendRequest request) {
        log.info("发送消息: {}", request.getMessage());
        ChatSendResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 流式发送消息（SSE）
     *
     * @param request 发送请求
     * @return SSE事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@Valid @RequestBody ChatSendRequest request) {
        log.info("流式发送消息: {}", request.getMessage());
        return chatService.streamMessageRealtime(request);  // 使用真正的流式方法
    }

    /**
     * 创建新对话
     *
     * @param userId 用户ID
     * @param title  对话标题
     * @return 对话DTO
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationDTO> createConversation(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String title) {
        ConversationDTO response = chatService.createConversation(userId, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取对话列表
     *
     * @param userId 用户ID
     * @param status 状态
     * @param page   页码
     * @param size   每页大小
     * @return 对话分页列表
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationDTO>> getConversations(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) ConversationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<ConversationDTO> response = chatService.getConversations(userId, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取对话详情
     *
     * @param uuid 对话UUID
     * @return 对话DTO
     */
    @GetMapping("/conversations/{uuid}")
    public ResponseEntity<ConversationDTO> getConversation(
            @PathVariable String uuid) {
        ConversationDTO response = chatService.getConversation(uuid);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新对话标题
     *
     * @param uuid  对话UUID
     * @param title 新标题
     * @return 对话DTO
     */
    @PutMapping("/conversations/{uuid}")
    public ResponseEntity<ConversationDTO> updateConversationTitle(
            @PathVariable String uuid,
            @RequestParam String title) {
        ConversationDTO response = chatService.updateConversationTitle(uuid, title);
        return ResponseEntity.ok(response);
    }

    /**
     * 归档对话
     *
     * @param uuid 对话UUID
     * @return 无内容响应
     */
    @PostMapping("/conversations/{uuid}/archive")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable String uuid) {
        chatService.archiveConversation(uuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除对话
     *
     * @param uuid 对话UUID
     * @return 无内容响应
     */
    @DeleteMapping("/conversations/{uuid}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String uuid) {
        chatService.deleteConversation(uuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取快捷问题列表
     *
     * @return 快捷问题列表
     */
    @GetMapping("/quick-questions")
    public ResponseEntity<List<QuickQuestionDTO>> getQuickQuestions() {
        List<QuickQuestionDTO> response = chatService.getQuickQuestions();
        return ResponseEntity.ok(response);
    }

    /**
     * 提交消息反馈
     *
     * @param messageUuid 消息UUID
     * @param request     反馈请求
     * @return 无内容响应
     */
    @PostMapping("/messages/{messageUuid}/feedback")
    public ResponseEntity<Void> submitFeedback(
            @PathVariable String messageUuid,
            @Valid @RequestBody FeedbackRequest request) {
        chatService.submitFeedback(messageUuid, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 知操作确认
     * <p>
     * 当 AI 请求执行非查询操作时，前端会收到 confirmation_required 事件。
     * 用户确认后，前端调用此 API 执行操作。
     * </p>
     *
     * @param request 确认请求
     * @return 确认响应
     */
    @PostMapping("/action/confirm")
    public ResponseEntity<ActionConfirmResponse> confirmAction(
            @Valid @RequestBody ActionConfirmRequest request) {
        log.info("收到操作确认请求: pendingActionId={}, confirmed={}",
                request.getPendingActionId(), request.getConfirmed());

        if (request.getPendingActionId() == null || request.getPendingActionId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ActionConfirmResponse.builder()
                            .success(false)
                            .message("pendingActionId 不能为空")
                            .build());
        }

        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            // 用户拒绝执行
            log.info("用户拒绝执行操作: pendingActionId={}", request.getPendingActionId());
            pendingActionService.removePendingAction(request.getPendingActionId());
            return ResponseEntity.ok(
                    ActionConfirmResponse.builder()
                            .success(true)
                            .message("操作已取消")
                            .pendingActionId(request.getPendingActionId())
                            .build());
        }

        // 获取待确认操作
        PendingActionService.PendingAction pendingAction =
                pendingActionService.getPendingAction(request.getPendingActionId());

        if (pendingAction == null) {
            log.warn("待确认操作不存在或已过期: pendingActionId={}", request.getPendingActionId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ActionConfirmResponse.builder()
                            .success(false)
                            .message("待确认操作不存在或已过期")
                            .pendingActionId(request.getPendingActionId())
                            .build());
        }

        try {
            // 执行操作
            Map<String, Object> results = executePendingActions(pendingAction);

            log.info("操作执行成功: pendingActionId={}, resultCount={}",
                    request.getPendingActionId(), results.size());

            // 通过 SSE 发送操作结果给前端
            pendingActionService.sendActionResult(
                    pendingAction.getEmitter(),
                    true,
                    "操作已执行完成",
                    results,
                    pendingAction.getObjectMapper()
            );

            // 继续调用 AgentExecutor 让 AI 生成总结
            continueAgentConversation(pendingAction, results);

            // 清理待确认操作
            pendingActionService.removePendingAction(request.getPendingActionId());

            // 从 results 中提取 executionId（如果有）
            Object executionId = null;
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<?, ?> actionResult = (Map<?, ?>) entry.getValue();
                    if (actionResult.containsKey("executionId")) {
                        executionId = actionResult.get("executionId");
                        log.info("从操作结果中提取到 executionId: {}", executionId);
                        break;
                    }
                }
            }

            ActionConfirmResponse.ActionConfirmResponseBuilder responseBuilder = ActionConfirmResponse.builder()
                    .success(true)
                    .message("操作执行成功")
                    .pendingActionId(request.getPendingActionId())
                    .results(results);

            if (executionId != null) {
                responseBuilder.executionId(executionId);
            }

            return ResponseEntity.ok(responseBuilder.build());

        } catch (Exception e) {
            log.error("操作执行失败: pendingActionId={}, error={}",
                    request.getPendingActionId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ActionConfirmResponse.builder()
                            .success(false)
                            .message("操作执行失败: " + e.getMessage())
                            .pendingActionId(request.getPendingActionId())
                            .build());
        }
    }

    /**
     * 获取待确认操作详情
     *
     * @param pendingActionId 待确认操作ID
     * @return 操作详情
     */
    @GetMapping("/action/pending/{pendingActionId}")
    public ResponseEntity<ActionConfirmResponse> getPendingAction(@PathVariable String pendingActionId) {
        PendingActionService.PendingAction pendingAction =
                pendingActionService.getPendingAction(pendingActionId);

        if (pendingAction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ActionConfirmResponse.builder()
                            .success(false)
                            .message("待确认操作不存在或已过期")
                            .pendingActionId(pendingActionId)
                            .build());
        }

        return ResponseEntity.ok(
                ActionConfirmResponse.builder()
                        .success(true)
                        .message("获取成功")
                        .pendingActionId(pendingActionId)
                        .actionSummary(pendingActionService.generateActionSummary(pendingAction.getActions()))
                        .build());
    }

    /**
     * 执行待确认的操作
     */
    private Map<String, Object> executePendingActions(PendingActionService.PendingAction pendingAction) {
        Map<String, Object> results = new java.util.LinkedHashMap<>();

        for (Map<String, Object> action : pendingAction.getActions()) {
            String actionId = (String) action.get("id");
            String method = (String) action.get("method");
            String path = (String) action.get("path");
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) action.get("body");

            log.info("执行操作: id={}, method={}, path={}", actionId, method, path);

            try {
                Object result = executeWorkflowAction(method, path, body, pendingAction.getWorkflowId());
                results.put(actionId, result);
            } catch (Exception e) {
                log.error("操作执行失败: id={}, error={}", actionId, e.getMessage());
                Map<String, Object> errorResult = new java.util.LinkedHashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", e.getMessage());
                results.put(actionId, errorResult);
            }
        }

        return results;
    }

    /**
     * 执行工作流操作（动态调用后端 API）
     * 使用 WebClient 内部调用，支持所有已注册的 API 端点
     */
    private Object executeWorkflowAction(String method, String path, Map<String, Object> body, String workflowId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();

        try {
            if (path == null || path.isEmpty()) {
                result.put("success", false);
                result.put("error", "路径为空");
                return result;
            }

            log.info("动态执行 API 调用: method={}, path={}, workflowId={}", method, path, workflowId);

            // 使用 WebClient 内部调用
            org.springframework.web.reactive.function.client.WebClient webClient =
                org.springframework.web.reactive.function.client.WebClient.create("http://localhost:8080");

            reactor.core.publisher.Mono<String> responseMono;

            switch (method.toUpperCase()) {
                case "POST":
                    responseMono = webClient.post()
                        .uri(path)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .bodyValue(body != null ? body : new java.util.HashMap<>())
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                case "PUT":
                    responseMono = webClient.put()
                        .uri(path)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .bodyValue(body != null ? body : new java.util.HashMap<>())
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                case "PATCH":
                    responseMono = webClient.patch()
                        .uri(path)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .bodyValue(body != null ? body : new java.util.HashMap<>())
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                case "DELETE":
                    responseMono = webClient.delete()
                        .uri(path)
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                default:
                    log.warn("不支持的 HTTP 方法: {}", method);
                    result.put("success", false);
                    result.put("error", "不支持的 HTTP 方法: " + method);
                    return result;
            }

            // 同步等待结果
            String responseBody = responseMono.block(java.time.Duration.ofSeconds(30));

            result.put("success", true);
            result.put("method", method);
            result.put("path", path);
            result.put("workflowId", workflowId);

            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    Object responseData = objectMapper.readValue(responseBody, Object.class);
                    result.put("data", responseData);

                    // 如果是执行工作流的 API，返回 executionId
                    if (responseData instanceof Map) {
                        Object executionId = ((Map<?, ?>) responseData).get("executionId");
                        if (executionId != null) {
                            result.put("executionId", executionId);
                            log.info("提取到 executionId: {}", executionId);
                        }
                    } else if (responseData instanceof Number) {
                        // 如果响应是纯数字，则直接作为 executionId
                        result.put("executionId", responseData);
                        log.info("响应为纯数字 executionId: {}", responseData);
                    }
                } catch (Exception e) {
                    // 尝试解析为数字（可能是纯数字响应）
                    try {
                        String executionId = responseBody.trim();
                        result.put("executionId", executionId);
                        result.put("data", responseBody);
                        log.info("解析纯数字响应为 executionId: {}", executionId);
                    } catch (Exception nfe) {
                        result.put("data", responseBody);
                    }
                }
            }
            result.put("message", "操作执行成功");
            log.info("API 调用成功: method={}, path={}", method, path);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("API 调用错误: method={}, path={}, status={}, error={}",
                    method, path, e.getStatusCode(), e.getMessage());
            result.put("success", false);
            result.put("statusCode", e.getStatusCode().value());
            result.put("error", "HTTP 错误: " + e.getStatusText());
            try {
                result.put("details", objectMapper.readValue(e.getResponseBodyAsString(), Object.class));
            } catch (Exception ex) {
                result.put("details", e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.error("操作执行异常: method={}, path={}, error={}", method, path, e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 继续调用 Agent 对话，让 AI 生成操作完成后的总结
     *
     * @param pendingAction 待确认操作
     * @param results       操作执行结果
     */
    private void continueAgentConversation(PendingActionService.PendingAction pendingAction,
                                           Map<String, Object> results) {
        try {
            log.info("继续 AI 对话生成总结: conversationId={}, workflowId={}",
                    pendingAction.getConversationId(), pendingAction.getWorkflowId());

            // 构建操作结果上下文
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("【操作已确认执行】\n\n");
            contextBuilder.append("用户已确认执行以下操作，操作结果如下：\n\n");

            for (Map.Entry<String, Object> entry : results.entrySet()) {
                contextBuilder.append("操作 ").append(entry.getKey()).append(": ");
                if (entry.getValue() instanceof Map) {
                    Map<?, ?> resultMap = (Map<?, ?>) entry.getValue();
                    Boolean success = (Boolean) resultMap.get("success");
                    if (Boolean.TRUE.equals(success)) {
                        contextBuilder.append("执行成功\n");
                    } else {
                        contextBuilder.append("执行失败: ").append(resultMap.get("error")).append("\n");
                    }
                } else {
                    contextBuilder.append(entry.getValue()).append("\n");
                }
            }

            contextBuilder.append("\n请根据操作结果生成最终总结回复给用户。");

            // 调用 AgentExecutor 继续对话
            agentExecutor.processMessageStream(
                    contextBuilder.toString(),
                    pendingAction.getWorkflowId(),
                    pendingAction.getConversationId(),
                    new AgentExecutor.StreamCallback() {
                        @Override
                        public void onStart(String sessionId) {
                            log.debug("AI 总结会话开始: sessionId={}", sessionId);
                        }

                        @Override
                        public void onChunk(StreamChunk chunk) {
                            try {
                                String content = chunk.getContentOrMessage();
                                String contentType = chunk.getContentType();

                                if (content != null && !content.trim().isEmpty()) {
                                    // 过滤内部调试信息
                                    if (content.startsWith("[思考]") ||
                                        content.startsWith("[工具调用]") ||
                                        content.startsWith("[工具结果]") ||
                                        "user".equals(contentType)) {
                                        return;
                                    }

                                    // 检查 content 是否为 JSON 格式，如果是则提取 summary 字段
                                    String displayContent = content;
                                    if (content.trim().startsWith("{") && content.trim().endsWith("}")) {
                                        try {
                                            Map<String, Object> jsonContent = pendingAction.getObjectMapper()
                                                    .readValue(content, Map.class);
                                            // 如果包含 summary 字段，使用 summary 作为显示内容
                                            if (jsonContent.containsKey("summary")) {
                                                displayContent = (String) jsonContent.get("summary");
                                                log.debug("从 JSON 中提取 summary: {}", displayContent);
                                            }
                                        } catch (Exception e) {
                                            // 解析失败，保持原内容
                                            log.debug("content 不是有效的 JSON，保持原样: {}", content);
                                        }
                                    }

                                    // 发送 chunk 给前端
                                    Map<String, Object> chunkData = new java.util.LinkedHashMap<>();
                                    chunkData.put("type", "chunk");
                                    chunkData.put("content", displayContent);
                                    chunkData.put("contentType", contentType != null ? contentType : "assistant");

                                    pendingAction.getEmitter().send(
                                            SseEmitter.event().name("message")
                                                    .data(pendingAction.getObjectMapper().writeValueAsString(chunkData)));
                                }
                            } catch (Exception e) {
                                log.error("发送 chunk 失败: {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onDone(String sessionId, Long duration) {
                            try {
                                log.info("AI 总结完成: sessionId={}, duration={}ms", sessionId, duration);

                                // 发送 done 事件
                                Map<String, Object> doneData = new java.util.LinkedHashMap<>();
                                doneData.put("type", "done");
                                doneData.put("conversationId", pendingAction.getConversationId());

                                pendingAction.getEmitter().send(
                                        SseEmitter.event().name("message")
                                                .data(pendingAction.getObjectMapper().writeValueAsString(doneData)));

                                // 关闭 SSE 连接
                                pendingAction.getEmitter().complete();
                            } catch (Exception e) {
                                log.error("发送 done 事件失败: {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onError(String error) {
                            try {
                                log.error("AI 总结失败: {}", error);

                                // 发送错误事件
                                Map<String, Object> errorData = new java.util.LinkedHashMap<>();
                                errorData.put("type", "error");
                                errorData.put("message", error);

                                pendingAction.getEmitter().send(
                                        SseEmitter.event().name("message")
                                                .data(pendingAction.getObjectMapper().writeValueAsString(errorData)));

                                // 关闭 SSE 连接
                                pendingAction.getEmitter().complete();
                            } catch (Exception e) {
                                log.error("发送 error 事件失败: {}", e.getMessage());
                            }
                        }
                    }
            );

        } catch (Exception e) {
            log.error("继续 AI 对话失败: {}", e.getMessage(), e);
            // 即使失败，也要关闭 SSE 连接
            try {
                pendingAction.getEmitter().complete();
            } catch (Exception ex) {
                log.error("关闭 SSE 连接失败: {}", ex.getMessage());
            }
        }
    }
}
