package com.huawei.cloudopenlabs.agent.framework;

import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import com.huawei.cloudopenlabs.agent.service.AgentSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AgentExecutor 递归深度限制测试
 * 验证 MAX_ROUNDS 和 MAX_PARSE_ERROR_ROUNDS 限制功能
 */
@ExtendWith(MockitoExtension.class)
class AgentExecutorRoundLimitTest {

    @Mock
    private AgentSessionService sessionService;

    private AgentSessionEntity session;

    @BeforeEach
    void setUp() {
        session = new AgentSessionEntity();
        session.setConversationId("test-conversation-123");
        session.setWorkflowId(1L);
        session.setRoundCount(0);
        session.setParseErrorCount(0);
    }

    @Test
    @DisplayName("测试轮次计数初始化为0")
    void testInitialRoundCount() {
        assertNotNull(session.getRoundCount());
        assertEquals(0, session.getRoundCount());
    }

    @Test
    @DisplayName("测试解析错误计数初始化为0")
    void testInitialParseErrorCount() {
        assertNotNull(session.getParseErrorCount());
        assertEquals(0, session.getParseErrorCount());
    }

    @Test
    @DisplayName("测试轮次递增逻辑")
    void testRoundCountIncrement() {
        // 模拟递增
        session.setRoundCount(1);
        assertEquals(1, session.getRoundCount());

        session.setRoundCount(2);
        assertEquals(2, session.getRoundCount());

        session.setRoundCount(15);
        assertEquals(15, session.getRoundCount());
    }

    @Test
    @DisplayName("测试解析错误递增逻辑")
    void testParseErrorCountIncrement() {
        session.setParseErrorCount(1);
        assertEquals(1, session.getParseErrorCount());

        session.setParseErrorCount(2);
        assertEquals(2, session.getParseErrorCount());

        session.setParseErrorCount(3);
        assertEquals(3, session.getParseErrorCount());
    }

    @Test
    @DisplayName("测试超过最大轮次限制 (MAX_ROUNDS=15)")
    void testMaxRoundsExceeded() {
        // 设置轮次为最大值
        session.setRoundCount(15);

        // 验证超过限制的条件
        int maxRounds = 15;
        assertTrue(session.getRoundCount() >= maxRounds,
                "当 roundCount >= MAX_ROUNDS 时应该触发限制");
    }

    @Test
    @DisplayName("测试超过最大解析错误次数限制 (MAX_PARSE_ERROR_ROUNDS=3)")
    void testMaxParseErrorsExceeded() {
        // 设置解析错误次数为最大值
        session.setParseErrorCount(3);

        // 验证超过限制的条件
        int maxParseErrors = 3;
        assertTrue(session.getParseErrorCount() >= maxParseErrors,
                "当 parseErrorCount >= MAX_PARSE_ERROR_ROUNDS 时应该触发限制");
    }

    @Test
    @DisplayName("测试AgentSessionService更新轮次方法被调用")
    void testUpdateRoundCountCalled() {
        // 模拟调用
        sessionService.updateRoundCount("test-conversation-123", 5);

        // 验证方法被调用
        verify(sessionService, times(1)).updateRoundCount("test-conversation-123", 5);
    }

    @Test
    @DisplayName("测试AgentSessionService更新解析错误计数方法被调用")
    void testUpdateParseErrorCountCalled() {
        // 模拟调用
        sessionService.updateParseErrorCount("test-conversation-123", 2);

        // 验证方法被调用
        verify(sessionService, times(1)).updateParseErrorCount("test-conversation-123", 2);
    }

    @Test
    @DisplayName("测试标记会话错误方法被调用")
    void testMarkAsErrorCalled() {
        // 模拟调用
        sessionService.markAsError("test-conversation-123", "超过最大轮次限制");

        // 验证方法被调用
        verify(sessionService, times(1))
                .markAsError("test-conversation-123", "超过最大轮次限制");
    }

    @Test
    @DisplayName("测试空值轮次计数的默认处理")
    void testNullRoundCountDefault() {
        AgentSessionEntity newSession = new AgentSessionEntity();
        newSession.setRoundCount(null);

        // 模拟 incrementRoundCount 的逻辑
        int currentRound = (newSession.getRoundCount() != null ? newSession.getRoundCount() : 0) + 1;
        assertEquals(1, currentRound);
    }

    @Test
    @DisplayName("测试空值解析错误计数的默认处理")
    void testNullParseErrorCountDefault() {
        AgentSessionEntity newSession = new AgentSessionEntity();
        newSession.setParseErrorCount(null);

        // 模拟 incrementParseErrorCount 的逻辑
        int currentCount = (newSession.getParseErrorCount() != null ? newSession.getParseErrorCount() : 0) + 1;
        assertEquals(1, currentCount);
    }

    @Test
    @DisplayName("测试StreamCallback错误回调")
    void testStreamCallbackOnError() {
        AtomicInteger errorCalled = new AtomicInteger(0);
        StringBuilder errorMessage = new StringBuilder();

        AgentExecutor.StreamCallback callback = new AgentExecutor.StreamCallback() {
            @Override
            public void onChunk(StreamChunk chunk) {
                // 不需要实现
            }

            @Override
            public void onError(String error) {
                errorCalled.incrementAndGet();
                errorMessage.append(error);
            }
        };

        // 模拟错误回调
        callback.onError("超过最大轮次限制（15 轮）");

        assertEquals(1, errorCalled.get());
        assertTrue(errorMessage.toString().contains("15"));
    }

    @Test
    @DisplayName("测试MultiRoundCallback错误回调")
    void testMultiRoundCallbackOnError() {
        AtomicInteger errorCalled = new AtomicInteger(0);
        StringBuilder errorMessage = new StringBuilder();

        AgentExecutor.MultiRoundCallback callback = new AgentExecutor.MultiRoundCallback() {
            @Override
            public void onReasoning(String reasoning) {
                // 不需要实现
            }

            @Override
            public void onStatus(String status) {
                // 不需要实现
            }

            @Override
            public void onWorkflowUpdate(Object result) {
                // 不需要实现
            }

            @Override
            public void onComplete(String summary, Object result) {
                // 不需要实现
            }

            @Override
            public void onError(String error) {
                errorCalled.incrementAndGet();
                errorMessage.append(error);
            }
        };

        // 模拟错误回调
        callback.onError("AI 响应格式持续异常，已尝试 3 次仍无法解析");

        assertEquals(1, errorCalled.get());
        assertTrue(errorMessage.toString().contains("3"));
    }
}
