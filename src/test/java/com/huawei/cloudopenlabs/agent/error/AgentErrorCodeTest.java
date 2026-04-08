package com.huawei.cloudopenlabs.agent.error;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentErrorCode 单元测试
 */
class AgentErrorCodeTest {

    @Nested
    @DisplayName("错误码分类测试")
    class CategoryTests {

        @Test
        @DisplayName("客户端错误判断")
        void testIsClientError() {
            assertTrue(AgentErrorCode.INVALID_SESSION_ID.isClientError());
            assertTrue(AgentErrorCode.SESSION_BUSY.isClientError());
            assertTrue(AgentErrorCode.INVALID_REQUEST.isClientError());
            assertFalse(AgentErrorCode.INTERNAL_ERROR.isClientError());
        }

        @Test
        @DisplayName("服务端错误判断")
        void testIsServerError() {
            assertTrue(AgentErrorCode.INTERNAL_ERROR.isServerError());
            assertTrue(AgentErrorCode.CLI_EXECUTION_FAILED.isServerError());
            assertTrue(AgentErrorCode.TIMEOUT.isServerError());
            assertFalse(AgentErrorCode.INVALID_SESSION_ID.isServerError());
        }

        @Test
        @DisplayName("外部服务错误判断")
        void testIsExternalError() {
            assertTrue(AgentErrorCode.EXTERNAL_SERVICE_ERROR.isExternalError());
            assertTrue(AgentErrorCode.DATABASE_ERROR.isExternalError());
            assertTrue(AgentErrorCode.NETWORK_ERROR.isExternalError());
            assertFalse(AgentErrorCode.INTERNAL_ERROR.isExternalError());
        }

        @Test
        @DisplayName("限流错误判断")
        void testIsRateLimitError() {
            assertTrue(AgentErrorCode.RATE_LIMIT_EXCEEDED.isRateLimitError());
            assertTrue(AgentErrorCode.CONCURRENT_LIMIT.isRateLimitError());
            assertFalse(AgentErrorCode.TIMEOUT.isRateLimitError());
        }
    }

    @Nested
    @DisplayName("错误响应创建测试")
    class ResponseTests {

        @Test
        @DisplayName("创建基本错误响应")
        void testToResponse() {
            // When
            AgentErrorResponse response = AgentErrorCode.INVALID_SESSION_ID.toResponse();

            // Then
            assertNotNull(response);
            assertEquals("A1001", response.getCode());
            assertEquals("无效的会话ID格式", response.getMessage());
            assertNotNull(response.getTraceId());
            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("创建带详情的错误响应")
        void testToResponseWithDetail() {
            // When
            AgentErrorResponse response = AgentErrorCode.SESSION_BUSY.toResponse("会话正在处理查询操作");

            // Then
            assertNotNull(response);
            assertEquals("A1002", response.getCode());
            assertEquals("会话正在处理中", response.getMessage());
            assertEquals("会话正在处理查询操作", response.getDetail());
        }

        @Test
        @DisplayName("创建带异常的错误响应")
        void testToResponseWithException() {
            // Given
            Exception e = new RuntimeException("测试异常");

            // When
            AgentErrorResponse response = AgentErrorCode.INTERNAL_ERROR.toResponse(e);

            // Then
            assertNotNull(response);
            assertEquals("A2001", response.getCode());
            assertEquals("测试异常", response.getDetail());
        }
    }

    @Nested
    @DisplayName("错误码查找测试")
    class LookupTests {

        @Test
        @DisplayName("根据错误码查找枚举")
        void testFromCode() {
            // When & Then
            assertEquals(AgentErrorCode.INVALID_SESSION_ID, AgentErrorCode.fromCode("A1001"));
            assertEquals(AgentErrorCode.INTERNAL_ERROR, AgentErrorCode.fromCode("A2001"));
            assertEquals(AgentErrorCode.RATE_LIMIT_EXCEEDED, AgentErrorCode.fromCode("A4001"));
        }

        @Test
        @DisplayName("未知错误码返回内部错误")
        void testFromCodeUnknown() {
            // When
            AgentErrorCode code = AgentErrorCode.fromCode("UNKNOWN");

            // Then
            assertEquals(AgentErrorCode.INTERNAL_ERROR, code);
        }
    }

    @Nested
    @DisplayName("错误码属性测试")
    class PropertyTests {

        @Test
        @DisplayName("获取错误码和消息")
        void testGetCodeAndMessage() {
            // When & Then
            assertEquals("A1001", AgentErrorCode.INVALID_SESSION_ID.getCode());
            assertEquals("无效的会话ID格式", AgentErrorCode.INVALID_SESSION_ID.getMessage());
        }

        @Test
        @DisplayName("成功状态码")
        void testSuccessCode() {
            // When & Then
            assertEquals("A0000", AgentErrorCode.SUCCESS.getCode());
            assertEquals("操作成功", AgentErrorCode.SUCCESS.getMessage());
        }
    }
}
