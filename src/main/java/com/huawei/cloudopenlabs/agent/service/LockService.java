/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 锁服务接口
 * 支持本地锁和分布式锁的切换
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public interface LockService {

    /**
     * 尝试获取锁
     *
     * @param sessionId 会话ID
     * @return 是否成功获取锁
     */
    boolean tryLock(String sessionId);

    /**
     * 尝试获取锁（带超时）
     *
     * @param sessionId 会话ID
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否成功获取锁
     */
    boolean tryLock(String sessionId, long timeoutMs);

    /**
     * 释放锁
     *
     * @param sessionId 会话ID
     */
    void unlock(String sessionId);

    /**
     * 带锁执行
     *
     * @param sessionId 会话ID
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws AgentSessionBusyException 如果获取锁失败
     */
    <T> T executeWithLock(String sessionId, Supplier<T> action);

    /**
     * 检查锁是否被当前线程持有
     *
     * @param sessionId 会话ID
     * @return 是否被持有
     */
    boolean isHeldByCurrentThread(String sessionId);
}
