/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huawei.cloudopenlabs.agent.dto.AgentResponse;
import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.framework.AgentExecutor;
import com.huawei.cloudopenlabs.chat.dto.*;
import com.huawei.cloudopenlabs.chat.entity.*;
import com.huawei.cloudopenlabs.chat.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.chat.dto.*;
import com.huawei.cloudopenlabs.chat.entity.*;
import com.huawei.cloudopenlabs.chat.mapper.ChatConversationMapper;
import com.huawei.cloudopenlabs.chat.mapper.ChatFeedbackMapper;
import com.huawei.cloudopenlabs.chat.mapper.ChatMessageMapper;
import com.huawei.cloudopenlabs.chat.mapper.ChatQuickQuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * AI聊天服务实现类 (MyBatis-Plus版本)
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final ChatQuickQuestionMapper quickQuestionMapper;

    private AgentExecutor agentExecutor;
    private PendingActionService pendingActionService;

    /**
     * 内容去重器：存储每个会话已发送内容的哈希值
     * Key: conversationId, Value: 已发送内容的 MD5 哈希集合
     */
    private final ConcurrentHashMap<String, Set<String>> contentDedupMap = new ConcurrentHashMap<>();

    /**
     * 最小去重长度：短于此长度的内容不去重
     */
    private static final int MIN_DEDUP_LENGTH = 20;

    // AI回复模板（Agent失败时的备用回复）
    private static final List<String> FALLBACK_REPLIES = List.of(
            "抱歉，AI服务暂时不可用，请稍后再试。",
            "系统繁忙中，请稍后再试。"
    );

    @Autowired
    public ChatServiceImpl(ChatConversationMapper conversationMapper,
                           ChatMessageMapper messageMapper,
                           ChatFeedbackMapper feedbackMapper,
                           ChatQuickQuestionMapper quickQuestionMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.feedbackMapper = feedbackMapper;
        this.quickQuestionMapper = quickQuestionMapper;
    }

    @Autowired(required = false)
    public void setAgentExecutor(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    @Autowired(required = false)
    public void setPendingActionService(PendingActionService pendingActionService) {
        this.pendingActionService = pendingActionService;
    }

    /**
     * 流式发送消息（真正的端到端流式）
     * 使用 WebClient 流式调用 AI 服务，实时转发给前端
     */
    @Override
    public SseEmitter streamMessageRealtime(ChatSendRequest request) {
        log.info("实时流式发送消息: conversationId={}, message={}", request.getConversationId(), request.getMessage());

        // 创建 SSE 发射器，超时 10 分钟
        SseEmitter emitter = new SseEmitter(600000L);
        ObjectMapper objectMapper = new ObjectMapper();

        // 从 context 中提取 workflowId
        String workflowId = extractWorkflowId(request.getContext());
        boolean isNewConversation = request.getConversationId() == null || request.getConversationId().isEmpty();

        // 创建或获取对话
        ChatConversationEntity conversation;
        if (isNewConversation) {
            conversation = createConversationEntity(request.getUserId(), generateTitle(request.getMessage()));
        } else {
            conversation = conversationMapper.selectByConversationUuid(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("对话不存在: " + request.getConversationId()));
        }

        // 创建用户消息
        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setConversationId(conversation.getId());
        userMessage.setMessageUuid(UUID.randomUUID().toString());
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage());
        userMessage.setContentType("text");
        messageMapper.insert(userMessage);

        StringBuilder fullContent = new StringBuilder();
        long[] startTime = {System.currentTimeMillis()};
        String[] finalSessionId = {conversation.getConversationUuid()};

        // 使用流式 API
        agentExecutor.processMessageStream(
                request.getMessage(),
                workflowId,
                isNewConversation ? null : conversation.getConversationUuid(),
                new AgentExecutor.StreamCallback() {

                    @Override
                    public void onStart(String sessionId) {
                        log.info("流式会话开始: sessionId={}", sessionId);
                        finalSessionId[0] = sessionId;

                        // 如果是新会话，异步更新对话的 UUID（非阻塞）
                        if (isNewConversation && sessionId != null) {
                            conversation.setConversationUuid(sessionId);
                            // 异步执行数据库更新，不阻塞响应式流
                            Mono.fromRunnable(() -> {
                                try {
                                    conversationMapper.updateById(conversation);
                                    log.info("对话UUID更新成功: {}", sessionId);
                                } catch (Exception e) {
                                    log.error("更新对话UUID失败: {}", e.getMessage());
                                }
                            }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        }

                        // 发送 start 事件
                        try {
                            sendStartEvent(emitter, objectMapper, sessionId);
                        } catch (Exception e) {
                            log.error("发送 start 事件失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onChunk(StreamChunk chunk) {
                        try {
                            String content = chunk.getContentOrMessage();
                            String contentType = chunk.getContentType();
                            log.info("onChunk 回调被调用，内容类型: {}, 内容长度: {}",
                                    contentType, content != null ? content.length() : 0);

                            // 过滤空内容
                            if (content == null || content.trim().isEmpty()) {
                                log.debug("跳过空内容: contentType={}", contentType);
                                return;
                            }

                            // 过滤内部调试信息：[思考]、[工具调用]、[工具结果] 等标记的内容
                            if (content.startsWith("[思考]") ||
                                content.startsWith("[工具调用]") ||
                                content.startsWith("[工具结果]") ||
                                content.contains("💭 思考:") ||
                                content.contains("📤 工具结果:")) {
                                log.debug("过滤内部调试信息: contentType={}, preview={}",
                                        contentType, content.substring(0, Math.min(50, content.length())));
                                return;
                            }

                            // 过滤 user 类型（工具结果）
                            if ("user".equals(contentType)) {
                                log.debug("过滤 user 类型内容（工具结果）");
                                return;
                            }

                            // 只处理 assistant、text、result 类型
                            String displayContent = null;
                            if ("assistant".equals(contentType) || "text".equals(contentType)) {
                                // assistant 和 text 类型：提取 reasoning 和 summary
                                displayContent = extractReasoningAndSummary(content);
                            } else if ("result".equals(contentType)) {
                                // result 类型：最终结果，提取 reasoning 和 summary
                                displayContent = extractReasoningAndSummary(content);
                            }

                            // 如果没有有效内容，跳过
                            if (displayContent == null || displayContent.trim().isEmpty()) {
                                log.debug("跳过无效内容: contentType={}", contentType);
                                return;
                            }

                            // 内容去重检查（基于会话ID）
                            String sessionId = finalSessionId[0];
                            if (isDuplicateContent(sessionId, displayContent)) {
                                log.debug("跳过重复内容: contentType={}, length={}, preview={}",
                                        contentType, displayContent.length(),
                                        displayContent.substring(0, Math.min(50, displayContent.length())));
                                return;
                            }

                            // 过滤重复和冗余的内容片段
                            displayContent = filterDuplicateContent(displayContent);
                            if (displayContent == null || displayContent.trim().isEmpty()) {
                                log.debug("过滤后内容为空，跳过: contentType={}", contentType);
                                return;
                            }

                            // 构建发送给前端的数据
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", displayContent);
                            chunkData.put("contentType", contentType);

                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            log.info("SSE chunk 已发送到前端，contentType={}, contentLength={}", contentType, displayContent.length());

                            // 累积文本内容
                            fullContent.append(displayContent);
                        } catch (IOException e) {
                            log.error("发送 chunk 失败: {}", e.getMessage());
                        } catch (Exception e) {
                            log.error("发送 chunk 异常: {}", e.getMessage(), e);
                        }
                    }

                    @Override
                    public void onDone(String sessionId, Long duration) {
                        try {
                            log.info("流式会话完成: sessionId={}, duration={}ms", sessionId, duration);

                            // 清理去重集合
                            clearDedupSet(sessionId);

                            // 保存 AI 消息
                            saveAssistantMessage(conversation, fullContent.toString(),
                                    System.currentTimeMillis() - startTime[0]);

                            // 发送完成事件
                            sendDoneEvent(emitter, objectMapper, conversation);
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("处理 onDone 失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        try {
                            log.error("流式会话错误: {}", error);

                            // 清理去重集合
                            clearDedupSet(finalSessionId[0]);

                            // 保存包含错误的消息
                            String errorContent = fullContent.toString() + "\n\n❌ 错误: " + error;
                            saveAssistantMessage(conversation, errorContent,
                                    System.currentTimeMillis() - startTime[0]);

                            // 发送错误事件
                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("type", "error");
                            errorData.put("message", error);
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(errorData)));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("处理 onError 失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onConfirmationRequired(String pendingActionId,
                                                       java.util.List<Map<String, Object>> actions,
                                                       String workflowId) {
                        try {
                            log.info("收到操作确认请求: pendingActionId={}, actionCount={}, workflowId={}",
                                    pendingActionId, actions.size(), workflowId);

                            // 存储待确认操作（用于后续通过 /api/chat/action/confirm 执行）
                            pendingActionService.storePendingAction(
                                    pendingActionId,
                                    finalSessionId[0],
                                    workflowId,
                                    actions,
                                    emitter,
                                    objectMapper
                            );

                            // 生成操作摘要
                            java.util.List<Map<String, Object>> actionSummary = new java.util.ArrayList<>();
                            for (Map<String, Object> action : actions) {
                                Map<String, Object> summary = new java.util.LinkedHashMap<>();
                                summary.put("id", action.get("id"));
                                summary.put("method", action.get("method"));
                                summary.put("path", action.get("path"));
                                summary.put("description", action.get("description"));

                                // 生成 body 预览
                                Object body = action.get("body");
                                if (body != null) {
                                    String bodyStr = objectMapper.writeValueAsString(body);
                                    summary.put("bodyPreview", bodyStr.length() > 200 ? bodyStr.substring(0, 200) + "..." : bodyStr);
                                }

                                actionSummary.add(summary);
                            }

                            // 发送确认请求事件
                            Map<String, Object> confirmData = new java.util.LinkedHashMap<>();
                            confirmData.put("type", "confirmation_required");
                            confirmData.put("pendingActionId", pendingActionId);
                            confirmData.put("workflowId", workflowId);
                            confirmData.put("actionSummary", actionSummary);
                            confirmData.put("message", "即将执行 " + actions.size() + " 个修改操作，请确认是否继续？");

                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(confirmData)));
                            log.info("已发送确认请求到前端: pendingActionId={}", pendingActionId);

                            // 注意：不调用 emitter.complete()，保持连接打开，等待用户确认
                            // 用户确认后，前端会调用 /api/chat/action/confirm API
                        } catch (Exception e) {
                            log.error("处理 onConfirmationRequired 失败: {}", e.getMessage(), e);
                        }
                    }
                }
        );

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            // 清理去重集合
            clearDedupSet(finalSessionId[0]);
        });

        emitter.onCompletion(() -> {
            log.debug("SSE 连接完成");
            // 清理去重集合
            clearDedupSet(finalSessionId[0]);
        });

        return emitter;
    }

    @Override
    @Transactional
    public ChatSendResponse sendMessage(ChatSendRequest request) {
        log.info("发送消息: conversationId={}, message={}", request.getConversationId(), request.getMessage());

        ChatConversationEntity conversation;
        boolean isNewConversation = request.getConversationId() == null || request.getConversationId().isEmpty();

        // 获取或创建对话
        if (isNewConversation) {
            conversation = createConversationEntity(request.getUserId(), generateTitle(request.getMessage()));
        } else {
            conversation = conversationMapper.selectByConversationUuid(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("对话不存在: " + request.getConversationId()));
        }

        // 创建用户消息
        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setConversationId(conversation.getId());
        userMessage.setMessageUuid(UUID.randomUUID().toString());
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage());
        userMessage.setContentType("text");
        messageMapper.insert(userMessage);

        // 生成AI回复
        long startTime = System.currentTimeMillis();
        String aiContent = generateAIReply(request.getMessage());
        long latencyMs = System.currentTimeMillis() - startTime;

        // 创建AI消息
        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setMessageUuid(UUID.randomUUID().toString());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(aiContent);
        assistantMessage.setContentType("markdown");
        assistantMessage.setLatencyMs(latencyMs);
        messageMapper.insert(assistantMessage);

        // 更新对话信息
        conversation.setMessageCount(conversation.getMessageCount() + 2);
        conversation.setLastMessageAt(LocalDateTime.now());
        if (isNewConversation) {
            conversation.setTitle(generateTitle(request.getMessage()));
        }
        conversationMapper.updateById(conversation);

        // 构建响应
        ChatSendResponse response = new ChatSendResponse();
        response.setConversationId(conversation.getConversationUuid());
        response.setUserMessage(convertToMessageDTO(userMessage));
        response.setAssistantMessage(convertToMessageDTO(assistantMessage));

        return response;
    }

    @Override
    public SseEmitter streamMessage(ChatSendRequest request) {
        log.info("流式发送消息: conversationId={}, message={}", request.getConversationId(), request.getMessage());

        // 创建SSE发射器，超时时间5分钟
        SseEmitter emitter = new SseEmitter(300000L);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ObjectMapper objectMapper = new ObjectMapper();

        executor.execute(() -> {
            try {
                ChatConversationEntity conversation;
                boolean isNewConversation = request.getConversationId() == null || request.getConversationId().isEmpty();

                // 获取 workflowId（从 context 中）
                String workflowId = extractWorkflowId(request.getContext());

                // 多轮会话模式：新对话时不立即创建，等获取 sessionId 后再创建
                if (isNewConversation && agentExecutor != null && workflowId != null) {
                    // 多轮会话模式 - 新对话
                    // 先创建一个临时 conversation 用于存储用户消息
                    conversation = createConversationEntity(request.getUserId(), generateTitle(request.getMessage()));

                    // 创建用户消息
                    ChatMessageEntity userMessage = new ChatMessageEntity();
                    userMessage.setConversationId(conversation.getId());
                    userMessage.setMessageUuid(UUID.randomUUID().toString());
                    userMessage.setRole("user");
                    userMessage.setContent(request.getMessage());
                    userMessage.setContentType("text");
                    messageMapper.insert(userMessage);

                    // 处理多轮会话（会在回调中更新 conversationId）
                    processMultiRoundMessageWithNewSession(request.getMessage(), workflowId, conversation,
                            emitter, objectMapper, request.getUserId());

                } else if (!isNewConversation && agentExecutor != null && workflowId != null) {
                    // 多轮会话模式 - 已有对话
                    conversation = conversationMapper.selectByConversationUuid(request.getConversationId())
                            .orElseThrow(() -> new RuntimeException("对话不存在: " + request.getConversationId()));

                    // 创建用户消息
                    ChatMessageEntity userMessage = new ChatMessageEntity();
                    userMessage.setConversationId(conversation.getId());
                    userMessage.setMessageUuid(UUID.randomUUID().toString());
                    userMessage.setRole("user");
                    userMessage.setContent(request.getMessage());
                    userMessage.setContentType("text");
                    messageMapper.insert(userMessage);

                    // 发送对话ID
                    sendStartEvent(emitter, objectMapper, conversation.getConversationUuid());

                    // 处理多轮会话
                    processMultiRoundMessage(request.getMessage(), workflowId, conversation, emitter, objectMapper);

                } else {
                    // 传统单轮模式
                    if (isNewConversation) {
                        conversation = createConversationEntity(request.getUserId(), generateTitle(request.getMessage()));
                    } else {
                        conversation = conversationMapper.selectByConversationUuid(request.getConversationId())
                                .orElseThrow(() -> new RuntimeException("对话不存在: " + request.getConversationId()));
                    }

                    // 发送对话ID
                    sendStartEvent(emitter, objectMapper, conversation.getConversationUuid());

                    // 创建用户消息
                    ChatMessageEntity userMessage = new ChatMessageEntity();
                    userMessage.setConversationId(conversation.getId());
                    userMessage.setMessageUuid(UUID.randomUUID().toString());
                    userMessage.setRole("user");
                    userMessage.setContent(request.getMessage());
                    userMessage.setContentType("text");
                    messageMapper.insert(userMessage);

                    // 生成 AI 回复
                    long startTime = System.currentTimeMillis();
                    String fullContent = generateAIReply(request.getMessage());
                    long latencyMs = System.currentTimeMillis() - startTime;

                    // 流式发送内容
                    sendChunkedContent(emitter, objectMapper, fullContent);

                    // 保存AI消息到数据库
                    saveAssistantMessage(conversation, fullContent, latencyMs);

                    // 发送完成事件
                    sendDoneEvent(emitter, objectMapper, conversation);
                }

                emitter.complete();
            } catch (Exception e) {
                log.error("流式发送消息异常: {}", e.getMessage(), e);
                try {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("type", "error");
                    errorData.put("message", e.getMessage());
                    emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(errorData)));
                } catch (IOException ex) {
                    log.error("发送错误事件失败: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        executor.shutdown();

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            executor.shutdownNow();
        });

        emitter.onCompletion(() -> {
            log.debug("SSE连接完成");
        });

        return emitter;
    }

    /**
     * 处理新对话的多轮会话消息
     * 等待 Claude CLI 返回 sessionId 后，用它作为 conversationId
     */
    private void processMultiRoundMessageWithNewSession(String userMessage, String workflowId,
                                                         ChatConversationEntity tempConversation,
                                                         SseEmitter emitter, ObjectMapper objectMapper,
                                                         String userId) {
        StringBuilder fullContent = new StringBuilder();
        final long[] startTime = {System.currentTimeMillis()};
        final boolean[] messageSaved = {false};
        final boolean[] sessionCreated = {false};
        final ChatConversationEntity[] finalConversation = {tempConversation};

        agentExecutor.processMessage(
                userMessage,
                workflowId,
                null,  // conversationId 为 null，表示新会话
                new AgentExecutor.MultiRoundCallback() {

                    @Override
                    public void onSessionCreated(String sessionId) {
                        try {
                            log.info("收到新的 sessionId: {}, 将用作 conversationId", sessionId);

                            // 更新 conversation 的 UUID 为 sessionId
                            tempConversation.setConversationUuid(sessionId);
                            conversationMapper.updateById(tempConversation);

                            finalConversation[0] = tempConversation;
                            sessionCreated[0] = true;

                            // 发送 start 事件给前端
                            sendStartEvent(emitter, objectMapper, sessionId);

                        } catch (Exception e) {
                            log.error("处理 sessionId 失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onReasoning(String reasoning) {
                        try {
                            // 确保已发送 start 事件
                            if (!sessionCreated[0]) {
                                sendStartEvent(emitter, objectMapper, tempConversation.getConversationUuid());
                                sessionCreated[0] = true;
                            }

                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", "💭 " + reasoning + "\n\n");
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            fullContent.append("💭 ").append(reasoning).append("\n\n");
                        } catch (IOException e) {
                            log.error("发送推理内容失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onStatus(String status) {
                        try {
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", "⏳ " + status + "\n\n");
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            fullContent.append("⏳ ").append(status).append("\n\n");
                        } catch (IOException e) {
                            log.error("发送状态更新失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onWorkflowUpdate(Object result) {
                        try {
                            Map<String, Object> actionData = new HashMap<>();
                            actionData.put("type", "workflow_update");
                            actionData.put("workflowId", workflowId);
                            actionData.put("result", result);
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(actionData)));
                            log.info("发送工作流更新事件: workflowId={}", workflowId);
                        } catch (IOException e) {
                            log.error("发送工作流更新事件失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete(String summary, Object result) {
                        try {
                            long latencyMs = System.currentTimeMillis() - startTime[0];

                            if (summary != null && !summary.isBlank()) {
                                Map<String, Object> chunkData = new HashMap<>();
                                chunkData.put("type", "chunk");
                                chunkData.put("content", summary);
                                emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                                fullContent.append(summary);
                            }

                            saveAssistantMessage(finalConversation[0], fullContent.toString(), latencyMs);
                            messageSaved[0] = true;

                            sendDoneEvent(emitter, objectMapper, finalConversation[0]);

                        } catch (IOException e) {
                            log.error("发送完成事件失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        try {
                            long latencyMs = System.currentTimeMillis() - startTime[0];

                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", "\n❌ " + error + "\n\n");
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            fullContent.append("\n❌ ").append(error).append("\n\n");

                            if (!messageSaved[0]) {
                                saveAssistantMessage(finalConversation[0], fullContent.toString(), latencyMs);
                            }

                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("type", "error");
                            errorData.put("message", error);
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(errorData)));

                        } catch (IOException e) {
                            log.error("发送错误事件失败: {}", e.getMessage());
                        }
                    }
                }
        );
    }

    /**
     * 发送 start 事件
     */
    private void sendStartEvent(SseEmitter emitter, ObjectMapper objectMapper, String conversationId) throws IOException {
        Map<String, Object> startData = new HashMap<>();
        startData.put("type", "start");
        startData.put("conversationId", conversationId);
        emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(startData)));
    }

    /**
     * 处理多轮会话消息（已有会话）
     */
    private void processMultiRoundMessage(String userMessage, String workflowId,
                                           ChatConversationEntity conversation,
                                           SseEmitter emitter, ObjectMapper objectMapper) {
        StringBuilder fullContent = new StringBuilder();
        final long[] startTime = {System.currentTimeMillis()};
        final boolean[] messageSaved = {false};

        agentExecutor.processMessage(
                userMessage,
                workflowId,
                conversation.getConversationUuid(),
                new AgentExecutor.MultiRoundCallback() {

                    @Override
                    public void onSessionCreated(String sessionId) {
                        // 已有会话通常不会触发此回调，但以防万一
                        log.info("已有会话收到新的 sessionId: {}", sessionId);
                    }

                    @Override
                    public void onReasoning(String reasoning) {
                        try {
                            // 发送推理内容
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", "💭 " + reasoning + "\n\n");
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            fullContent.append("💭 ").append(reasoning).append("\n\n");
                        } catch (IOException e) {
                            log.error("发送推理内容失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onStatus(String status) {
                        try {
                            // 发送状态更新
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", "⏳ " + status + "\n\n");
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            fullContent.append("⏳ ").append(status).append("\n\n");
                        } catch (IOException e) {
                            log.error("发送状态更新失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onWorkflowUpdate(Object result) {
                        try {
                            // 发送工作流更新事件给前端
                            Map<String, Object> actionData = new HashMap<>();
                            actionData.put("type", "workflow_update");
                            actionData.put("workflowId", workflowId);
                            actionData.put("result", result);
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(actionData)));
                            log.info("发送工作流更新事件: workflowId={}", workflowId);
                        } catch (IOException e) {
                            log.error("发送工作流更新事件失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete(String summary, Object result) {
                        try {
                            long latencyMs = System.currentTimeMillis() - startTime[0];

                            // 发送最终摘要
                            if (summary != null && !summary.isBlank()) {
                                Map<String, Object> chunkData = new HashMap<>();
                                chunkData.put("type", "chunk");
                                chunkData.put("content", summary);
                                emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                                fullContent.append(summary);
                            }

                            // 保存AI消息到数据库
                            saveAssistantMessage(conversation, fullContent.toString(), latencyMs);
                            messageSaved[0] = true;

                            // 发送完成事件
                            sendDoneEvent(emitter, objectMapper, conversation);

                        } catch (IOException e) {
                            log.error("发送完成事件失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        try {
                            long latencyMs = System.currentTimeMillis() - startTime[0];

                            // 发送错误信息
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", "\n❌ " + error + "\n\n");
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            fullContent.append("\n❌ ").append(error).append("\n\n");

                            // 保存AI消息（包含错误信息）
                            if (!messageSaved[0]) {
                                saveAssistantMessage(conversation, fullContent.toString(), latencyMs);
                            }

                            // 发送错误事件
                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("type", "error");
                            errorData.put("message", error);
                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(errorData)));

                        } catch (IOException e) {
                            log.error("发送错误事件失败: {}", e.getMessage());
                        }
                    }
                }
        );
    }

    /**
     * 从 context 中提取 workflowId
     */
    private String extractWorkflowId(Object context) {
        if (context == null) {
            return null;
        }
        try {
            if (context instanceof Map) {
                Object workflowId = ((Map<?, ?>) context).get("workflowId");
                if (workflowId != null) {
                    return workflowId.toString();
                }
            }
        } catch (Exception e) {
            log.warn("提取 workflowId 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 流式发送内容
     */
    private void sendChunkedContent(SseEmitter emitter, ObjectMapper objectMapper, String content) throws IOException, InterruptedException {
        int chunkSize = 5; // 每次发送5个字符
        for (int i = 0; i < content.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, content.length());
            String chunk = content.substring(i, end);

            Map<String, Object> chunkData = new HashMap<>();
            chunkData.put("type", "chunk");
            chunkData.put("content", chunk);
            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));

            // 添加短暂延迟以模拟打字效果
            Thread.sleep(30);
        }
    }

    /**
     * 保存AI消息到数据库
     */
    private void saveAssistantMessage(ChatConversationEntity conversation, String content, long latencyMs) {
        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setMessageUuid(UUID.randomUUID().toString());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(content);
        assistantMessage.setContentType("markdown");
        assistantMessage.setLatencyMs(latencyMs);
        messageMapper.insert(assistantMessage);

        // 更新对话信息
        conversation.setMessageCount(conversation.getMessageCount() + 2);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    /**
     * 发送完成事件
     */
    private void sendDoneEvent(SseEmitter emitter, ObjectMapper objectMapper, ChatConversationEntity conversation) throws IOException {
        Map<String, Object> doneData = new HashMap<>();
        doneData.put("type", "done");
        doneData.put("conversationId", conversation.getConversationUuid());
        emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(doneData)));
    }

    @Override
    @Transactional
    public ConversationDTO createConversation(String userId, String title) {
        ChatConversationEntity conversation = createConversationEntity(userId, title);
        return convertToConversationDTO(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDTO getConversation(String uuid) {
        ChatConversationEntity conversation = conversationMapper.selectByConversationUuid(uuid)
                .orElseThrow(() -> new RuntimeException("对话不存在: " + uuid));
        return convertToConversationDTOWithMessages(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ConversationDTO> getConversations(String userId, ConversationStatus status, org.springframework.data.domain.Pageable pageable) {
        Page<ChatConversationEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        IPage<ChatConversationEntity> result;

        if (userId != null && !userId.isEmpty()) {
            if (status != null) {
                result = conversationMapper.selectByUserIdAndStatus(page, userId, status.name());
            } else {
                result = conversationMapper.selectByUserId(page, userId);
            }
        } else {
            if (status != null) {
                result = conversationMapper.selectByStatus(page, status.name());
            } else {
                result = conversationMapper.selectPage(page, new LambdaQueryWrapper<ChatConversationEntity>().orderByDesc(ChatConversationEntity::getLastMessageAt));
            }
        }

        return convertToSpringPage(result);
    }

    @Override
    @Transactional
    public ConversationDTO updateConversationTitle(String uuid, String title) {
        ChatConversationEntity conversation = conversationMapper.selectByConversationUuid(uuid)
                .orElseThrow(() -> new RuntimeException("对话不存在: " + uuid));
        conversation.setTitle(title);
        conversationMapper.updateById(conversation);
        return convertToConversationDTO(conversation);
    }

    @Override
    @Transactional
    public void archiveConversation(String uuid) {
        ChatConversationEntity conversation = conversationMapper.selectByConversationUuid(uuid)
                .orElseThrow(() -> new RuntimeException("对话不存在: " + uuid));
        conversation.setStatus(ConversationStatus.ARCHIVED.name());
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void deleteConversation(String uuid) {
        ChatConversationEntity conversation = conversationMapper.selectByConversationUuid(uuid)
                .orElseThrow(() -> new RuntimeException("对话不存在: " + uuid));
        conversation.setStatus(ConversationStatus.DELETED.name());
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuickQuestionDTO> getQuickQuestions() {
        return quickQuestionMapper.selectEnabled().stream()
                .map(this::convertToQuickQuestionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void submitFeedback(String messageUuid, FeedbackRequest request) {
        ChatMessageEntity message = messageMapper.selectByMessageUuid(messageUuid)
                .orElseThrow(() -> new RuntimeException("消息不存在: " + messageUuid));

        // 检查是否已有反馈
        Optional<ChatFeedbackEntity> existingFeedback = feedbackMapper.selectByMessageId(message.getId());
        ChatFeedbackEntity feedback = existingFeedback.orElseGet(() -> {
            ChatFeedbackEntity newFeedback = new ChatFeedbackEntity();
            newFeedback.setMessageId(message.getId());
            return newFeedback;
        });

        feedback.setUserId(request.getUserId());
        feedback.setRating(request.getRating());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setComment(request.getComment());

        if (existingFeedback.isPresent()) {
            feedbackMapper.updateById(feedback);
        } else {
            feedbackMapper.insert(feedback);
        }
    }

    // ========== 私有方法 ==========

    private ChatConversationEntity createConversationEntity(String userId, String title) {
        ChatConversationEntity conversation = new ChatConversationEntity();
        conversation.setConversationUuid(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setStatus(ConversationStatus.ACTIVE.name());
        conversation.setMessageCount(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isEmpty()) {
            return "新对话";
        }
        return firstMessage.length() > 30 ? firstMessage.substring(0, 30) + "..." : firstMessage;
    }

    private String generateAIReply(String userMessage) {
        // 尝试调用真实的AI Agent
        if (agentExecutor != null) {
            try {
                log.info("调用AI Agent处理消息: {}", userMessage);
                AgentResponse response = agentExecutor.executeSimple(userMessage);

                if (response.getSuccess() && response.getResponse() != null) {
                    log.info("AI Agent响应成功，耗时: {}ms", response.getExecutionTimeMs());
                    return response.getResponse();
                } else {
                    log.warn("AI Agent响应失败: {}", response.getError());
                    return getFallbackReply(userMessage, response.getError());
                }
            } catch (Exception e) {
                log.error("调用AI Agent异常: {}", e.getMessage(), e);
                return getFallbackReply(userMessage, e.getMessage());
            }
        } else {
            // Agent未启用，使用备用回复
            log.warn("AI Agent未启用，使用备用回复");
            return getFallbackReply(userMessage, "Agent服务未配置");
        }
    }

    /**
     * 获取备用回复（Agent失败时使用）
     */
    private String getFallbackReply(String userMessage, String error) {
        log.info("使用备用回复，原因: {}", error);
        int index = (int) (Math.random() * FALLBACK_REPLIES.size());
        return FALLBACK_REPLIES.get(index);
    }

    private ChatSendResponse.MessageDTO convertToMessageDTO(ChatMessageEntity message) {
        ChatSendResponse.MessageDTO dto = new ChatSendResponse.MessageDTO();
        dto.setId(message.getId());
        dto.setMessageUuid(message.getMessageUuid());
        dto.setRole(message.getRole().toLowerCase());
        dto.setContent(message.getContent());
        dto.setContentType(message.getContentType().toLowerCase());
        dto.setCreatedAt(message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return dto;
    }

    private ConversationDTO convertToConversationDTO(ChatConversationEntity conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setConversationUuid(conversation.getConversationUuid());
        dto.setUserId(conversation.getUserId());
        dto.setTitle(conversation.getTitle());
        dto.setStatus(conversation.getStatus());
        dto.setMessageCount(conversation.getMessageCount());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        return dto;
    }

    private ConversationDTO convertToConversationDTOWithMessages(ChatConversationEntity conversation) {
        ConversationDTO dto = convertToConversationDTO(conversation);
        List<ChatMessageEntity> messages = messageMapper.selectByConversationId(conversation.getId());
        dto.setMessages(messages.stream().map(this::convertToConversationMessageDTO).collect(Collectors.toList()));
        return dto;
    }

    private ConversationDTO.MessageDTO convertToConversationMessageDTO(ChatMessageEntity message) {
        ConversationDTO.MessageDTO dto = new ConversationDTO.MessageDTO();
        dto.setId(message.getId());
        dto.setMessageUuid(message.getMessageUuid());
        dto.setRole(message.getRole().toLowerCase());
        dto.setContent(message.getContent());
        dto.setContentType(message.getContentType().toLowerCase());
        dto.setTokens(message.getTokens());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    private QuickQuestionDTO convertToQuickQuestionDTO(ChatQuickQuestionEntity question) {
        QuickQuestionDTO dto = new QuickQuestionDTO();
        dto.setId(question.getId());
        dto.setIcon(question.getIcon());
        dto.setText(question.getText());
        dto.setCategory(question.getCategory());
        return dto;
    }

    private org.springframework.data.domain.Page<ConversationDTO> convertToSpringPage(IPage<ChatConversationEntity> mybatisPage) {
        List<ConversationDTO> content = mybatisPage.getRecords().stream()
                .map(this::convertToConversationDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(
                        (int) mybatisPage.getCurrent() - 1,
                        (int) mybatisPage.getSize()
                ),
                mybatisPage.getTotal()
        );
    }

    /**
     * 从内容中提取 reasoning 和 summary 属性
     * 处理两种情况：
     * 1. 纯 JSON 内容（以 { 开头）
     * 2. 包含 JSON 代码块的 markdown 内容（```json ... ```）
     * 如果 JSON 包含 reasoning 和/或 summary 属性，则只返回这两个属性的值
     * 否则返回原始内容
     */
    private String extractReasoningAndSummary(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String trimmedContent = content.trim();
        ObjectMapper localMapper = new ObjectMapper();

        // 情况1：纯 JSON 内容（以 { 开头）
        if (trimmedContent.startsWith("{")) {
            return parseAndExtract(localMapper, trimmedContent, content);
        }

        // 情况2：检查是否包含 markdown JSON 代码块
        // 匹配 ```json ... ``` 或 ``` ... ```
        java.util.regex.Pattern codeBlockPattern = java.util.regex.Pattern.compile(
                "```(?:json)?\\s*\\n?\\s*(\\{[\\s\\S]*?\\})\\s*\\n?\\s*```",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = codeBlockPattern.matcher(trimmedContent);

        StringBuffer result = new StringBuffer();
        boolean foundAndReplaced = false;

        while (matcher.find()) {
            String jsonStr = matcher.group(1);
            String extracted = parseAndExtract(localMapper, jsonStr, jsonStr);

            // 如果提取成功（返回的不是原始内容），则替换代码块
            if (!extracted.equals(jsonStr)) {
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(extracted));
                foundAndReplaced = true;
            } else {
                // 没有提取到 reasoning/summary，保留原始代码块
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        if (foundAndReplaced) {
            log.debug("从 markdown JSON 代码块中提取 reasoning 和 summary: 原长度={}, 提取后长度={}",
                    content.length(), result.length());
            return result.toString();
        }

        // 情况3：纯文本内容（非 JSON 格式），提取最后的总结部分
        // AI 常输出多个层次的内容：思考过程 → 中间状态 → 最终总结
        // 我们只需要展示最终总结给用户
        return extractFinalSummary(trimmedContent);
    }

    /**
     * 从纯文本内容中提取最终的总结部分
     * AI 输出通常遵循模式：思考过程 → 中间状态 → 最终结果
     * 此方法只保留最终结果，过滤中间过程
     */
    private String extractFinalSummary(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        // 按换行或句号分割成段落/句子
        String[] segments = content.split("(?<=[。！？\\n])\\s*");

        if (segments.length <= 1) {
            // 单段内容，直接返回
            return content;
        }

        // 过滤掉思考过程和中间状态的句子
        List<String> filteredSegments = new ArrayList<>();
        Set<String> seenKeywords = new HashSet<>();

        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 跳过明显的思考过程句子
            if (isThinkingProcess(trimmed)) {
                log.debug("过滤思考过程: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                continue;
            }

            // 跳过重复的关键词句子
            String keyPhrase = extractKeyPhrase(trimmed);
            if (keyPhrase != null && seenKeywords.contains(keyPhrase)) {
                log.debug("过滤重复关键词句子: {}", keyPhrase);
                continue;
            }
            if (keyPhrase != null) {
                seenKeywords.add(keyPhrase);
            }

            filteredSegments.add(trimmed);
        }

        // 如果过滤后只剩一句，直接返回
        if (filteredSegments.size() <= 1) {
            return String.join(" ", filteredSegments);
        }

        // 如果有多句，优先返回最后一句（通常是最终总结）
        // 但也要保留上下文
        String lastSegment = filteredSegments.get(filteredSegments.size() - 1);

        // 检查最后一句是否是完整的总结
        if (isSummaryLike(lastSegment)) {
            return lastSegment;
        }

        // 否则返回所有过滤后的内容
        return String.join("\n", filteredSegments);
    }

    /**
     * 判断句子是否是思考过程
     */
    private boolean isThinkingProcess(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();

        // 思考过程的典型开头
        String[] thinkingPrefixes = {
            "让我先", "我需要先", "现在我已", "我还需要", "现在我已理解",
            "正在查询", "正在获取", "正在检查", "正在执行",
            "正在将", "正在收集", "正在更新", "正在修改",
            "用户要求", "用户请求"
        };

        for (String prefix : thinkingPrefixes) {
            if (lowerText.startsWith(prefix.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 提取句子中的关键短语用于去重
     */
    private String extractKeyPhrase(String text) {
        if (text == null || text.length() < 10) {
            return null;
        }

        // 提取包含数字或特定关键词的短语
        // 例如："变量a的默认值从30修改为31" → "变量a默认值"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(变量\\w+|节点\\w+|工作流\\d+)[的]?(默认值|配置|详情)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * 判断句子是否像是一个总结
     */
    private boolean isSummaryLike(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 总结的典型开头
        String[] summaryPrefixes = {
            "修改完成", "操作成功", "任务完成", "已完成",
            "成功", "结果", "最终"
        };

        for (String prefix : summaryPrefixes) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }

        // 包含完成标记
        if (text.contains("已更新") || text.contains("已修改") ||
            text.contains("已完成") || text.contains("已成功")) {
            return true;
        }

        return false;
    }

    /**
     * 解析 JSON 并提取 reasoning 和 summary
     * @return 提取后的内容，如果不包含这两个属性则返回原始内容
     */
    private String parseAndExtract(ObjectMapper mapper, String jsonStr, String originalContent) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = mapper.readValue(jsonStr, Map.class);

            // 检查是否包含 reasoning 或 summary 属性
            boolean hasReasoning = jsonMap.containsKey("reasoning");
            boolean hasSummary = jsonMap.containsKey("summary");

            if (!hasReasoning && !hasSummary) {
                return originalContent;
            }

            StringBuilder result = new StringBuilder();
            if (hasReasoning) {
                Object reasoning = jsonMap.get("reasoning");
                if (reasoning != null) {
                    result.append(reasoning.toString());
                }
            }
            if (hasSummary) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                Object summary = jsonMap.get("summary");
                if (summary != null) {
                    result.append(summary.toString());
                }
            }

            return result.length() > 0 ? result.toString() : originalContent;

        } catch (Exception e) {
            log.debug("JSON 解析失败: {}", e.getMessage());
            return originalContent;
        }
    }

    /**
     * 获取或创建会话的去重集合
     */
    private Set<String> getDedupSet(String sessionId) {
        return contentDedupMap.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet());
    }

    /**
     * 清理会话的去重集合
     */
    private void clearDedupSet(String sessionId) {
        contentDedupMap.remove(sessionId);
    }

    /**
     * 计算内容的 MD5 哈希值
     */
    private String computeContentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 算法不可用", e);
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * 检查内容是否为重复内容
     * 使用内容标准化和哈希比对来检测重复
     *
     * @param sessionId 会话ID
     * @param content 待检查的内容
     * @return true 表示是重复内容，应跳过
     */
    private boolean isDuplicateContent(String sessionId, String content) {
        if (content == null || content.length() < MIN_DEDUP_LENGTH) {
            return false;
        }

        Set<String> dedupSet = getDedupSet(sessionId);

        // 标准化内容：去除多余空白、统一换行符
        String normalizedContent = content.trim().replaceAll("\\s+", " ");
        String contentHash = computeContentHash(normalizedContent);

        if (dedupSet.contains(contentHash)) {
            return true;
        }

        dedupSet.add(contentHash);
        return false;
    }

    /**
     * 过滤重复和冗余的内容片段
     * AI 输出中常包含多个层次重复的内容：
     * 1. reasoning 思考过程
     * 2. 重复的用户请求说明
     * 3. status 状态更新
     * 此方法提取最有价值的内容（最后的 summary 部分）
     *
     * @param content 原始内容
     * @return 过滤后的内容
     */
    private String filterDuplicateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String[] lines = content.split("\n");
        StringBuilder filtered = new StringBuilder();
        Set<String> seenSentences = new HashSet<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // 将行拆分为句子
            String[] sentences = trimmedLine.split("(?<=[。！？.!?])\\s*");

            for (String sentence : sentences) {
                if (sentence.trim().isEmpty()) {
                    continue;
                }

                // 标准化句子用于比较
                String normalized = sentence.trim().replaceAll("\\s+", " ");

                // 检查是否与已见过的句子高度相似
                boolean isDuplicate = false;
                for (String seen : seenSentences) {
                    if (calculateSimilarity(normalized, seen) > 0.7) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (!isDuplicate) {
                    if (filtered.length() > 0) {
                        filtered.append(" ");
                    }
                    filtered.append(sentence.trim());
                    seenSentences.add(normalized);
                }
            }
        }

        return filtered.toString();
    }

    /**
     * 计算两个字符串的相似度（0-1之间）
     * 使用词语级别的 Jaccard 相似度 + 关键词重叠检测
     * 可以检测语义相似但不完全相同的句子
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        // 方法1：词语级别的 Jaccard 相似度
        // 将句子分割为词语（支持中文和英文）
        Set<String> words1 = extractWords(s1);
        Set<String> words2 = extractWords(s2);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        // 计算交集
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        // 计算并集
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        double jaccardSimilarity = (double) intersection.size() / union.size();

        // 方法2：连续词组匹配（检测"我需要先获取工作流"这类相似片段）
        double phraseSimilarity = calculatePhraseSimilarity(s1, s2);

        // 取两种方法的最大值
        return Math.max(jaccardSimilarity, phraseSimilarity);
    }

    /**
     * 从字符串中提取词语（支持中文分词和英文单词）
     */
    private Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();

        // 提取中文词语（按字符分割，每个中文字符作为独立词）
        // 对于简单场景，我们可以按2-3个字符的滑动窗口来提取词组
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // 中文字符
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                words.add(Character.toString(c));
                // 2字词组
                if (i + 1 < text.length() && Character.toString(text.charAt(i + 1)).matches("[\\u4e00-\\u9fa5]")) {
                    words.add(text.substring(i, i + 2));
                }
            }
            // 英文单词
            else if (Character.isLetter(c)) {
                int end = i;
                while (end < text.length() && Character.isLetter(text.charAt(end))) {
                    end++;
                }
                if (end > i) {
                    words.add(text.substring(i, end).toLowerCase());
                    i = end - 1;
                }
            }
        }

        return words;
    }

    /**
     * 计算连续词组相似度
     * 检测是否有共同的连续词组
     */
    private double calculatePhraseSimilarity(String s1, String s2) {
        // 提取3-5个字符的连续词组
        Set<String> phrases1 = extractPhrases(s1, 3);
        Set<String> phrases2 = extractPhrases(s2, 3);

        if (phrases1.isEmpty() || phrases2.isEmpty()) {
            return 0.0;
        }

        // 计算交集
        Set<String> intersection = new HashSet<>(phrases1);
        intersection.retainAll(phrases2);

        // 如果有较多共同的连续词组，说明相似
        int maxPhrases = Math.max(phrases1.size(), phrases2.size());
        return (double) intersection.size() / maxPhrases;
    }

    /**
     * 提取指定长度的连续词组
     */
    private Set<String> extractPhrases(String text, int phraseLength) {
        Set<String> phrases = new HashSet<>();
        for (int i = 0; i <= text.length() - phraseLength; i++) {
            String phrase = text.substring(i, i + phraseLength);
            // 只保留包含中文或字母的词组
            if (phrase.matches(".*[\\u4e00-\\u9fa5a-zA-Z].*")) {
                phrases.add(phrase);
            }
        }
        return phrases;
    }
}
