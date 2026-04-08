package com.huawei.cloudopenlabs.agent.observability;

import com.huawei.cloudopenlabs.agent.cache.CacheStatistics;
import com.huawei.cloudopenlabs.agent.cache.SkillCacheManager;
import com.huawei.cloudopenlabs.agent.sse.SseHeartbeatManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentHealthIndicator 单元测试
 */
class AgentHealthIndicatorTest {

    private AgentHealthIndicator healthIndicator;
    private AgentMetrics metrics;
    private SkillCacheManager cacheManager;
    private SseHeartbeatManager heartbeatManager;
    private ThreadPoolTaskExecutor taskExecutor;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new AgentMetrics(meterRegistry);
        cacheManager = new SkillCacheManager();
        heartbeatManager = new SseHeartbeatManager();
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.initialize();

        healthIndicator = new AgentHealthIndicator(metrics, cacheManager, heartbeatManager, taskExecutor);

        // 设置配置
        ReflectionTestUtils.setField(healthIndicator, "queueUsageThreshold", 0.8);
        ReflectionTestUtils.setField(healthIndicator, "cacheHitRateThreshold", 0.5);
    }

    @AfterEach
    void tearDown() {
        taskExecutor.shutdown();
        heartbeatManager.shutdown();
    }

    @Nested
    @DisplayName("健康检查测试")
    class HealthCheckTests {

        @Test
        @DisplayName("正常健康状态")
        void testHealthUp() {
                // When
                Health health = healthIndicator.health();

                // Then
                assertEquals(Status.UP, health.getStatus());
                assertNotNull(health.getDetails().get("activeSessions"));
                assertNotNull(health.getDetails().get("activeConnections"));
                assertNotNull(health.getDetails().get("cacheHitRate"));
                assertNotNull(health.getDetails().get("threadPoolActive"));
            }

        @Test
        @DisplayName("健康检查包含详细信息")
        void testHealthContainsDetails() {
                // When
                Health health = healthIndicator.health();

                // Then
                assertTrue(health.getDetails().containsKey("activeSessions"));
                assertTrue(health.getDetails().containsKey("activeConnections"));
                assertTrue(health.getDetails().containsKey("cacheHitRate"));
                assertTrue(health.getDetails().containsKey("threadPoolActive"));
                assertTrue(health.getDetails().containsKey("threadPoolMax"));
                assertTrue(health.getDetails().containsKey("threadPoolQueue"));
            }

        @Test
        @DisplayName("轻量级健康检查")
        void testIsHealthy() {
                // When
                boolean healthy = healthIndicator.isHealthy();

                // Then
                assertTrue(healthy);
            }
    }

    @Nested
    @DisplayName("组件状态测试")
    class ComponentStatusTests {

        @Test
        @DisplayName("有活跃会话时正确计数")
        void testActiveSessionsCount() {
                // Given
                metrics.recordRequest();
                metrics.recordRequest();

                // When
                Health health = healthIndicator.health();

                // Then
                assertEquals(2, health.getDetails().get("activeSessions"));
        }

        @Test
        @DisplayName("活跃连接数检查")
        void testActiveConnectionsCount() {
                // When - 获取健康状态，初始时无连接
                Health health = healthIndicator.health();

                // Then - 活跃连接数从 heartbeatManager 获取，初始为0
                int activeConnections = (Integer) health.getDetails().get("activeConnections");
                assertEquals(0, activeConnections);
            }
    }

    @Nested
    @DisplayName("线程池统计测试")
    class ThreadPoolStatsTests {

        @Test
        @DisplayName("线程池统计信息正确")
        void testThreadPoolStats() {
                // When
                ThreadPoolStats stats = new ThreadPoolStats(5, 10, 20, 3, 100);

                // Then
                assertEquals(5, stats.activeCount());
                assertEquals(10, stats.poolSize());
                assertEquals(20, stats.maxPoolSize());
                assertEquals(3, stats.queueSize());
                assertEquals(100, stats.completedTaskCount());
            }

        @Test
        @DisplayName("计算队列使用率")
        void testQueueUsageRate() {
                // Given
                ThreadPoolStats stats = new ThreadPoolStats(5, 10, 20, 8, 100);

                // When
                double rate = stats.queueUsageRate();

                // Then
                assertEquals(0.4, rate, 0.01);
            }

        @Test
        @DisplayName("计算线程使用率")
        void testThreadUsageRate() {
                // Given
                ThreadPoolStats stats = new ThreadPoolStats(10, 10, 20, 0, 100);

                // When
                double rate = stats.threadUsageRate();

                // Then
                assertEquals(0.5, rate, 0.01);
            }

        @Test
        @DisplayName("判断是否繁忙")
        void testIsBusy() {
                // Given
                ThreadPoolStats notBusy = new ThreadPoolStats(5, 10, 20, 2, 100);
                ThreadPoolStats busy = new ThreadPoolStats(18, 20, 20, 15, 100);

                // Then
                assertFalse(notBusy.isBusy(0.8));
                assertTrue(busy.isBusy(0.5));
            }
    }
}
