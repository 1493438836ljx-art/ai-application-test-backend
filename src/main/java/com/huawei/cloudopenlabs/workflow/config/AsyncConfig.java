/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.config;

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
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
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

        log.info("Workflow task executor initialized: corePoolSize={}, maxPoolSize={}",
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

        log.info("Workflow validation executor initialized");

        return executor;
    }

    /**
     * 工作流执行线程池
     * 用于异步执行工作流
     *
     * @return 任务执行器
     */
    @Bean("workflowExecutor")
    public ThreadPoolTaskExecutor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：10
        executor.setCorePoolSize(10);

        // 最大线程数：50
        executor.setMaxPoolSize(50);

        // 队列容量：500
        executor.setQueueCapacity(500);

        // 线程名前缀
        executor.setThreadNamePrefix("workflow-exec-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // 初始化
        executor.initialize();

        log.info("Workflow execution thread pool initialized: corePoolSize=10, maxPoolSize=50, queueCapacity=500");

        return executor;
    }

    /**
     * 节点执行线程池
     * 用于并行执行工作流中的节点
     *
     * @return 任务执行器
     */
    @Bean("nodeExecutor")
    public Executor nodeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：20
        executor.setCorePoolSize(20);

        // 最大线程数：100
        executor.setMaxPoolSize(100);

        // 队列容量：1000
        executor.setQueueCapacity(1000);

        // 线程名前缀
        executor.setThreadNamePrefix("node-exec-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // 初始化
        executor.initialize();

        log.info("Node execution thread pool initialized: corePoolSize=20, maxPoolSize=100, queueCapacity=1000");

        return executor;
    }
}
