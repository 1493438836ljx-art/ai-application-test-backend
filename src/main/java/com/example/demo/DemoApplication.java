package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 生成式AI应用推理测试平台主启动类
 * <p>
 * 该类是Spring Boot应用的入口点，负责启动整个应用程序。
 * 通过各种注解启用异步处理、缓存、重试、定时任务等功能。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync        // 启用异步方法执行支持
@EnableCaching      // 启用缓存功能
@EnableRetry        // 启用方法重试机制
@EnableScheduling   // 启用定时任务支持
public class DemoApplication {

    /**
     * 应用程序主入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
