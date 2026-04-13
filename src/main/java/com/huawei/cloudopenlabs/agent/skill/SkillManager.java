/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Skill 管理器
 * <p>
 * 负责 Skill 文件的管理，包括解压、缓存和清理
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class SkillManager {

    @Value("${claude.code.skill-file-path:skills/workflow-assistant.zip}")
    private String skillFilePath;

    @Value("${claude.code.skill-cache-dir:uploads/skills}")
    private String skillCacheDir;

    /**
     * 准备 Skill 目录
     *
     * @param sessionId 会话ID
     * @return Skill 目录路径
     */
    public String prepareSkill(String sessionId) {
        Path cachePath = Paths.get(skillCacheDir);
        Path skillDir = cachePath.resolve("skill-" + sessionId);

        // 如果目录已存在，直接返回
        if (Files.exists(skillDir)) {
            log.debug("Skill directory already exists: {}", skillDir);
            return skillDir.toAbsolutePath().toString();
        }

        // 检查 Skill 文件是否存在
        Path skillFile = Paths.get(skillFilePath);
        if (!Files.exists(skillFile)) {
            log.warn("Skill file not found: {}，using default config", skillFile);
            return null;
        }

        try {
            // 解压 Skill 文件
            extractZip(skillFile, skillDir);
            log.info("Skill extraction completed: {} -> {}", skillFile, skillDir);
            return skillDir.toAbsolutePath().toString();

        } catch (IOException e) {
            log.error("Failed to extract skill file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 安全解压 ZIP 文件
     */
    private void extractZip(Path zipFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());

                // 安全检查：防止路径遍历
                if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                    log.warn("Path traversal attack detected, skipping: {}", entry.getName());
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }
    }

    /**
     * 清理过期的 Skill 目录
     *
     * @param maxAgeMs 最大存活时间（毫秒）
     */
    public void cleanupExpiredSkills(long maxAgeMs) {
        Path cachePath = Paths.get(skillCacheDir);
        if (!Files.exists(cachePath)) {
            return;
        }

        long now = System.currentTimeMillis();
        int cleanedCount = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cachePath, "skill-*")) {
            for (Path skillDir : stream) {
                if (Files.isDirectory(skillDir)) {
                    long lastModified = Files.getLastModifiedTime(skillDir).toMillis();
                    if (now - lastModified > maxAgeMs) {
                        deleteDirectory(skillDir);
                        cleanedCount++;
                        log.info("Cleaning expired skill directory: {}", skillDir);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to clean skill directory: {}", e.getMessage(), e);
        }

        if (cleanedCount > 0) {
            log.info("Cleanup completed: cleaned {} expired skill directories", cleanedCount);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete file: {}", p);
                        }
                    });
        }
    }

    /**
     * 获取 Skill 缓存目录
     */
    public String getSkillCacheDir() {
        return skillCacheDir;
    }

    /**
     * 获取 Skill 文件路径
     */
    public String getSkillFilePath() {
        return skillFilePath;
    }
}
