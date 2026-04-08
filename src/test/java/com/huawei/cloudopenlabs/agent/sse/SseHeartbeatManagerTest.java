package com.huawei.cloudopenlabs.agent.sse;

import org.junit.jupiter.api.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SseHeartbeatManager 单元测试
 */
class SseHeartbeatManagerTest {

    private SseHeartbeatManager heartbeatManager;

    @BeforeEach
    void setUp() {
        heartbeatManager = new SseHeartbeatManager();
        // 设置测试配置
        setField(heartbeatManager, "heartbeatIntervalMs", 1000L);
        setField(heartbeatManager, "heartbeatTimeoutCount", 3);
    }

    @AfterEach
    void tearDown() {
        heartbeatManager.shutdown();
    }

    @Nested
    @DisplayName("连接注册测试")
    class RegisterConnectionTests {

        @Test
        @DisplayName("注册连接成功")
        void testRegisterConnectionSuccess() {
            // When
            SseEmitter emitter = heartbeatManager.registerConnection("test-1", 60000L);

            // Then
            assertNotNull(emitter);
            assertTrue(heartbeatManager.isActive("test-1"));
            assertEquals(1, heartbeatManager.getActiveConnectionCount());
        }

        @Test
        @DisplayName("注册多个连接")
        void testRegisterMultipleConnections() {
            // When
            heartbeatManager.registerConnection("session-1", 60000L);
            heartbeatManager.registerConnection("session-2", 60000L);
            heartbeatManager.registerConnection("session-3", 60000L);

            // Then
            assertEquals(3, heartbeatManager.getActiveConnectionCount());
            assertTrue(heartbeatManager.isActive("session-1"));
            assertTrue(heartbeatManager.isActive("session-2"));
            assertTrue(heartbeatManager.isActive("session-3"));
        }

        @Test
        @DisplayName("重复注册同一会话ID覆盖旧连接")
        void testRegisterSameSessionIdTwice() {
            // Given
            heartbeatManager.registerConnection("same-id", 60000L);

            // When
            heartbeatManager.registerConnection("same-id", 60000L);

            // Then
            assertEquals(1, heartbeatManager.getActiveConnectionCount());
        }
    }

    @Nested
    @DisplayName("连接清理测试")
    class CleanupTests {

        @Test
        @DisplayName("完成连接后清理")
        void testCompleteConnection() {
            // Given
            heartbeatManager.registerConnection("test-complete", 60000L);
            assertTrue(heartbeatManager.isActive("test-complete"));

            // When
            heartbeatManager.completeConnection("test-complete");

            // Then
            assertFalse(heartbeatManager.isActive("test-complete"));
            assertEquals(0, heartbeatManager.getActiveConnectionCount());
        }

        @Test
        @DisplayName("清理不存在的连接不报错")
        void testCleanupNonExistentConnection() {
            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> heartbeatManager.completeConnection("non-existent"));
        }
    }

    @Nested
    @DisplayName("事件发送测试")
    class SendEventTests {

        @Test
        @DisplayName("发送事件成功")
        void testSendEventSuccess() {
            // Given
            String sessionId = "event-test";
            AtomicReference<Object> receivedData = new AtomicReference<>();

            SseEmitter emitter = heartbeatManager.registerConnection(sessionId, 60000L);
            emitter.onCompletion(() -> {});

            // When
            boolean result = heartbeatManager.sendEvent(sessionId, "test-event", "test-data");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("发送事件到不存在的连接失败")
        void testSendEventToNonExistentConnection() {
            // When
            boolean result = heartbeatManager.sendEvent("non-existent", "test-event", "test-data");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("发送错误事件")
        void testSendError() {
            // Given
            String sessionId = "error-test";
            heartbeatManager.registerConnection(sessionId, 60000L);

            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> heartbeatManager.sendError(sessionId, "A1001", "测试错误"));
        }
    }

    @Nested
    @DisplayName("状态查询测试")
    class StatusTests {

        @Test
        @DisplayName("检查活跃状态")
        void testIsActive() {
            // Given
            heartbeatManager.registerConnection("active-test", 60000L);

            // Then
            assertTrue(heartbeatManager.isActive("active-test"));
            assertFalse(heartbeatManager.isActive("inactive-test"));
        }

        @Test
        @DisplayName("获取连接数")
        void testGetActiveConnectionCount() {
            // When
            heartbeatManager.registerConnection("count-1", 60000L);
            assertEquals(1, heartbeatManager.getActiveConnectionCount());

            heartbeatManager.registerConnection("count-2", 60000L);
            assertEquals(2, heartbeatManager.getActiveConnectionCount());

            heartbeatManager.completeConnection("count-1");
            assertEquals(1, heartbeatManager.getActiveConnectionCount());
        }

        @Test
        @DisplayName("获取统计信息")
        void testGetStats() {
            // Given
            heartbeatManager.registerConnection("stats-1", 60000L);
            heartbeatManager.registerConnection("stats-2", 60000L);

            // When
            SseHeartbeatManager.ConnectionStats stats = heartbeatManager.getStats();

            // Then
            assertEquals(2, stats.activeCount());
            assertNotNull(stats.toString());
        }
    }

    @Nested
    @DisplayName("心跳机制测试")
    class HeartbeatTests {

        @Test
        @DisplayName("心跳任务正常启动")
        void testHeartbeatTaskStarted() throws InterruptedException {
            // Given
            String sessionId = "heartbeat-test";
            AtomicBoolean heartbeatReceived = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            SseEmitter emitter = heartbeatManager.registerConnection(sessionId, 60000L);

            emitter.onCompletion(latch::countDown);

            // 等待至少一个心跳周期
            boolean completed = latch.await(2, TimeUnit.SECONDS);

            // Then - 连接仍在活跃列表中说明心跳正常
            assertTrue(heartbeatManager.isActive(sessionId));
        }
    }

    // ==================== 辅助方法 ====================

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            // 忽略
        }
    }
}
