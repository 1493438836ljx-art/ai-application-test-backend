package com.example.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务执行器配置类
 * <p>
 * 配置用于执行异步任务（如测试任务执行）的线程池。
 * 采用ThreadPoolTaskExecutor实现，支持任务的异步执行和优雅关闭。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 任务执行器Bean名称 */
    public static final String TASK_EXECUTOR = "taskExecutor";

    /**
     * 创建并配置任务执行器线程池
     * <p>
     * 线程池配置说明：
     * <ul>
     *   <li>核心线程数：5 - 始终保持的线程数量</li>
     *   <li>最大线程数：10 - 高负载时最多可扩展到的线程数</li>
     *   <li>队列容量：100 - 等待执行的任务队列大小</li>
     *   <li>拒绝策略：CallerRunsPolicy - 队列满时由调用线程执行</li>
     * </ul>
     * </p>
     *
     * @return 配置好的任务执行器
     */
    @Bean(name = TASK_EXECUTOR)
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);                                    // 核心线程数
        executor.setMaxPoolSize(10);                                    // 最大线程数
        executor.setQueueCapacity(100);                                 // 队列容量
        executor.setKeepAliveSeconds(60);                               // 空闲线程存活时间（秒）
        executor.setThreadNamePrefix("task-executor-");                 // 线程名前缀，便于排查问题
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());  // 拒绝策略：调用者运行
        executor.setWaitForTasksToCompleteOnShutdown(true);             // 关闭时等待任务完成
        executor.setAwaitTerminationSeconds(60);                        // 最多等待60秒
        executor.initialize();
        return executor;
    }
}
