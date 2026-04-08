package com.huawei.cloudopenlabs.agent.config;

import com.huawei.cloudopenlabs.agent.service.LocalLockService;
import com.huawei.cloudopenlabs.agent.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 锁服务配置
 * 默认使用本地锁服务
 * 分布式锁服务需要 Redis 依赖，在生产环境中配置
 */
@Slf4j
@Configuration
public class LockServiceConfig {

    /**
     * 本地锁服务
     * 当没有其他 LockService Bean 时使用
     */
    @Bean
    @ConditionalOnMissingBean(LockService.class)
    public LockService localLockService() {
        log.info("使用本地锁服务 (单机模式)");
        return new LocalLockService();
    }
}
