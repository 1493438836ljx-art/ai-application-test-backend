package com.huawei.cloudopenlabs.agent.service;

import com.huawei.cloudopenlabs.agent.exception.AgentSessionBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 本地锁服务实现
 * 适用于开发环境和单机部署
 *
 * 注意：此实现不支持分布式环境， * 在多实例部署时，请使用 DistributedLockService
 */
@Slf4j
@Service
public class LocalLockService implements LockService {

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 默认锁超时时间（30秒）
     */
    private static final long DEFAULT_LOCK_TIMEOUT_MS = 30000;

    /**
     * 尝试获取锁
     *
     * @param sessionId 会话ID
     * @return 是否成功获取
     */
    @Override
    public boolean tryLock(String sessionId) {
        return tryLock(sessionId, DEFAULT_LOCK_TIMEOUT_MS);
    }

    /**
     * 尝试获取锁（带超时）
     *
     * @param sessionId 会话ID
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否成功获取
     */
    @Override
    public boolean tryLock(String sessionId, long timeoutMs) {
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("尝试获取锁失败: sessionId 为空");
            return false;
        }

        ReentrantLock lock = getOrCreateLock(sessionId);
        try {
            boolean acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("成功获取锁: sessionId={}", sessionId);
            } else {
                log.warn("获取锁超时: sessionId={}", sessionId);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取锁被中断: sessionId={}", sessionId);
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param sessionId 会话ID
     */
    @Override
    public void unlock(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }

        ReentrantLock lock = lockMap.get(sessionId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放锁: sessionId={}", sessionId);
        }
    }

    /**
     * 带锁执行
     *
     * @param sessionId 会话ID
     * @param action 要执行的操作
     * @return 操作结果
     */
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

    /**
     * 检查锁是否被当前线程持有
     */
    @Override
    public boolean isHeldByCurrentThread(String sessionId) {
        ReentrantLock lock = lockMap.get(sessionId);
        return lock != null && lock.isHeldByCurrentThread();
    }

    /**
     * 获取或创建锁
     */
    private ReentrantLock getOrCreateLock(String sessionId) {
        return lockMap.computeIfAbsent(sessionId, k -> new ReentrantLock());
    }

    /**
     * 定期清理空闲锁
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupIdleLocks() {
        int beforeSize = lockMap.size();
        lockMap.entrySet().removeIf(entry -> {
            ReentrantLock lock = entry.getValue();
            return !lock.isLocked();
        });
        int afterSize = lockMap.size();
        if (beforeSize != afterSize) {
            log.debug("清理空闲锁: 清理前={}, 清理后={}", beforeSize, afterSize);
        }
    }
}
