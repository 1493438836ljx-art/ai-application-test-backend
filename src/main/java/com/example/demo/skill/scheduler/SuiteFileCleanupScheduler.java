package com.example.demo.skill.scheduler;

import com.example.demo.skill.entity.SkillEntity;
import com.example.demo.skill.mapper.SkillMapper;
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
 * @author AI Test Platform Team
 * @version 1.0.0
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
        log.info("开始执行执行套件文件清理任务...");

        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir) || !Files.isDirectory(uploadDir)) {
                log.info("执行套件目录不存在或不是目录: {}", uploadPath);
                return;
            }

            // 1. 获取数据库中所有有效的执行套件路径
            Set<String> referencedFiles = getReferencedSuiteFiles();
            log.info("数据库中引用的执行套件文件数量: {}", referencedFiles.size());

            // 2. 获取文件系统中的所有文件
            Set<String> existingFiles = getExistingSuiteFiles(uploadDir);
            log.info("文件系统中执行套件文件数量: {}", existingFiles.size());

            // 3. 找出孤儿文件（文件系统存在但数据库未引用）
            Set<String> orphanedFiles = new HashSet<>(existingFiles);
            orphanedFiles.removeAll(referencedFiles);
            log.info("发现孤儿文件数量: {}", orphanedFiles.size());

            // 4. 删除孤儿文件
            int deletedCount = 0;
            for (String orphanedFile : orphanedFiles) {
                if (deleteFile(orphanedFile)) {
                    deletedCount++;
                }
            }

            log.info("执行套件文件清理任务完成，共删除 {} 个孤儿文件", deletedCount);

        } catch (Exception e) {
            log.error("执行套件文件清理任务失败", e);
        }
    }

    /**
     * 获取数据库中所有有效的执行套件文件路径（只取文件名）
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
     */
    private Set<String> getExistingSuiteFiles(Path uploadDir) {
        try (Stream<Path> paths = Files.list(uploadDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("读取执行套件目录失败: {}", uploadDir, e);
            return Set.of();
        }
    }

    /**
     * 从完整路径中提取文件名
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
     */
    private boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadPath, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("已删除孤儿文件: {}", fileName);
                return true;
            }
        } catch (IOException e) {
            log.warn("删除文件失败: {}", fileName, e);
        }
        return false;
    }
}
