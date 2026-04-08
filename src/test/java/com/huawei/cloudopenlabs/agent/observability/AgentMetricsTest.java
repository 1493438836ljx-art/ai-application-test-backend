package com.huawei.cloudopenlabs.agent.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentMetrics 单元测试
 */
class AgentMetricsTest {

    private AgentMetrics metrics;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new AgentMetrics(meterRegistry);
    }

    @Nested
    @DisplayName("请求计数测试")
    class RequestCountTests {

        @Test
        @DisplayName("记录请求")
        void testRecordRequest() {
                // When
                metrics.recordRequest();

                // Then
                assertEquals(1, metrics.getActiveSessionCount());
                assertEquals(1.0, metrics.getRequestCount());
        }

        @Test
        @DisplayName("记录成功")
        void testRecordSuccess() {
                // Given
                metrics.recordRequest();

                // When
                metrics.recordSuccess();

                // Then
                assertEquals(0, metrics.getActiveSessionCount());
                assertEquals(1.0, metrics.getSuccessCount());
        }

        @Test
        @DisplayName("记录失败")
        void testRecordFailure() {
                // Given
                metrics.recordRequest();

                // When
                metrics.recordFailure();

                // Then
                assertEquals(0, metrics.getActiveSessionCount());
                assertEquals(1.0, metrics.getFailureCount());
        }

        @Test
        @DisplayName("记录超时")
        void testRecordTimeout() {
                // Given
                metrics.recordRequest();

                // When
                metrics.recordTimeout();

                // Then
                assertEquals(0, metrics.getActiveSessionCount());
                assertEquals(1.0, metrics.getTimeoutCount());
        }
    }

    @Nested
    @DisplayName("计时器测试")
    class TimerTests {

        @Test
        @DisplayName("记录请求耗时")
        void testRecordRequestDuration() {
                // When
                metrics.recordRequestDuration(100);

                // Then - 不应抛出异常
                assertTrue(true);
        }

        @Test
        @DisplayName("记录 CLI 耗时")
        void testRecordCliDuration() {
                // When
                metrics.recordCliDuration(500);

                // Then - 不应抛出异常
                assertTrue(true);
        }

        @Test
        @DisplayName("记录 CLI 耗时（带命令）")
        void testRecordCliDurationWithCommand() {
                // When
                metrics.recordCliDuration(500, "execute");

                // Then - 不应抛出异常
                assertTrue(true);
        }

        @Test
        @DisplayName("记录查询耗时")
        void testRecordQueryDuration() {
                // When
                metrics.recordQueryDuration(200);

                // Then - 不应抛出异常
                assertTrue(true);
        }

        @Test
        @DisplayName("记录操作耗时")
        void testRecordActionDuration() {
                // When
                metrics.recordActionDuration(300);

                // Then - 不应抛出异常
                assertTrue(true);
        }
    }

    @Nested
    @DisplayName("连接状态测试")
    class ConnectionStateTests {

        @Test
        @DisplayName("记录连接增加")
        void testRecordConnectionIncrement() {
                // When
                metrics.recordConnectionIncrement();

                // Then
                assertEquals(1, metrics.getActiveConnectionCount());
        }

        @Test
        @DisplayName("记录连接减少")
        void testRecordConnectionDecrement() {
                // Given
                metrics.recordConnectionIncrement();

                // When
                metrics.recordConnectionDecrement();

                // Then
                assertEquals(0, metrics.getActiveConnectionCount());
        }

        @Test
        @DisplayName("设置连接数")
        void testSetActiveConnections() {
                // When
                metrics.setActiveConnections(10);

                // Then
                assertEquals(10, metrics.getActiveConnectionCount());
        }
    }

    @Nested
    @DisplayName("统计计算测试")
    class StatisticsTests {

        @Test
        @DisplayName("计算成功率")
        void testGetSuccessRate() {
                // Given
                metrics.recordRequest();
                metrics.recordRequest();
                metrics.recordSuccess();

                // When
                double rate = metrics.getSuccessRate();

                // Then
                assertEquals(0.5, rate, 0.01);
        }

        @Test
        @DisplayName("计算错误率")
        void testGetErrorRate() {
                // Given
                metrics.recordRequest();
                metrics.recordRequest();
                metrics.recordRequest();
                metrics.recordFailure();
                metrics.recordTimeout();

                // When
                double rate = metrics.getErrorRate();

                // Then
                assertTrue(rate > 0.6 && rate < 0.7);
        }

        @Test
        @DisplayName("无请求时成功率为1")
        void testGetSuccessRateWhenNoRequests() {
                // When
                double rate = metrics.getSuccessRate();

                // Then
                assertEquals(1.0, rate, 0.01);
        }

        @Test
        @DisplayName("无请求时错误率为0")
        void testGetErrorRateWhenNoRequests() {
                // When
                double rate = metrics.getErrorRate();

                // Then
                assertEquals(0.0, rate, 0.01);
        }
    }
}
