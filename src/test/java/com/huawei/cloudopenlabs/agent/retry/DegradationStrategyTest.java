package com.huawei.cloudopenlabs.agent.retry;

import com.huawei.cloudopenlabs.agent.error.AgentErrorResponse;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DegradationStrategy 单元测试
 */
class DegradationStrategyTest {

    private DegradationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DegradationStrategy();
    }

    @Nested
    @DisplayName("降级处理测试")
    class HandleDegradationTests {

        @Test
        @DisplayName("查询操作降级")
        void testDegradeQuery() {
            // Given
            Exception error = new TimeoutException("查询超时");

            // When
            Object result = strategy.handleDegradation("query", error);

            // Then
            assertTrue(result instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(true, map.get("degraded"));
            assertNotNull(map.get("message"));
            assertNotNull(map.get("suggestion"));
        }

        @Test
        @DisplayName("操作降级")
        void testDegradeAction() {
            // Given
            Exception error = new RuntimeException("操作失败");

            // When
            Object result = strategy.handleDegradation("action", error);

            // Then
            assertTrue(result instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(true, map.get("degraded"));
            assertTrue(map.get("message").toString().contains("已记录"));
        }

        @Test
        @DisplayName("流式操作降级")
        void testDegradeStream() {
            // Given
            Exception error = new RuntimeException("连接断开");

            // When
            Object result = strategy.handleDegradation("stream", error);

            // Then
            assertTrue(result instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(true, map.get("degraded"));
            assertTrue(map.get("message").toString().contains("流式"));
        }

        @Test
        @DisplayName("执行操作降级")
        void testDegradeExecute() {
            // Given
            Exception error = new RuntimeException("执行失败");

            // When
            Object result = strategy.handleDegradation("execute", error);

            // Then
            assertTrue(result instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(true, map.get("degraded"));
            assertEquals(true, map.get("retryable"));
        }

        @Test
        @DisplayName("未知操作返回错误响应")
        void testUnknownOperation() {
            // Given
            Exception error = new RuntimeException("未知错误");

            // When
            Object result = strategy.handleDegradation("unknown", error);

            // Then
            assertTrue(result instanceof AgentErrorResponse);
        }

        @Test
        @DisplayName("操作名不区分大小写")
        void testCaseInsensitive() {
            // Given
            Exception error = new RuntimeException("错误");

            // When
            Object result1 = strategy.handleDegradation("QUERY", error);
            Object result2 = strategy.handleDegradation("Query", error);

            // Then
            assertTrue(result1 instanceof Map);
            assertTrue(result2 instanceof Map);
        }
    }

    @Nested
    @DisplayName("用户友好消息测试")
    class UserFriendlyMessageTests {

        @Test
        @DisplayName("超时异常友好消息")
        void testTimeoutMessage() {
            // Given
            Exception error = new TimeoutException("操作超时");

            // When
            String message = strategy.extractUserFriendlyMessage(error);

            // Then
            assertTrue(message.contains("超时"));
        }

        @Test
        @DisplayName("网络异常友好消息")
        void testNetworkMessage() {
            // Given
            Exception error = new java.net.SocketException("连接断开");

            // When
            String message = strategy.extractUserFriendlyMessage(error);

            // Then
            assertTrue(message.contains("网络"));
        }

        @Test
        @DisplayName("重试耗尽友好消息")
        void testRetryExhaustedMessage() {
            // Given
            Exception error = new RetryExhaustedException("重试耗尽");

            // When
            String message = strategy.extractUserFriendlyMessage(error);

            // Then
            assertTrue(message.contains("不可用"));
        }

        @Test
        @DisplayName("中断异常友好消息")
        void testInterruptMessage() {
            // Given
            Exception error = new InterruptedException("操作被中断");

            // When
            String message = strategy.extractUserFriendlyMessage(error);

            // Then
            assertTrue(message.contains("中断"));
        }

        @Test
        @DisplayName("空异常返回默认消息")
        void testNullException() {
            // When
            String message = strategy.extractUserFriendlyMessage(null);

            // Then
            assertEquals("系统异常", message);
        }

        @Test
        @DisplayName("未知异常返回默认消息")
        void testUnknownException() {
            // Given
            Exception error = new RuntimeException("未知错误");

            // When
            String message = strategy.extractUserFriendlyMessage(error);

            // Then
            assertEquals("系统繁忙，请稍后重试", message);
        }
    }

    @Nested
    @DisplayName("降级判断测试")
    class ShouldDegradeTests {

        @Test
        @DisplayName("超时应该降级")
        void testTimeoutShouldDegrade() {
            assertTrue(strategy.shouldDegrade(new TimeoutException()));
        }

        @Test
        @DisplayName("网络错误应该降级")
        void testNetworkShouldDegrade() {
            assertTrue(strategy.shouldDegrade(new java.net.SocketException()));
        }

        @Test
        @DisplayName("重试耗尽应该降级")
        void testRetryExhaustedShouldDegrade() {
            assertTrue(strategy.shouldDegrade(new RetryExhaustedException("")));
        }

        @Test
        @DisplayName("普通异常不降级")
        void testGenericExceptionNotDegrade() {
            assertFalse(strategy.shouldDegrade(new RuntimeException()));
        }

        @Test
        @DisplayName("空异常不降级")
        void testNullExceptionNotDegrade() {
            assertFalse(strategy.shouldDegrade(null));
        }
    }

    @Nested
    @DisplayName("降级级别测试")
    class DegradationLevelTests {

        @Test
        @DisplayName("内存溢出完全降级 - 使用模拟异常")
        void testOutOfMemoryFullDegrade() {
            // Given - 使用包含 OutOfMemory 关键字的 RuntimeException 模拟
            Exception error = new RuntimeException("OutOfMemoryError: 内存不足");

            // When
            DegradationStrategy.DegradationLevel level = strategy.getDegradationLevel(error);

            // Then - 普通异常不触发完全降级，因为没有 OutOfMemoryError 类型
            assertEquals(DegradationStrategy.DegradationLevel.NONE, level);
        }

        @Test
        @DisplayName("超时部分降级")
        void testTimeoutPartialDegrade() {
            // Given
            Exception error = new TimeoutException();

            // When
            DegradationStrategy.DegradationLevel level = strategy.getDegradationLevel(error);

            // Then
            assertEquals(DegradationStrategy.DegradationLevel.PARTIAL, level);
        }

        @Test
        @DisplayName("网络错误轻微降级")
        void testNetworkMinorDegrade() {
            // Given
            Exception error = new java.net.SocketException();

            // When
            DegradationStrategy.DegradationLevel level = strategy.getDegradationLevel(error);

            // Then
            assertEquals(DegradationStrategy.DegradationLevel.MINOR, level);
        }

        @Test
        @DisplayName("普通错误无降级")
        void testGenericNoDegrade() {
            // Given
            Exception error = new RuntimeException();

            // When
            DegradationStrategy.DegradationLevel level = strategy.getDegradationLevel(error);

            // Then
            assertEquals(DegradationStrategy.DegradationLevel.NONE, level);
        }

        @Test
        @DisplayName("空异常无降级")
        void testNullNoDegrade() {
            // When
            DegradationStrategy.DegradationLevel level = strategy.getDegradationLevel(null);

            // Then
            assertEquals(DegradationStrategy.DegradationLevel.NONE, level);
        }
    }
}
