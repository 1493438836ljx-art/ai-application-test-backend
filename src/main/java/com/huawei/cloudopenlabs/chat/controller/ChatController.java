package com.huawei.cloudopenlabs.chat.controller;

import com.huawei.cloudopenlabs.chat.dto.*;
import com.huawei.cloudopenlabs.chat.dto.*;
import com.huawei.cloudopenlabs.chat.entity.ConversationStatus;
import com.huawei.cloudopenlabs.chat.service.ChatService;
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
}
