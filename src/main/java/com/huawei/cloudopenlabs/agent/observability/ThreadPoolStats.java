/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.observability;

/**
 * 线程池统计信息
 *
 * @param activeCount  活跃线程数
 * @param poolSize     当前线程池大小
 * @param maxPoolSize  最大线程池大小
 * @param queueSize    队列大小
 * @param completedTaskCount 已完成任务数
 *
 * @author GNEEC LIVE
 * @version 27.0.4.0
 * @since 2026-04-13
 */
public record ThreadPoolStats(
        int activeCount,
        int poolSize,
        int maxPoolSize,
        int queueSize,
        long completedTaskCount
) {
    /**
     * 计算队列使用率
     */
    public double queueUsageRate() {
        if (maxPoolSize == 0) return 0.0;
        return (double) queueSize / maxPoolSize;
    }

    /**
     * 计算线程使用率
     */
    public double threadUsageRate() {
        if (maxPoolSize == 0) return 0.0;
        return (double) activeCount / maxPoolSize;
    }

    /**
     * 判断是否繁忙
     */
    public boolean isBusy(double threshold) {
        return queueUsageRate() > threshold || threadUsageRate() > threshold;
    }

    @Override
    public String toString() {
        return String.format(
                "ThreadPoolStats{active=%d, pool=%d, max=%d, queue=%d, completed=%d}",
                activeCount, poolSize, maxPoolSize, queueSize, completedTaskCount
        );
    }
}
