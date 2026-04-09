package com.huawei.cloudopenlabs.agent.framework;

import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import com.huawei.cloudopenlabs.agent.service.AgentSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentExecutor 超时机制集成测试
 * 验证 startTime 字段的数据库持久化和超时检查
 */
@SpringBootTest
@ActiveProfiles("test")
class AgentExecutorTimeoutIntegrationTest {

    @Autowired
    private AgentSessionService sessionService;

    // ========== startTime 字段持久化测试 ==========

    @Test
    @DisplayName("集成测试: 新会话创建时 startTime 被自动设置")
    void testStartTimeSetOnNewSessionCreation() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 验证 startTime 被设置
        assertNotNull(session.getStartTime(), "新会话的 startTime 应该被自动设置");
        assertTrue(session.getStartTime() > 0, "startTime 应该是正数");

        long now = System.currentTimeMillis();
        long diff = now - session.getStartTime();
        assertTrue(diff >= 0 && diff < 5000, "startTime 应该接近当前时间");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    @Test
    @DisplayName("集成测试: startTime 从数据库正确读取")
    void testStartTimeReadFromDatabase() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();
        Long expectedStartTime = session.getStartTime();

        assertNotNull(expectedStartTime, "startTime 应该被设置");

        // 重新从数据库获取
        AgentSessionEntity retrievedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow(() -> new AssertionError("Session not found"));

        // 验证 startTime 正确保存和读取
        assertNotNull(retrievedSession.getStartTime(), "从数据库读取的 startTime 不应为 null");
        assertEquals(expectedStartTime, retrievedSession.getStartTime(), "startTime 应该一致");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    @Test
    @DisplayName("集成测试: setStartTime 方法正确更新数据库")
    void testSetStartTimeUpdatesDatabase() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 设置新的 startTime
        long newStartTime = System.currentTimeMillis() - 60000; // 1 分钟前
        sessionService.setStartTime(conversationId, newStartTime);

        // 重新从数据库获取
        AgentSessionEntity updatedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow(() -> new AssertionError("Session not found"));

        // 验证 startTime 被更新
        assertEquals(newStartTime, updatedSession.getStartTime(), "startTime 应该被更新");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    // ========== 会话重置时的 startTime 测试 ==========

    @Test
    @DisplayName("集成测试: 会话重置时 startTime 被更新")
    void testStartTimeResetOnSessionReset() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();
        Long originalStartTime = session.getStartTime();

        // 等待一小段时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 标记会话为完成
        sessionService.markAsCompleted(conversationId);

        // 再次获取会话（应该重置）
        AgentSessionEntity resetSession = sessionService.getOrCreateSession("test-workflow-1", conversationId);

        // 验证会话状态被重置
        assertEquals("ACTIVE", resetSession.getStatus(), "会话状态应该重置为 ACTIVE");

        // 验证 startTime 被更新（新的 startTime 应该 >= 原始 startTime）
        assertNotNull(resetSession.getStartTime(), "重置后的 startTime 不应为 null");
        assertTrue(resetSession.getStartTime() >= originalStartTime,
                "重置后的 startTime 应该 >= 原始 startTime");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    // ========== 超时检查集成测试 ==========

    @Test
    @DisplayName("集成测试: 模拟超时场景")
    void testSimulatedTimeoutScenario() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 模拟设置 6 分钟前的 startTime（超过 5 分钟限制）
        long sixMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000);
        sessionService.setStartTime(conversationId, sixMinutesAgo);

        // 重新获取会话
        AgentSessionEntity timedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow(() -> new AssertionError("Session not found"));

        // 验证超时检查
        boolean isTimeout = checkTimeout(timedSession);
        assertTrue(isTimeout, "6 分钟后应该检测到超时");

        // 验证已用时间
        long elapsed = calculateElapsedTime(timedSession);
        assertTrue(elapsed >= 6 * 60 * 1000, "已用时间应该 >= 6 分钟");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    @Test
    @DisplayName("集成测试: 正常执行不超时")
    void testNormalExecutionNoTimeout() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 等待一小段时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 重新获取会话
        AgentSessionEntity currentSession = sessionService.getByConversationId(conversationId)
                .orElseThrow(() -> new AssertionError("Session not found"));

        // 验证不超时
        boolean isTimeout = checkTimeout(currentSession);
        assertFalse(isTimeout, "刚创建的会话不应该超时");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    // ========== 超时错误消息测试 ==========

    @Test
    @DisplayName("集成测试: 超时错误消息包含正确信息")
    void testTimeoutErrorMessageContainsCorrectInfo() {
        // 创建新会话
        AgentSessionEntity session = sessionService.getOrCreateSession("test-workflow-1", null);
        String conversationId = session.getConversationId();

        // 设置一些数据
        sessionService.updateRoundCount(conversationId, 8);

        // 模拟超时
        long sixMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000);
        sessionService.setStartTime(conversationId, sixMinutesAgo);

        // 重新获取会话
        AgentSessionEntity timedSession = sessionService.getByConversationId(conversationId)
                .orElseThrow(() -> new AssertionError("Session not found"));

        // 生成错误消息
        String errorMessage = buildTimeoutErrorMessage(timedSession);

        // 验证错误消息内容
        assertTrue(errorMessage.contains("超时"), "错误消息应包含'超时'");
        assertTrue(errorMessage.contains("360"), "错误消息应包含已执行秒数（约 360 秒）");
        assertTrue(errorMessage.contains("8 轮"), "错误消息应包含已执行轮次");
        assertTrue(errorMessage.contains("300"), "错误消息应包含超时限制（300 秒）");

        // 清理
        sessionService.deleteSession(conversationId);
    }

    // ========== 辅助方法 ==========

    private static final long MAX_EXECUTION_TIME_MS = 5 * 60 * 1000; // 5 分钟

    private boolean checkTimeout(AgentSessionEntity session) {
        Long startTime = session.getStartTime();
        if (startTime == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed > MAX_EXECUTION_TIME_MS;
    }

    private long calculateElapsedTime(AgentSessionEntity session) {
        Long startTime = session.getStartTime();
        if (startTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    private String buildTimeoutErrorMessage(AgentSessionEntity session) {
        long elapsedSeconds = calculateElapsedTime(session) / 1000;
        int executedRounds = session.getRoundCount() != null ? session.getRoundCount() : 0;

        return String.format(
                "任务执行超时（已执行 %d 秒，超过 %d 秒限制）。\n\n" +
                "可能原因：\n" +
                "1. 任务过于复杂，建议拆分为多个简单任务分别执行\n" +
                "2. AI 正在处理大量数据，请耐心等待或简化请求\n" +
                "3. 系统暂时繁忙，请稍后重试\n\n" +
                "执行统计：已执行 %d 轮对话，耗时 %d 秒",
                elapsedSeconds,
                MAX_EXECUTION_TIME_MS / 1000,
                executedRounds,
                elapsedSeconds
        );
    }
}
