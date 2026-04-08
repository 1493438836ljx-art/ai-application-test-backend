package com.huawei.cloudopenlabs.agent.error;

import org.junit.jupiter.api.*;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentErrorResponse 单元测试
 */
class AgentErrorResponseTest {

    @Nested
    @DisplayName("构造器测试")
    class ConstructorTests {

        @Test
        @DisplayName("基本构造器")
        void testBasicConstructor() {
            // When
            AgentErrorResponse response = new AgentErrorResponse("A1001", "测试消息");

            // Then
            assertEquals("A1001", response.getCode());
            assertEquals("测试消息", response.getMessage());
            assertNotNull(response.getTraceId());
            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("带详情的构造器")
        void testConstructorWithDetail() {
            // When
            AgentErrorResponse response = new AgentErrorResponse("A1001", "测试消息", "详细信息");

            // Then
            assertEquals("A1001", response.getCode());
            assertEquals("测试消息", response.getMessage());
            assertEquals("详细信息", response.getDetail());
        }

        @Test
        @DisplayName("完整构造器")
        void testFullConstructor() {
            // When
            AgentErrorResponse response = new AgentErrorResponse(
                    "A1001", "测试消息", "详情",
                    123456789L, "trace123", true
            );

            // Then
            assertEquals("A1001", response.getCode());
            assertEquals("测试消息", response.getMessage());
            assertEquals("详情", response.getDetail());
            assertEquals(123456789L, response.getTimestamp());
            assertEquals("trace123", response.getTraceId());
            assertTrue(response.getDegraded());
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTests {

        @Test
        @DisplayName("从错误码创建")
        void testFromErrorCode() {
            // When
            AgentErrorResponse response = AgentErrorResponse.from(AgentErrorCode.INVALID_SESSION_ID);

            // Then
            assertEquals("A1001", response.getCode());
            assertEquals("无效的会话ID格式", response.getMessage());
        }

        @Test
        @DisplayName("从错误码创建带详情")
        void testFromErrorCodeWithDetail() {
            // When
            AgentErrorResponse response = AgentErrorResponse.from(AgentErrorCode.SESSION_BUSY, "会话ID: test-123");

            // Then
            assertEquals("A1002", response.getCode());
            assertEquals("会话正在处理中", response.getMessage());
            assertEquals("会话ID: test-123", response.getDetail());
        }

        @Test
        @DisplayName("从异常创建 - TimeoutException")
        void testFromTimeoutException() {
            // Given
            Exception e = new TimeoutException("操作超时");

            // When
            AgentErrorResponse response = AgentErrorResponse.from(e);

            // Then
            assertEquals("A2004", response.getCode()); // TIMEOUT
            assertNotNull(response.getDetail());
        }

        @Test
        @DisplayName("从异常创建 - 普通异常")
        void testFromGenericException() {
            // Given
            Exception e = new RuntimeException("未知错误");

            // When
            AgentErrorResponse response = AgentErrorResponse.from(e);

            // Then
            assertEquals("A2001", response.getCode()); // INTERNAL_ERROR
            assertEquals("未知错误", response.getDetail());
        }

        @Test
        @DisplayName("创建降级响应")
        void testDegradedResponse() {
            // When
            AgentErrorResponse response = AgentErrorResponse.degraded("服务降级中");

            // Then
            assertEquals("A2001", response.getCode());
            assertEquals("服务降级中", response.getMessage());
            assertTrue(response.getDegraded());
        }
    }

    @Nested
    @DisplayName("SSE 事件转换测试")
    class SseEventTests {

        @Test
        @DisplayName("转换为 SSE 事件")
        void testToSseEvent() {
            // Given
            AgentErrorResponse response = new AgentErrorResponse("A1001", "测试消息", null, 0L, "trace123", null);

            // When
            String event = response.toSseEvent();

            // Then
            assertTrue(event.startsWith("event: error\n"));
            assertTrue(event.contains("\"code\":\"A1001\""));
            assertTrue(event.contains("\"message\":\"测试消息\""));
            assertTrue(event.contains("\"traceId\":\"trace123\""));
            assertTrue(event.endsWith("\n\n"));
        }

        @Test
        @DisplayName("转换为带详情的 SSE 事件")
        void testToSseEventWithDetail() {
            // Given
            AgentErrorResponse response = new AgentErrorResponse("A1001", "测试消息", "详细信息", 0L, "trace123", null);

            // When
            String event = response.toSseEventWithDetail();

            // Then
            assertTrue(event.contains("\"detail\":\"详细信息\""));
        }

        @Test
        @DisplayName("转义特殊字符")
        void testEscapeJson() {
            // Given
            AgentErrorResponse response = new AgentErrorResponse("A1001", "消息包含\"引号\"和\n换行", null, 0L, "trace123", null);

            // When
            String event = response.toSseEvent();

            // Then
            assertTrue(event.contains("\\\"引号\\\""));
            assertTrue(event.contains("\\n"));
        }
    }

    @Nested
    @DisplayName("用户友好消息测试")
    class UserFriendlyMessageTests {

        @Test
        @DisplayName("超时错误友好消息")
        void testTimeoutFriendlyMessage() {
            // Given
            AgentErrorResponse response = AgentErrorResponse.from(AgentErrorCode.TIMEOUT);

            // When
            String message = response.toUserFriendlyMessage();

            // Then
            assertEquals("操作超时，请稍后重试", message);
        }

        @Test
        @DisplayName("网络错误友好消息")
        void testNetworkFriendlyMessage() {
            // Given
            AgentErrorResponse response = AgentErrorResponse.from(AgentErrorCode.NETWORK_ERROR);

            // When
            String message = response.toUserFriendlyMessage();

            // Then
            assertEquals("网络连接异常，请检查网络后重试", message);
        }

        @Test
        @DisplayName("限流错误友好消息")
        void testRateLimitFriendlyMessage() {
            // Given
            AgentErrorResponse response = AgentErrorResponse.from(AgentErrorCode.RATE_LIMIT_EXCEEDED);

            // When
            String message = response.toUserFriendlyMessage();

            // Then
            assertEquals("请求过于频繁，请稍后重试", message);
        }

        @Test
        @DisplayName("会话忙碌友好消息")
        void testSessionBusyFriendlyMessage() {
            // Given
            AgentErrorResponse response = AgentErrorResponse.from(AgentErrorCode.SESSION_BUSY);

            // When
            String message = response.toUserFriendlyMessage();

            // Then
            assertEquals("会话正在处理中，请等待完成后再试", message);
        }

        @Test
        @DisplayName("降级响应友好消息")
        void testDegradedFriendlyMessage() {
            // Given
            AgentErrorResponse response = AgentErrorResponse.degraded("测试降级");

            // When
            String message = response.toUserFriendlyMessage();

            // Then
            assertTrue(message.startsWith("服务暂时降级运行："));
        }
    }
}
