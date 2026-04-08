package com.huawei.cloudopenlabs.agent.observability;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TracingContext 单元测试
 */
class TracingContextTest {

    private TracingContext tracingContext;

    @BeforeEach
    void setUp() {
        tracingContext = new TracingContext();
    }

    @AfterEach
    void tearDown() {
        tracingContext.endTrace();
    }

    @Nested
    @DisplayName("追踪生命周期测试")
    class TraceLifecycleTests {

        @Test
        @DisplayName("开始追踪成功")
        void testStartTrace() {
            // When
            TracingContext.TraceInfo info = tracingContext.startTrace(null);

            // Then
            assertNotNull(info);
            assertNotNull(info.traceId());
            assertNotNull(info.spanId());
            assertTrue(info.startTime() > 0);
        }

        @Test
        @DisplayName("使用自定义追踪ID")
        void testStartTraceWithCustomId() {
            // Given
            String customTraceId = "custom-trace-12345";

            // When
            TracingContext.TraceInfo info = tracingContext.startTrace(customTraceId);

            // Then
            assertEquals(customTraceId, info.traceId());
        }

        @Test
        @DisplayName("结束追踪成功")
        void testEndTrace() {
            // Given
            tracingContext.startTrace(null);
            assertTrue(tracingContext.hasActiveTrace());

            // When
            tracingContext.endTrace();

            // Then
            assertFalse(tracingContext.hasActiveTrace());
        }

        @Test
        @DisplayName("获取追踪ID")
        void testGetTraceId() {
            // Given
            TracingContext.TraceInfo info = tracingContext.startTrace("test-trace-id");

            // When
            String traceId = tracingContext.getTraceId();

            // Then
            assertEquals("test-trace-id", traceId);
        }

        @Test
        @DisplayName("无追踪时返回默认ID")
        void testGetTraceIdWhenNoTrace() {
            // When
            String traceId = tracingContext.getTraceId();

            // Then
            assertEquals("no-trace", traceId);
        }
    }

    @Nested
    @DisplayName("子追踪测试")
    class ChildTraceTests {

        @Test
        @DisplayName("创建子追踪成功")
        void testCreateChildTrace() {
            // Given
            TracingContext.TraceInfo parent = tracingContext.startTrace("parent-trace-id");

            // When
            TracingContext.TraceInfo child = tracingContext.createChildSpan("query-operation");

            // Then
            assertEquals(parent.traceId(), child.traceId());
            assertNotNull(child.spanId());
            assertNotEquals(parent.spanId(), child.spanId());
            assertEquals("query-operation", child.operation());
        }

        @Test
        @DisplayName("无父追踪时创建新追踪")
        void testCreateChildTraceWithoutParent() {
            // When
            TracingContext.TraceInfo info = tracingContext.createChildSpan("test-operation");

            // Then
            assertNotNull(info);
            assertNotNull(info.traceId());
        }
    }

    @Nested
    @DisplayName("线程隔离测试")
    class ThreadIsolationTests {

        @Test
        @DisplayName("不同线程有独立追踪")
        void testThreadIsolation() throws Exception {
            // Given
            tracingContext.startTrace("main-thread-trace");

            // When - 在新线程中检查
            Thread thread = new Thread(() -> {
                // 子线程不应该看到主线程的追踪
                assertFalse(tracingContext.hasActiveTrace());
            });
            thread.start();
            thread.join();

            // Then - 主线程仍有追踪
            assertTrue(tracingContext.hasActiveTrace());
            assertEquals("main-thread-trace", tracingContext.getTraceId());
        }
    }

    @Nested
    @DisplayName("追踪信息测试")
    class TraceInfoTests {

        @Test
        @DisplayName("追踪持续时间计算")
        void testTraceDuration() throws InterruptedException {
            // Given
            TracingContext.TraceInfo info = tracingContext.startTrace(null);
            Thread.sleep(100);

            // When
            long duration = info.duration();

            // Then
            assertTrue(duration >= 100);
        }
    }
}
