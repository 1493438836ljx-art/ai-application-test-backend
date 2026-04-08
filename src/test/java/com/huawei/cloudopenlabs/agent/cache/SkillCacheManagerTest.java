package com.huawei.cloudopenlabs.agent.cache;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillCacheManager 单元测试
 */
class SkillCacheManagerTest {

    private SkillCacheManager cacheManager;
    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        // 创建临时测试文件
        tempFile = Files.createTempFile("skill-test-", ".txt");
        Files.writeString(tempFile, "test skill content");

        cacheManager = new SkillCacheManager();
        // 设置测试配置
        ReflectionTestUtils.setField(cacheManager, "cacheTtlMinutes", 60);
        ReflectionTestUtils.setField(cacheManager, "maxCacheSize", 100);
        ReflectionTestUtils.setField(cacheManager, "skillTempDir", System.getProperty("java.io.tmpdir"));
    }

    @AfterEach
    void tearDown() {
        cacheManager.invalidateAll();
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
        }
    }

    @Nested
    @DisplayName("文件缓存测试")
    class FileCacheTests {

        @Test
        @DisplayName("获取存在的文件成功")
        void testGetExistingFile() {
            // When
            byte[] result = cacheManager.getSkillFile(tempFile.toString());

            // Then
            assertNotNull(result);
            assertEquals("test skill content", new String(result));
        }

        @Test
        @DisplayName("获取不存在的文件抛出异常")
        void testGetNonExistentFile() {
            // When & Then
            assertThrows(SkillCacheException.class, () -> {
                cacheManager.getSkillFile("/non/existent/file");
            });
        }

        @Test
        @DisplayName("缓存命中后直接返回")
        void testCacheHit() {
            // Given - 第一次加载
            byte[] first = cacheManager.getSkillFile(tempFile.toString());
            assertNotNull(first);

            // When - 第二次获取
            byte[] second = cacheManager.getSkillFile(tempFile.toString());

            // Then
            assertNotNull(second);
            assertArrayEquals(first, second);
        }
    }

    @Nested
    @DisplayName("目录缓存测试")
    class DirCacheTests {

        @Test
        @DisplayName("获取目录成功")
        void testGetDir() {
            // When
            String dir = cacheManager.getSkillDir("test-session-123");

            // Then
            assertNotNull(dir);
        }

        @Test
        @DisplayName("相同会话返回相同目录")
        void testSameSessionSameDir() {
            // Given
            String first = cacheManager.getSkillDir("test-session");

            // When
            String second = cacheManager.getSkillDir("test-session");

            // Then
            assertEquals(first, second);
        }
    }

    @Nested
    @DisplayName("缓存统计测试")
    class CacheStatsTests {

        @Test
        @DisplayName("获取缓存统计")
        void testGetStats() {
            // When
            CacheStatistics stats = cacheManager.getStats();

            // Then
            assertNotNull(stats);
        }

        @Test
        @DisplayName("缓存命中和未命中统计")
        void testCacheHitAndMissStats() {
            // Given - 重置统计
            cacheManager.getStats().reset();

            // When - 第一次获取（未命中）
            cacheManager.getSkillFile(tempFile.toString());
            CacheStatistics stats1 = cacheManager.getStats();

            // Then
            assertEquals(1, stats1.missCount());
            assertEquals(0, stats1.hitCount());

            // When - 第二次获取（命中）
            cacheManager.getSkillFile(tempFile.toString());
            CacheStatistics stats2 = cacheManager.getStats();

            // Then
            assertEquals(1, stats2.hitCount());
            assertEquals(1, stats2.missCount());
        }

        @Test
        @DisplayName("清理所有缓存")
        void testInvalidateAll() {
            // Given - 先获取文件以填充缓存
            cacheManager.getSkillFile(tempFile.toString());
            cacheManager.getStats().reset();

            // When
            cacheManager.invalidateAll();

            // When - 再次获取（应该是未命中）
            cacheManager.getSkillFile(tempFile.toString());
            CacheStatistics stats = cacheManager.getStats();

            // Then
            assertEquals(1, stats.missCount());
        }
    }

    @Nested
    @DisplayName("统计类测试")
    class CacheStatisticsTests {

        @Test
        @DisplayName("统计初始值")
        void testInitialStats() {
            // Given
            CacheStatistics stats = new CacheStatistics();

            // Then
            assertEquals(0, stats.hitCount());
            assertEquals(0, stats.missCount());
            assertEquals(0, stats.requestCount());
            assertEquals(0.0, stats.hitRate());
        }

        @Test
        @DisplayName("统计记录")
        void testRecordStats() {
            // Given
            CacheStatistics stats = new CacheStatistics();

            // When
            stats.recordHit();
            stats.recordHit();
            stats.recordMiss();

            // Then
            assertEquals(2, stats.hitCount());
            assertEquals(1, stats.missCount());
            assertEquals(3, stats.requestCount());
            assertEquals(2.0/3.0, stats.hitRate(), 0.001);
        }

        @Test
        @DisplayName("统计重置")
        void testResetStats() {
            // Given
            CacheStatistics stats = new CacheStatistics();
            stats.recordHit();
            stats.recordMiss();

            // When
            stats.reset();

            // Then
            assertEquals(0, stats.hitCount());
            assertEquals(0, stats.missCount());
        }
    }
}
