/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 执行器线程池配置
 * <p>
 * 为 Agent 模块提供统一的线程池管理
 * </p>
 *
 * <h3>配置项：</h3>
 * <ul>
 *   <li>agent.executor.core-pool-size: 核心线程数（默认 10）</li>
 *   <li>agent.executor.max-pool-size: 最大线程数（默认 50）</li>
 *   <li>agent.executor.queue-capacity: 队列容量（默认 100）</li>
 *   <li>agent.executor.stream-reader-threads: 流读取线程数（默认 4）</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Slf4j
@Configuration
public class AgentExecutorConfig {

    @Value("${agent.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${agent.executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${agent.executor.queue-capacity:100}")
    private int queueCapacity;

    @Value("${agent.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${agent.executor.stream-reader-threads:4}")
    private int streamReaderThreads;

    @Value("${agent.executor.query-parallel-threads:10}")
    private int queryParallelThreads;

    /**
     * Agent 主执行线程池
     * <p>
     * 用于执行 Agent 的主要处理任务，包括：
     * <ul>
     *   <li>多 round, 处理</li>
     *   <li>查询/操作执行</li>
     *   <li>响应解析</li>
     * </ul>
     * </p>
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean("agentTaskExecutor")
    public ThreadPoolTaskExecutor agentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("agent-exec-");

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("Agent 线程池已满，由调用线程执行任务");
            r.run();
        });

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Agent 主执行线程池初始化完成: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * 流读取专用线程池
     * <p>
     * 用于读取 Claude CLI 的输出流（stdout/stderr）
     * 使用固定大小的线程池，避免资源浪费
     * </p>
     *
     * @return ExecutorService
     */
    @Bean("streamReaderExecutor")
    public ExecutorService streamReaderExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(
                streamReaderThreads,
                new NamedThreadFactory("stream-reader")
        );

        log.info("流读取线程池初始化完成: threads={}", streamReaderThreads);

        return executor;
    }

    /**
     * 查询并行执行线程池
     * <p>
     * 用于并行执行多个独立的查询请求
     * 使用有界队列防止资源耗尽
     * </p>
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean("queryParallelExecutor")
    public ThreadPoolTaskExecutor queryParallelExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(queryParallelThreads);
        executor.setMaxPoolSize(queryParallelThreads * 2);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("query-par-");

        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("查询并行线程池已满，由调用线程执行");
            r.run();
        });

        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);

        executor.initialize();

        log.info("查询并行线程池初始化完成: core={}, max={}",
                queryParallelThreads, queryParallelThreads * 2);

        return executor;
    }

    /**
     * Agent 调度线程池
     * <p>
     * 用于执行定时任务，如：
     * <ul>
     *   <li>空闲锁清理</li>
     *   <li>过期会话清理</li>
     *   <li>统计信息收集</li>
     * </ul>
     * </p>
     *
     * @return ExecutorService
     */
    @Bean("agentScheduledExecutor")
    public ExecutorService agentScheduledExecutor() {
        ExecutorService executor = Executors.newScheduledThreadPool(
                2,
                new NamedThreadFactory("agent-sched")
        );

        log.info("Agent 调度线程池初始化完成");

        return executor;
    }

    /**
     * 命名线程工厂
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
