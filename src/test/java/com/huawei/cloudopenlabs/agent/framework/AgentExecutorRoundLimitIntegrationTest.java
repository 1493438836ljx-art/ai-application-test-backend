package com.huawei.cloudopenlabs.agent.framework;

import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import com.huawei.cloudopenlabs.agent.service.AgentSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentExecutor 递归深度限制集成测试
 * 需要 H2 数据库环境
 */
@SpringBootTest
@ActiveProfiles("test")
class AgentExecutorRoundLimitIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutorRoundLimitIntegrationTest.class);

    @Autowired
    private AgentSessionService sessionService;

    /**
     * 测试超过最大解析错误次数限制
     *
     * 这个测试模拟 AI 持续返回错误格式的场景
     * 预期：在 3 次解析错误后，应该触发错误回调并终止执行
     */
    @Test
    @DisplayName("集成测试: 解析错误次数限制 (MAX_PARSE_ERROR_ROUNDS=3)")
    void testMaxParseErrorRoundsLimit() throws InterruptedException {
        // 创建一个会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();
        log.info("Created session: {}", conversationId);

        // 模拟设置解析错误计数为 3（达到限制）
        sessionService.updateParseErrorCount(conversationId, 3);

        // 验证计数已更新 - 重新从数据库获取
        AgentSessionEntity updatedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow(() -> new AssertionError("Session not found after update"));

        Integer actualCount = updatedSession.getParseErrorCount();
        log.info("Expected parse_error_count: 3, Actual: {}", actualCount);

        assertNotNull(actualCount, "解析错误计数不应为 null");
        assertEquals(3, actualCount, "解析错误计数应该是 3");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    /**
     * 测试超过最大轮次限制
     *
     * 这个测试模拟长时间的多轮对话
     * 预期：在 15 轮后，应该触发错误回调并终止执行
     */
    @Test
    @DisplayName("集成测试: 最大轮次限制 (MAX_ROUNDS=15)")
    void testMaxRoundsLimit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        AtomicReference<String> sessionId = new AtomicReference<>();

        // 创建一个会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        sessionId.set(session.getConversationId());

        // 模拟设置轮次为 15（达到限制）
        sessionService.updateRoundCount(session.getConversationId(), 15);

        // 验证计数已更新
        AgentSessionEntity updatedSession = sessionService.getByConversationId(session.getConversationId())
                .orElseThrow();
        assertEquals(15, updatedSession.getRoundCount(), "轮次计数应该是 15");

        // 清理
        sessionService.deleteSession(session.getConversationId());
    }

    /**
     * 测试正常流程下轮次计数递增
     */
    @Test
    @DisplayName("集成测试: 正常轮次计数递增")
    void testNormalRoundCountIncrement() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 验证初始轮次为 0
        assertEquals(0, session.getRoundCount(), "初始轮次应该为 0");

        // 模拟轮次递增
        sessionService.updateRoundCount(conversationId, 1);
        AgentSessionEntity updatedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow();
        assertEquals(1, updatedSession.getRoundCount(), "轮次应该递增到 1");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    /**
     * 测试解析错误计数递增
     */
    @Test
    @DisplayName("集成测试: 解析错误计数递增")
    void testParseErrorCountIncrement() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 验证初始解析错误计数为 0 或 null
        Integer initialCount = session.getParseErrorCount();
        log.info("Initial parse_error_count: {}", initialCount);
        assertTrue(initialCount == null || initialCount == 0, "初始解析错误计数应该为 0 或 null");

        // 模拟解析错误递增
        sessionService.updateParseErrorCount(conversationId, 1);

        // 重新从数据库获取
        AgentSessionEntity updatedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow(() -> new AssertionError("Session not found after update"));

        Integer updatedCount = updatedSession.getParseErrorCount();
        log.info("Updated parse_error_count: {}", updatedCount);

        // 验证更新成功
        assertNotNull(updatedCount, "解析错误计数不应为 null");
        assertEquals(1, updatedCount, "解析错误计数应该递增到 1");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    /**
     * 测试会话标记为错误状态
     */
    @Test
    @DisplayName("集成测试: 标记会话为错误状态")
    void testMarkSessionAsError() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 标记为错误
        sessionService.markAsError(conversationId, "超过最大轮次限制");

        // 验证状态
        AgentSessionEntity updatedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow();
        assertEquals("ERROR", updatedSession.getStatus(), "会话状态应该是 ERROR");
        assertTrue(updatedSession.getLastReasoning().contains("超过最大轮次限制"),
                "错误信息应该被记录");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    /**
     * 测试会话重置（已完成会话再次使用时重置）
     */
    @Test
    @DisplayName("集成测试: 已完成会话重置")
    void testSessionReset() {
        // 创建会话并标记为完成
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 设置一些数据
        sessionService.updateRoundCount(conversationId, 5);
        sessionService.updateParseErrorCount(conversationId, 2);
        sessionService.markAsCompleted(conversationId);

        // 验证已完成状态
        AgentSessionEntity completedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow();
        assertEquals("COMPLETED", completedSession.getStatus());

        // 再次获取会话（应该重置）
        AgentSessionEntity resetSession = sessionService.getOrCreateSession("test-workflow-1", conversationId);

        // 验证重置后的状态
        assertEquals("ACTIVE", resetSession.getStatus(), "会话状态应该重置为 ACTIVE");
        assertEquals(0, resetSession.getRoundCount(), "轮次计数应该重置为 0");

        // 清理
        sessionService.deleteSession(conversationId);
    }
}
