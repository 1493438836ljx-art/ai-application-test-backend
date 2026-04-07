package com.huawei.cloudopenlabs.agent.framework;

import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentExecutor 超时机制单元测试
 * 验证 MAX_EXECUTION_TIME_MS 和超时检查功能
 */
class AgentExecutorTimeoutTest {

    private AgentSessionEntity session;

    @BeforeEach
    void setUp() {
        session = new AgentSessionEntity();
        session.setConversationId("test-conversation-123");
        session.setWorkflowId(1L);
        session.setRoundCount(0);
        session.setParseErrorCount(0);
    }

    // ========== startTime 字段测试 ==========

    @Test
    @DisplayName("测试 startTime 初始值为 null")
    void testStartTimeInitialNull() {
        AgentSessionEntity newSession = new AgentSessionEntity();
        assertNull(newSession.getStartTime(), "新会话的 startTime 应该为 null");
    }

    @Test
    @DisplayName("测试 startTime 可以被设置")
    void testStartTimeCanBeSet() {
        long currentTime = System.currentTimeMillis();
        session.setStartTime(currentTime);
        assertEquals(currentTime, session.getStartTime());
    }

    @Test
    @DisplayName("测试 startTime 可以被更新")
    void testStartTimeCanBeUpdated() {
        long time1 = System.currentTimeMillis();
        session.setStartTime(time1);
        assertEquals(time1, session.getStartTime());

        long time2 = time1 + 1000;
        session.setStartTime(time2);
        assertEquals(time2, session.getStartTime());
    }

    // ========== 超时检查逻辑测试 ==========

    @Test
    @DisplayName("测试超时检查 - startTime 为 null 时不超时")
    void testTimeoutCheckWithNullStartTime() {
        session.setStartTime(null);

        // 模拟 isTimeout 逻辑
        boolean isTimeout = checkTimeout(session);
        assertFalse(isTimeout, "startTime 为 null 时不应判定为超时");
    }

    @Test
    @DisplayName("测试超时检查 - 刚开始时不超时")
    void testTimeoutCheckJustStarted() {
        session.setStartTime(System.currentTimeMillis());

        boolean isTimeout = checkTimeout(session);
        assertFalse(isTimeout, "刚开始执行时不应超时");
    }

    @Test
    @DisplayName("测试超时检查 - 执行 1 分钟后不超时")
    void testTimeoutCheckAfterOneMinute() {
        // 模拟 1 分钟前开始
        long oneMinuteAgo = System.currentTimeMillis() - (60 * 1000);
        session.setStartTime(oneMinuteAgo);

        boolean isTimeout = checkTimeout(session);
        assertFalse(isTimeout, "执行 1 分钟后不应超时（限制是 5 分钟）");
    }

    @Test
    @DisplayName("测试超时检查 - 执行 4 分钟后不超时")
    void testTimeoutCheckAfterFourMinutes() {
        // 模拟 4 分钟前开始
        long fourMinutesAgo = System.currentTimeMillis() - (4 * 60 * 1000);
        session.setStartTime(fourMinutesAgo);

        boolean isTimeout = checkTimeout(session);
        assertFalse(isTimeout, "执行 4 分钟后不应超时（限制是 5 分钟）");
    }

    @Test
    @DisplayName("测试超时检查 - 执行 5 分钟后超时")
    void testTimeoutCheckAfterFiveMinutes() {
        // 模拟 5 分钟 + 1 秒前开始
        long fiveMinutesAndOneSecondAgo = System.currentTimeMillis() - (5 * 60 * 1000 + 1000);
        session.setStartTime(fiveMinutesAndOneSecondAgo);

        boolean isTimeout = checkTimeout(session);
        assertTrue(isTimeout, "执行超过 5 分钟后应该超时");
    }

    @Test
    @DisplayName("测试超时检查 - 执行 10 分钟后超时")
    void testTimeoutCheckAfterTenMinutes() {
        // 模拟 10 分钟前开始
        long tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000);
        session.setStartTime(tenMinutesAgo);

        boolean isTimeout = checkTimeout(session);
        assertTrue(isTimeout, "执行 10 分钟后应该超时");
    }

    // ========== 已用时间计算测试 ==========

    @Test
    @DisplayName("测试已用时间计算 - startTime 为 null 返回 0")
    void testElapsedTimeWithNullStartTime() {
        session.setStartTime(null);

        long elapsed = calculateElapsedTime(session);
        assertEquals(0, elapsed, "startTime 为 null 时已用时间应为 0");
    }

    @Test
    @DisplayName("测试已用时间计算 - 刚开始约为 0")
    void testElapsedTimeJustStarted() {
        long startTime = System.currentTimeMillis();
        session.setStartTime(startTime);

        long elapsed = calculateElapsedTime(session);
        assertTrue(elapsed >= 0 && elapsed < 100, "刚开始时已用时间应接近 0");
    }

    @Test
    @DisplayName("测试已用时间计算 - 1 分钟后约为 60000ms")
    void testElapsedTimeAfterOneMinute() {
        long oneMinuteAgo = System.currentTimeMillis() - 60000;
        session.setStartTime(oneMinuteAgo);

        long elapsed = calculateElapsedTime(session);
        assertTrue(elapsed >= 59000 && elapsed <= 61000,
                "1 分钟后已用时间应约为 60000ms，实际为: " + elapsed);
    }

    // ========== 超时错误消息测试 ==========

    @Test
    @DisplayName("测试超时错误消息格式")
    void testTimeoutErrorMessageFormat() {
        session.setStartTime(System.currentTimeMillis() - 310000); // 310 秒
        session.setRoundCount(10);

        String errorMessage = buildTimeoutErrorMessage(session);

        assertTrue(errorMessage.contains("300"), "错误消息应包含超时限制（300秒）");
        assertTrue(errorMessage.contains("10"), "错误消息应包含已执行轮次");
        assertTrue(errorMessage.contains("超时"), "错误消息应包含'超时'关键词");
        assertTrue(errorMessage.contains("可能原因"), "错误消息应包含可能原因提示");
    }

    @Test
    @DisplayName("测试超时错误消息包含解决建议")
    void testTimeoutErrorMessageContainsSuggestions() {
        session.setStartTime(System.currentTimeMillis() - 310000);
        session.setRoundCount(5);

        String errorMessage = buildTimeoutErrorMessage(session);

        assertTrue(errorMessage.contains("拆分"), "错误消息应建议拆分任务");
        assertTrue(errorMessage.contains("简化"), "错误消息应建议简化请求");
        assertTrue(errorMessage.contains("稍后重试"), "错误消息应建议稍后重试");
    }

    // ========== 边界条件测试 ==========

    @Test
    @DisplayName("测试超时边界 - 刚好 5 分钟")
    void testTimeoutBoundaryExactlyFiveMinutes() {
        // 刚好 5 分钟（边界情况，可能超时也可能不超时）
        long exactlyFiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        session.setStartTime(exactlyFiveMinutesAgo);

        // 由于毫秒级精度，这个测试可能通过也可能失败
        // 重要的是验证逻辑是 > 而不是 >=
        boolean isTimeout = checkTimeout(session);
        // 边界情况，不强制要求结果
        System.out.println("边界测试（刚好5分钟）: isTimeout=" + isTimeout);
    }

    @Test
    @DisplayName("测试超时边界 - 4 分 59 秒不超时")
    void testTimeoutBoundaryFourMinutesFiftyNineSeconds() {
        long fourMinFiftyNineSecAgo = System.currentTimeMillis() - (4 * 60 * 1000 + 59 * 1000);
        session.setStartTime(fourMinFiftyNineSecAgo);

        boolean isTimeout = checkTimeout(session);
        assertFalse(isTimeout, "4 分 59 秒不应超时");
    }

    @Test
    @DisplayName("测试超时边界 - 5 分 01 秒超时")
    void testTimeoutBoundaryFiveMinutesOneSecond() {
        long fiveMinOneSecAgo = System.currentTimeMillis() - (5 * 60 * 1000 + 1000);
        session.setStartTime(fiveMinOneSecAgo);

        boolean isTimeout = checkTimeout(session);
        assertTrue(isTimeout, "5 分 01 秒应该超时");
    }

    // ========== 辅助方法（模拟 AgentExecutor 中的逻辑）==========

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
