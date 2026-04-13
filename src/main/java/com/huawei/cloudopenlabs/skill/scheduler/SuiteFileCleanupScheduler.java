/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */
package com.huawei.cloudopenlabs.skill.scheduler;

import com.huawei.cloudopenlabs.skill.entity.SkillEntity;
import com.huawei.cloudopenlabs.skill.mapper.SkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 执行套件文件清理定时任务
 * 每天凌晨0点执行，清理数据库中未引用的执行套件文件
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuiteFileCleanupScheduler {

    private final SkillMapper skillMapper;

    @Value("${skill.suite.upload-path:./uploads/suites}")
    private String uploadPath;

    /**
     * 每天凌晨0点执行清理任务
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOrphanedSuiteFiles() {
        log.info("Starting suite file cleanup task...");

        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir) || !Files.isDirectory(uploadDir)) {
                log.info("Suite directory does not exist or is not a directory: {}", uploadPath);
                return;
            }

            // 1. 获取数据库中所有有效的执行套件路径
            Set<String> referencedFiles = getReferencedSuiteFiles();
            log.info("Referenced suite files in database: {}", referencedFiles.size());

            // 2. 获取文件系统中的所有文件
            Set<String> existingFiles = getExistingSuiteFiles(uploadDir);
            log.info("Existing suite files on filesystem: {}", existingFiles.size());

            // 3. 找出孤儿文件（文件系统存在但数据库未引用）
            Set<String> orphanedFiles = new HashSet<>(existingFiles);
            orphanedFiles.removeAll(referencedFiles);
            log.info("Orphaned files found: {}", orphanedFiles.size());

            // 4. 删除孤儿文件
            int deletedCount = 0;
            for (String orphanedFile : orphanedFiles) {
                if (deleteFile(orphanedFile)) {
                    deletedCount++;
                }
            }

            log.info("Suite file cleanup completed, deleted {} orphan files", deletedCount);

        } catch (Exception e) {
            log.error("Suite file cleanup task failed", e);
        }
    }

    /**
     * 获取数据库中所有有效的执行套件文件路径（只取文件名）
     *
     * @return 执行套件文件路径
     */
    private Set<String> getReferencedSuiteFiles() {
        // 查询所有未删除的Skill
        List<SkillEntity> allSkills = skillMapper.selectAllNonDeleted();
        return allSkills.stream()
                .map(SkillEntity::getSuitePath)
                .filter(path -> path != null && !path.isEmpty())
                .map(this::extractFileName)
                .collect(Collectors.toSet());
    }

    /**
     * 获取文件系统中的所有执行套件文件名
     *
     * @param uploadDir 上传目录路径
     * @return 文件名集合
     */
    private Set<String> getExistingSuiteFiles(Path uploadDir) {
        try (Stream<Path> paths = Files.list(uploadDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("Failed to read suite directory: {}", uploadDir, e);
            return Set.of();
        }
    }

    /**
     * 从完整路径中提取文件名
     *
     * @param path 完整文件路径
     * @return 文件名
     */
    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // 处理 Windows 和 Unix 路径分隔符
        path = path.replace("\\", "/");
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 删除成功返回true
     */
    private boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadPath, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted orphan file: {}", fileName);
                return true;
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", fileName, e);
        }
        return false;
    }
}
