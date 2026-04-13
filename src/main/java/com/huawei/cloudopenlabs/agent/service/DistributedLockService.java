/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.service;

// Redis 分布式锁服务实现（需要 Redis 依赖）
// 当前项目未配置 Redis，此服务暂时禁用
// 启用步骤：
// 1. 在 pom.xml 添加 spring-boot-starter-data-redis 依赖
// 2. 在 application.yml 配置 Redis 连接信息
// 3. 取消下方代码注释
// 4. 在 LockServiceConfig 中配置条件加载

/*
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.redis.host")
public class DistributedLockService implements LockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "agent:lock:";
    private static final long DEFAULT_LOCK_TIMEOUT_MS = 30000;

    @Autowired
    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String sessionId) {
        return tryLock(sessionId, DEFAULT_LOCK_TIMEOUT_MS);
    }

    @Override
    public boolean tryLock(String sessionId, long timeoutMs) {
        String lockKey = LOCK_PREFIX + sessionId;
        String lockValue = UUID.randomUUID().toString();

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Distributed lock acquired: sessionId={}", sessionId);
                return true;
            }

            log.warn("Failed to acquire distributed lock: sessionId={}", sessionId);
            return false;

        } catch (Exception e) {
            log.error("Redis operation exception, sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }

    @Override
    public void unlock(String sessionId) {
        String lockKey = LOCK_PREFIX + sessionId;

        try {
            redisTemplate.delete(lockKey);
            log.debug("Releasing distributed lock: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("Distributed lock release exception: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    @Override
    public <T> T executeWithLock(String sessionId, Supplier<T> action) {
        if (!tryLock(sessionId)) {
            throw new AgentSessionBusyException("会话正在处理中，请稍后重试");
        }

        try {
            return action.get();
        } finally {
            unlock(sessionId);
        }
    }

    @Override
    public boolean isHeldByCurrentThread(String sessionId) {
        // Redis 锁无法直接判断是否被当前线程持有
        // 这里简化实现，检查锁是否存在
        String lockKey = LOCK_PREFIX + sessionId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            return false;
        }
    }
}
*/
