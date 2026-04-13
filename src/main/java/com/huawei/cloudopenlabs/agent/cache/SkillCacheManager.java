/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Skill 文件缓存管理器
 * <p>
 * 使用 ConcurrentHashMap 缓存 Skill 文件内容，避免重复加载
 * </p>
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>文件内容缓存（支持 TTL）</li>
 *   <li>解压目录缓存（自动清理）</li>
 *   <li>缓存统计监控</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class SkillCacheManager {

    /**
     * 文件内容缓存
     */
    private final Map<String, CacheEntry<byte[]>> fileCache = new ConcurrentHashMap<>();

    /**
     * 解压目录缓存
     */
    private final Map<String, CacheEntry<String>> dirCache = new ConcurrentHashMap<>();

    /**
     * 缓存统计
     */
    private final CacheStatistics stats = new CacheStatistics();

    /**
     * 清理调度器
     */
    private final ScheduledExecutorService cleanupExecutor;

    @Value("${agent.skill.cache-ttl-minutes:60}")
    private int cacheTtlMinutes;

    @Value("${agent.skill.cache-max-size:100}")
    private int maxCacheSize;

    @Value("${agent.skill.temp-dir:${java.io.tmpdir}/skill-temp}")
    private String skillTempDir;

    public SkillCacheManager() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "skill-cache-cleanup");
            t.setDaemon(true);
            return t;
        });

        // 每5分钟清理过期缓存
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                5, 5, TimeUnit.MINUTES
        );

        log.info("Skill cache manager initialized: ttl={}min, maxSize={}",
                cacheTtlMinutes, maxCacheSize);
    }

    /**
     * 获取 Skill 文件内容（带缓存）
     *
     * @param skillPath Skill 文件路径
     * @return 文件内容字节数组
     * @throws SkillCacheException 如果文件读取失败
     */
    public byte[] getSkillFile(String skillPath) {
        CacheEntry<byte[]> entry = fileCache.get(skillPath);

        if (entry != null && !entry.isExpired()) {
                stats.recordHit();
                return entry.getValue();
        }

        stats.recordMiss();
        try {
            byte[] content = loadSkillFile(skillPath);
            fileCache.put(skillPath, new CacheEntry<>(content, System.currentTimeMillis() + cacheTtlMinutes * 60 * 1000L));
            return content;
        } catch (Exception e) {
            log.error("Failed to get skill file: {}", skillPath, e);
            throw new SkillCacheException("Skill 文件加载失败", e);
        }
    }

    /**
     * 获取 Skill 解压目录（带缓存）
     *
     * @param sessionId 会话 ID
     * @return 解压目录路径
     * @throws SkillCacheException 如果目录准备失败
     */
    public String getSkillDir(String sessionId) {
        CacheEntry<String> entry = dirCache.get(sessionId);

        if (entry != null && !entry.isExpired()) {
            stats.recordHit();
            return entry.getValue();
        }

        stats.recordMiss();
        try {
            String dir = prepareSkillDir(sessionId);
            dirCache.put(sessionId, new CacheEntry<>(dir, System.currentTimeMillis() + cacheTtlMinutes * 60 * 1000L));
            return dir;
        } catch (Exception e) {
            log.error("Failed to prepare skill directory: sessionId={}", sessionId, e);
            throw new SkillCacheException("Skill 目录准备失败", e);
        }
    }

    /**
     * 获取缓存统计
     *
     * @return 缓存统计信息
     */
    public CacheStatistics getStats() {
        return stats;
    }

    /**
     * 清理所有缓存
     */
    public void invalidateAll() {
        fileCache.clear();
        dirCache.forEach((key, entry) -> {
            cleanupDirectory(entry.getValue());
        });
        dirCache.clear();
        log.info("All caches cleared");
    }

    /**
     * 加载 Skill 文件
     */
    private byte[] loadSkillFile(String path) {
        log.info("Loading skill file: {}", path);
        try {
            return Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            throw new SkillCacheException("读取文件失败: " + path, e);
        }
    }

    /**
     * 准备 Skill 目录
     */
    private String prepareSkillDir(String sessionId) {
        log.info("Preparing skill directory: sessionId={}", sessionId);
        Path dirPath = Path.of(skillTempDir, sessionId);
        try {
            Files.createDirectories(dirPath);
            return dirPath.toString();
        } catch (IOException e) {
            throw new SkillCacheException("创建目录失败: " + dirPath, e);
        }
    }

    /**
     * 清理过期缓存条目
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();

        fileCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        dirCache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                cleanupDirectory(entry.getValue().getValue());
                return true;
            }
            return false;
        });

        log.debug("Expired cache cleanup completed: fileCache={}, dirCache={}",
                fileCache.size(), dirCache.size());
    }

    /**
     * 清理目录
     */
    private void cleanupDirectory(String dirPath) {
        log.info("Cleaning cache directory: {}", dirPath);
        try {
            Files.walk(Path.of(dirPath))
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("Failed to clean file: {}", p);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to clean directory: {}", dirPath, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdown();
        invalidateAll();
        log.info("Skill cache manager shut down");
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long expireTime;

        public CacheEntry(T value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
        public T getValue() { return value; }
        public boolean isExpired() { return System.currentTimeMillis() > expireTime; }
    }
}
