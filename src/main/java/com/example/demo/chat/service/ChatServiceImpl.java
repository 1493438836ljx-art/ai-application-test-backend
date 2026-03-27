package com.example.demo.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.agent.dto.AgentResponse;
import com.example.demo.agent.framework.AgentExecutor;
import com.example.demo.chat.dto.*;
import com.example.demo.chat.entity.*;
import com.example.demo.chat.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * AI聊天服务实现类 (MyBatis-Plus版本)
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final ChatQuickQuestionMapper quickQuestionMapper;

    private AgentExecutor agentExecutor;

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
        Long workflowId = extractWorkflowId(request.getContext());
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
                    public void onChunk(com.example.demo.agent.dto.StreamChunk chunk) {
                        try {
                            String content = chunk.getContentOrMessage();
                            log.info("onChunk 回调被调用，内容类型: {}, 内容长度: {}",
                                    chunk.getContentType(), content != null ? content.length() : 0);

                            // 仅处理 contentType 为 text 的内容，过滤其他类型（thinking, tool_use, result 等）
                            if (!"text".equals(chunk.getContentType())) {
                                log.debug("过滤非 text 类型内容: contentType={}", chunk.getContentType());
                                return;
                            }

                            // 处理文本内容：如果包含 JSON 且有 reasoning 和 summary 属性，则提取这两个值
                            String processedContent = extractReasoningAndSummary(content);

                            // 构建发送给前端的数据
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("type", "chunk");
                            chunkData.put("content", processedContent);
                            chunkData.put("contentType", chunk.getContentType());

                            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));
                            log.info("SSE chunk 已发送到前端，contentType={}", chunk.getContentType());

                            // 累积文本内容
                            fullContent.append(processedContent);
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
                }
        );

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
        });

        emitter.onCompletion(() -> {
            log.debug("SSE 连接完成");
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
                Long workflowId = extractWorkflowId(request.getContext());

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
    private void processMultiRoundMessageWithNewSession(String userMessage, Long workflowId,
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
    private void processMultiRoundMessage(String userMessage, Long workflowId,
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
    private Long extractWorkflowId(Object context) {
        if (context == null) {
            return null;
        }
        try {
            if (context instanceof Map) {
                Object workflowId = ((Map<?, ?>) context).get("workflowId");
                if (workflowId != null) {
                    return Long.valueOf(workflowId.toString());
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

        return content;
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
}
