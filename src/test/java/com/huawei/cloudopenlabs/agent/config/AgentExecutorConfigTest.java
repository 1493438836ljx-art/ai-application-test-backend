package com.huawei.cloudopenlabs.agent.config;

import org.junit.jupiter.api.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentExecutorConfig 单元测试
 */
class AgentExecutorConfigTest {

    private AgentExecutorConfig config;

    @BeforeEach
    void setUp() {
        config = new AgentExecutorConfig();
        // 使用反射设置默认值
        setField(config, "corePoolSize", 10);
        setField(config, "maxPoolSize", 50);
        setField(config, "queueCapacity", 100);
        setField(config, "keepAliveSeconds", 60);
        setField(config, "streamReaderThreads", 4);
        setField(config, "queryParallelThreads", 10);
    }

    @Nested
    @DisplayName("Agent 主执行线程池测试")
    class AgentExecutorTests {

        @Test
        @DisplayName("创建线程池成功")
        void testCreateAgentExecutor() {
            // When
            ThreadPoolTaskExecutor executor = config.agentTaskExecutor();

            // Then
            assertNotNull(executor);
            assertEquals(10, executor.getCorePoolSize());
            assertEquals(50, executor.getMaxPoolSize());
            assertTrue(executor.getQueueCapacity() >= 0);
        }

        @Test
        @DisplayName("线程名称前缀正确")
        void testThreadNamePrefix() {
            // When
            ThreadPoolTaskExecutor executor = config.agentTaskExecutor();

            // Then
            assertNotNull(executor.getThreadNamePrefix());
            assertTrue(executor.getThreadNamePrefix().startsWith("agent"));
        }

        @Test
        @DisplayName("拒绝策略已配置")
        void testRejectedExecutionHandler() {
            // When
            ThreadPoolTaskExecutor executor = config.agentTaskExecutor();

            // Then
            assertNotNull(executor.getThreadPoolExecutor().getRejectedExecutionHandler());
        }

        @Test
        @DisplayName("允许核心线程超时")
        void testAllowCoreThreadTimeout() {
            // When
            ThreadPoolTaskExecutor executor = config.agentTaskExecutor();

            // Then
            assertTrue(executor.getThreadPoolExecutor().allowsCoreThreadTimeOut());
        }
    }

    @Nested
    @DisplayName("流读取线程池测试")
    class StreamReaderExecutorTests {

        @Test
        @DisplayName("创建固定大小线程池")
        void testCreateStreamReaderExecutor() {
            // When
            ExecutorService executor = config.streamReaderExecutor();

            // Then
            assertNotNull(executor);
            assertFalse(executor.isShutdown());
        }

        @Test
        @DisplayName("线程池可关闭")
        void testShutdown() {
            // Given
            ExecutorService executor = config.streamReaderExecutor();

            // When
            executor.shutdown();

            // Then
            assertTrue(executor.isShutdown());
        }
    }

    @Nested
    @DisplayName("查询并行线程池测试")
    class QueryParallelExecutorTests {

        @Test
        @DisplayName("创建线程池成功")
        void testCreateQueryParallelExecutor() {
            // When
            ThreadPoolTaskExecutor executor = config.queryParallelExecutor();

            // Then
            assertNotNull(executor);
            assertEquals(10, executor.getCorePoolSize());
            assertEquals(20, executor.getMaxPoolSize());
        }

        @Test
        @DisplayName("线程名称前缀正确")
        void testThreadNamePrefix() {
            // When
            ThreadPoolTaskExecutor executor = config.queryParallelExecutor();

            // Then
            assertTrue(executor.getThreadNamePrefix().startsWith("query"));
        }
    }

    @Nested
    @DisplayName("调度线程池测试")
    class ScheduledExecutorTests {

        @Test
        @DisplayName("创建调度线程池成功")
        void testCreateScheduledExecutor() {
            // When
            ExecutorService executor = config.agentScheduledExecutor();

            // Then
            assertNotNull(executor);
            assertFalse(executor.isShutdown());
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
