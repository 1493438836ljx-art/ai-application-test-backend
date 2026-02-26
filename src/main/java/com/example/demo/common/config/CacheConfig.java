package com.example.demo.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * <p>
 * 使用Caffeine作为本地缓存实现，用于缓存热点数据（如环境配置、插件信息等），
 * 减少数据库访问，提升系统响应速度。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置Caffeine缓存策略
     * <p>
     * 缓存配置说明：
     * <ul>
     *   <li>写入后过期时间：30分钟</li>
     *   <li>初始容量：100</li>
     *   <li>最大容量：1000</li>
     * </ul>
     * </p>
     *
     * @return Caffeine缓存配置对象
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)    // 写入后30分钟过期
                .initialCapacity(100)                       // 初始容量
                .maximumSize(1000);                         // 最大缓存条目数
    }

    /**
     * 创建缓存管理器
     *
     * @param caffeine Caffeine缓存配置
     * @return Caffeine缓存管理器
     */
    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }
}
