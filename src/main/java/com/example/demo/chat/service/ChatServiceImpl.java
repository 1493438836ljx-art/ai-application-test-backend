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

                // 获取或创建对话
                if (isNewConversation) {
                    conversation = createConversationEntity(request.getUserId(), generateTitle(request.getMessage()));
                } else {
                    conversation = conversationMapper.selectByConversationUuid(request.getConversationId())
                            .orElseThrow(() -> new RuntimeException("对话不存在: " + request.getConversationId()));
                }

                // 发送对话ID
                Map<String, Object> startData = new HashMap<>();
                startData.put("type", "start");
                startData.put("conversationId", conversation.getConversationUuid());
                emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(startData)));

                // 创建用户消息
                ChatMessageEntity userMessage = new ChatMessageEntity();
                userMessage.setConversationId(conversation.getId());
                userMessage.setMessageUuid(UUID.randomUUID().toString());
                userMessage.setRole("user");
                userMessage.setContent(request.getMessage());
                userMessage.setContentType("text");
                messageMapper.insert(userMessage);

                // 获取AI回复
                long startTime = System.currentTimeMillis();
                String fullContent = generateAIReply(request.getMessage());
                long latencyMs = System.currentTimeMillis() - startTime;

                // 流式发送内容（按字符或词组分块）
                int chunkSize = 5; // 每次发送5个字符
                for (int i = 0; i < fullContent.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, fullContent.length());
                    String chunk = fullContent.substring(i, end);

                    Map<String, Object> chunkData = new HashMap<>();
                    chunkData.put("type", "chunk");
                    chunkData.put("content", chunk);
                    emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(chunkData)));

                    // 添加短暂延迟以模拟打字效果
                    Thread.sleep(30);
                }

                // 保存AI消息到数据库
                ChatMessageEntity assistantMessage = new ChatMessageEntity();
                assistantMessage.setConversationId(conversation.getId());
                assistantMessage.setMessageUuid(UUID.randomUUID().toString());
                assistantMessage.setRole("assistant");
                assistantMessage.setContent(fullContent);
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

                // 发送完成事件
                Map<String, Object> doneData = new HashMap<>();
                doneData.put("type", "done");
                doneData.put("messageUuid", assistantMessage.getMessageUuid());
                doneData.put("latencyMs", latencyMs);
                emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(doneData)));

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
}
