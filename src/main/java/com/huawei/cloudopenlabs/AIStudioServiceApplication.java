/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 生成式AI应用推理测试平台主启动类
 * <p>
 * 该类是Spring Boot应用的入口点，负责启动整个应用程序。
 * 通过各种注解启用异步处理、缓存、重试、定时任务等功能。
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@SpringBootApplication
@EnableAsync        // 启用异步方法执行支持
@EnableScheduling   // 启用定时任务支持
public class AIStudioServiceApplication {

    /**
     * 应用程序主入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AIStudioServiceApplication.class, args);
    }
}
