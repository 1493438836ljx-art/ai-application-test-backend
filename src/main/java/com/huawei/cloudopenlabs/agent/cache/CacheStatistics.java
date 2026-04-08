package com.huawei.cloudopenlabs.agent.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计信息
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 */
public class CacheStatistics {

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    public void recordHit() {
        hitCount.incrementAndGet();
    }

    public void recordMiss() {
        missCount.incrementAndGet();
    }

    public long hitCount() {
        return hitCount.get();
    }

    public long missCount() {
        return missCount.get();
    }

    public long requestCount() {
        return hitCount.get() + missCount.get();
    }

    public double hitRate() {
        long total = requestCount();
        return total == 0 ? 0.0 : (double) hitCount.get() / total;
    }

    public void reset() {
        hitCount.set(0);
        missCount.set(0);
    }

    @Override
    public String toString() {
        return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f%%}",
                hitCount.get(), missCount.get(), hitRate() * 100);
    }
}
