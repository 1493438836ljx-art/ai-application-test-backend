package com.example.demo.chat.service;

import com.example.demo.chat.dto.*;
import com.example.demo.chat.entity.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI聊天服务接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface ChatService {

    /**
     * 发送消息并获取AI回复
     *
     * @param request 发送请求
     * @return 发送响应
     */
    ChatSendResponse sendMessage(ChatSendRequest request);

    /**
     * 流式发送消息并获取AI回复（SSE）
     *
     * @param request 发送请求
     * @return SSE发射器
     */
    SseEmitter streamMessage(ChatSendRequest request);

    /**
     * 创建新对话
     *
     * @param userId 用户ID
     * @param title  对话标题
     * @return 对话DTO
     */
    ConversationDTO createConversation(String userId, String title);

    /**
     * 获取对话详情
     *
     * @param uuid 对话UUID
     * @return 对话DTO
     */
    ConversationDTO getConversation(String uuid);

    /**
     * 获取对话列表
     *
     * @param userId   用户ID
     * @param status   状态
     * @param pageable 分页参数
     * @return 对话分页列表
     */
    Page<ConversationDTO> getConversations(String userId, ConversationStatus status, Pageable pageable);

    /**
     * 更新对话标题
     *
     * @param uuid  对话UUID
     * @param title 新标题
     * @return 对话DTO
     */
    ConversationDTO updateConversationTitle(String uuid, String title);

    /**
     * 归档对话
     *
     * @param uuid 对话UUID
     */
    void archiveConversation(String uuid);

    /**
     * 删除对话
     *
     * @param uuid 对话UUID
     */
    void deleteConversation(String uuid);

    /**
     * 获取快捷问题列表
     *
     * @return 快捷问题列表
     */
    java.util.List<QuickQuestionDTO> getQuickQuestions();

    /**
     * 提交消息反馈
     *
     * @param messageUuid 消息UUID
     * @param request     反馈请求
     */
    void submitFeedback(String messageUuid, com.example.demo.chat.dto.FeedbackRequest request);
}
