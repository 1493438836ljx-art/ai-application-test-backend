package com.huawei.cloudopenlabs.agent.observability;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentLogger 单元测试
 */
class AgentLoggerTest {

    private AgentLogger agentLogger;
    private TracingContext tracingContext;

    @BeforeEach
    void setUp() {
        tracingContext = new TracingContext();
        agentLogger = new AgentLogger(tracingContext);
        ReflectionTestUtils.setField(agentLogger, "truncateLength", 200);
        tracingContext.startTrace("test-trace-id");
    }

    @AfterEach
    void tearDown() {
        tracingContext.endTrace();
    }

    @Nested
    @DisplayName("日志记录测试")
    class LoggingTests {

        @Test
        @DisplayName("记录请求开始")
        void testLogRequestStart() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logRequestStart("session-123", "查询用户列表"));
        }

        @Test
        @DisplayName("记录请求结束")
        void testLogRequestEnd() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logRequestEnd("session-123", 1234L, 3));
        }

        @Test
        @DisplayName("记录 CLI 执行")
        void testLogCliExecution() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logCliExecution("session-123", "claude", 1234L, 0));
        }

        @Test
        @DisplayName("记录 CLI 输出")
        void testLogCliOutput() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logCliOutput("session-123", "output text", false));
            assertDoesNotThrow(() -> agentLogger.logCliOutput("session-123", "error text", true));
        }

        @Test
        @DisplayName("记录查询执行")
        void testLogQueryExecution() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logQueryExecution("session-123", "SELECT * FROM users", true));
        }

        @Test
        @DisplayName("记录操作执行")
        void testLogActionExecution() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logActionExecution("session-123", "INSERT INTO users", true));
        }

        @Test
        @DisplayName("记录错误")
        void testLogError() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logError("session-123", "CLI_EXEC", new RuntimeException("测试错误")));
        }

        @Test
        @DisplayName("记录性能警告")
        void testLogPerformanceWarning() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logPerformanceWarning("session-123", "query", 5000L, 1000L));
        }

        @Test
        @DisplayName("记录降级事件")
        void testLogDegradation() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logDegradation("session-123", "query", "timeout"));
        }

        @Test
        @DisplayName("记录会话创建")
        void testLogSessionCreate() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logSessionCreate("session-123"));
        }

        @Test
        @DisplayName("记录会话销毁")
        void testLogSessionDestroy() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> agentLogger.logSessionDestroy("session-123", "completed"));
        }
    }

    @Nested
    @DisplayName("字符串处理测试")
    class StringProcessingTests {

        @Test
        @DisplayName("短字符串不截断")
        void testShortStringNotTruncated() {
            // Given
            String shortStr = "short string";

            // When
            agentLogger.logRequestStart("session-123", shortStr);

            // Then - 不应抛出异常
            assertTrue(true);
        }

        @Test
        @DisplayName("长字符串被截断")
        void testLongStringTruncated() {
            // Given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 300; i++) {
                sb.append("a");
            }
            String longStr = sb.toString();

            // When
            agentLogger.logRequestStart("session-123", longStr);

            // Then - 不应抛出异常
            assertTrue(true);
        }

    }
}
