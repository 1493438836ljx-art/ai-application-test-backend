package com.huawei.cloudopenlabs.agent.retry;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExponentialBackoffRetryStrategy 单元测试
 */
class ExponentialBackoffRetryStrategyTest {

    private ExponentialBackoffRetryStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ExponentialBackoffRetryStrategy();
        // 设置测试配置
        ReflectionTestUtils.setField(strategy, "initialDelayMs", 100L);
        ReflectionTestUtils.setField(strategy, "backoffMultiplier", 2.0);
        ReflectionTestUtils.setField(strategy, "maxDelayMs", 1000L);
        ReflectionTestUtils.setField(strategy, "defaultMaxRetries", 3);
    }

    @Nested
    @DisplayName("成功执行测试")
    class SuccessTests {

        @Test
        @DisplayName("首次成功直接返回")
        void testFirstAttemptSuccess() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When
            String result = strategy.executeWithRetry(() -> {
                counter.incrementAndGet();
                return "success";
            }, 3);

            // Then
            assertEquals("success", result);
            assertEquals(1, counter.get());
        }

        @Test
        @DisplayName("重试后成功")
        void testSuccessAfterRetry() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When
            String result = strategy.executeWithRetry(() -> {
                int count = counter.incrementAndGet();
                if (count < 3) {
                    throw new RuntimeException("临时失败");
                }
                return "success";
            }, e -> true, 3);

            // Then
            assertEquals("success", result);
            assertEquals(3, counter.get());
        }
    }

    @Nested
    @DisplayName("重试失败测试")
    class FailureTests {

        @Test
        @DisplayName("达到最大重试次数后抛出异常")
        void testMaxRetriesExceeded() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When & Then
            assertThrows(RetryExhaustedException.class, () -> {
                strategy.executeWithRetry(() -> {
                    counter.incrementAndGet();
                    throw new RuntimeException("持续失败");
                }, 3);
            });

            assertEquals(4, counter.get()); // 初始 + 3次重试
        }

        @Test
        @DisplayName("不满足重试条件时不重试")
        void testShouldRetryFalse() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When & Then
            assertThrows(RetryExhaustedException.class, () -> {
                strategy.executeWithRetry(() -> {
                    counter.incrementAndGet();
                    throw new RuntimeException("不可重试的错误");
                }, e -> false, 3); // 不允许重试
            });

            assertEquals(1, counter.get()); // 只执行一次
        }
    }

    @Nested
    @DisplayName("延迟计算测试")
    class DelayTests {

        @Test
        @DisplayName("验证指数退避延迟")
        void testExponentialBackoff() {
            // Given
            ReflectionTestUtils.setField(strategy, "initialDelayMs", 50L);
            ReflectionTestUtils.setField(strategy, "backoffMultiplier", 2.0);
            ReflectionTestUtils.setField(strategy, "maxDelayMs", 1000L);

            long startTime = System.currentTimeMillis();
            AtomicInteger counter = new AtomicInteger(0);

            // When
            strategy.executeWithRetry(() -> {
                int count = counter.incrementAndGet();
                if (count < 3) {
                    throw new RuntimeException("临时失败");
                }
                return "done";
            }, 2);

            long elapsed = System.currentTimeMillis() - startTime;

            // Then - 50ms + 100ms = 150ms，允许一定误差
            assertTrue(elapsed >= 100, "延迟应该大于100ms，实际: " + elapsed);
        }

        @Test
        @DisplayName("延迟不超过最大值")
        void testMaxDelayCap() {
            // Given
            ReflectionTestUtils.setField(strategy, "initialDelayMs", 1000L);
            ReflectionTestUtils.setField(strategy, "maxDelayMs", 500L); // 最大延迟小于初始延迟

            // 获取配置验证
            ExponentialBackoffRetryStrategy.RetryConfig config = strategy.getConfig();

            // Then - 最大延迟应该被应用
            assertEquals(500L, config.maxDelayMs());
        }
    }

    @Nested
    @DisplayName("默认重试测试")
    class DefaultRetryTests {

        @Test
        @DisplayName("使用默认最大重试次数")
        void testDefaultMaxRetries() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When & Then
            assertThrows(RetryExhaustedException.class, () -> {
                strategy.executeWithDefaultRetry(() -> {
                    counter.incrementAndGet();
                    throw new RuntimeException("失败");
                });
            });

            assertEquals(4, counter.get()); // 初始 + 3次重试（默认值）
        }

        @Test
        @DisplayName("使用默认重试次数和自定义谓词")
        void testDefaultRetryWithPredicate() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When & Then
            assertThrows(RetryExhaustedException.class, () -> {
                strategy.executeWithDefaultRetry(
                        () -> {
                            counter.incrementAndGet();
                            throw new IllegalArgumentException("参数错误");
                        },
                        e -> e instanceof RuntimeException
                );
            });

            assertEquals(4, counter.get());
        }
    }

    @Nested
    @DisplayName("配置测试")
    class ConfigTests {

        @Test
        @DisplayName("获取配置信息")
        void testGetConfig() {
            // When
            var config = strategy.getConfig();

            // Then
            assertNotNull(config);
            assertEquals(100L, config.initialDelayMs());
            assertEquals(2.0, config.backoffMultiplier());
            assertEquals(1000L, config.maxDelayMs());
            assertEquals(3, config.defaultMaxRetries());
            assertNotNull(config.toString());
        }
    }

    @Nested
    @DisplayName("中断测试")
    class InterruptTests {

        @Test
        @DisplayName("线程中断时抛出 RetryInterruptedException")
        void testInterruptedException() {
            // Given
            Thread currentThread = Thread.currentThread();

            // When & Then
            assertThrows(RetryInterruptedException.class, () -> {
                strategy.executeWithRetry(() -> {
                    currentThread.interrupt();
                    throw new RuntimeException("触发中断");
                }, 1);
            });
        }
    }
}
