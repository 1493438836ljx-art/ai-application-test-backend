package com.example.demo.workflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 用于配置工作流相关的异步任务执行器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 工作流任务执行器
     * 用于处理 Skill 变更事件等异步任务
     *
     * @return 任务执行器
     */
    @Bean("workflowTaskExecutor")
    public Executor workflowTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU 核心数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(Math.max(2, corePoolSize));

        // 最大线程数：核心线程数的 2 倍
        executor.setMaxPoolSize(Math.max(4, corePoolSize * 2));

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名前缀
        executor.setThreadNamePrefix("workflow-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // 初始化
        executor.initialize();

        log.info("工作流任务执行器初始化完成: corePoolSize={}, maxPoolSize={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * 工作流验证执行器
     * 用于处理工作流验证等 CPU 密集型任务
     *
     * @return 任务执行器
     */
    @Bean("workflowValidationExecutor")
    public Executor workflowValidationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(2);

        // 最大线程数
        executor.setMaxPoolSize(4);

        // 队列容量
        executor.setQueueCapacity(50);

        // 线程名前缀
        executor.setThreadNamePrefix("workflow-validation-");

        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 初始化
        executor.initialize();

        log.info("工作流验证执行器初始化完成");

        return executor;
    }
}
