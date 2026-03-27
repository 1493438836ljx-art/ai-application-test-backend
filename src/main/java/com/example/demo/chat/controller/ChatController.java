package com.example.demo.chat.controller;

import com.example.demo.chat.dto.*;
import com.example.demo.chat.entity.ConversationStatus;
import com.example.demo.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI聊天", description = "AI智能助手相关接口")
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息
     *
     * @param request 发送请求
     * @return 发送响应
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送用户消息并获取AI回复")
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
    @Operation(summary = "流式发送消息", description = "发送用户消息并流式获取AI回复")
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
    @Operation(summary = "创建对话", description = "创建一个新的对话")
    public ResponseEntity<ConversationDTO> createConversation(
            @Parameter(description = "用户ID")
            @RequestParam(required = false) String userId,
            @Parameter(description = "对话标题")
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
    @Operation(summary = "获取对话列表", description = "分页获取对话列表")
    public ResponseEntity<Page<ConversationDTO>> getConversations(
            @Parameter(description = "用户ID")
            @RequestParam(required = false) String userId,
            @Parameter(description = "状态")
            @RequestParam(required = false) ConversationStatus status,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
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
    @Operation(summary = "获取对话详情", description = "根据UUID获取对话详情，包含所有消息")
    public ResponseEntity<ConversationDTO> getConversation(
            @Parameter(description = "对话UUID", required = true)
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
    @Operation(summary = "更新对话标题", description = "更新对话的标题")
    public ResponseEntity<ConversationDTO> updateConversationTitle(
            @Parameter(description = "对话UUID", required = true)
            @PathVariable String uuid,
            @Parameter(description = "新标题", required = true)
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
    @Operation(summary = "归档对话", description = "将对话状态设置为已归档")
    public ResponseEntity<Void> archiveConversation(
            @Parameter(description = "对话UUID", required = true)
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
    @Operation(summary = "删除对话", description = "删除指定对话（逻辑删除）")
    public ResponseEntity<Void> deleteConversation(
            @Parameter(description = "对话UUID", required = true)
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
    @Operation(summary = "获取快捷问题", description = "获取所有启用的快捷问题")
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
    @Operation(summary = "提交反馈", description = "对AI回复提交反馈评价")
    public ResponseEntity<Void> submitFeedback(
            @Parameter(description = "消息UUID", required = true)
            @PathVariable String messageUuid,
            @Valid @RequestBody FeedbackRequest request) {
        chatService.submitFeedback(messageUuid, request);
        return ResponseEntity.noContent().build();
    }
}
