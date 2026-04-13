/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传服务
 * 提供文件存储、加载和删除功能
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Service
public class FileUploadService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // 文件元数据缓存（文件ID -> 文件信息）
    private final Map<String, FileInfo> fileInfoCache = new ConcurrentHashMap<>();

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 文件ID
     */
    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            // 生成文件ID
            String fileId = generateFileId();

            // 按日期创建子目录
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path uploadPath = Paths.get(uploadDir, dateDir).toAbsolutePath().normalize();

            // 创建目录（如果不存在）
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成存储文件名（保留原始扩展名）
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = fileId + extension;

            // 保存文件
            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 缓存文件信息
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setOriginalFilename(originalFilename);
            fileInfo.setStoredPath(targetPath.toString());
            fileInfo.setContentType(file.getContentType());
            fileInfo.setFileSize(file.getSize());
            fileInfoCache.put(fileId, fileInfo);

            return fileId;
        } catch (IOException e) {
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * 加载文件为资源
     *
     * @param fileId 文件ID
     * @return 文件资源
     */
    public Resource loadFileAsResource(String fileId) {
        FileInfo fileInfo = fileInfoCache.get(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在: " + fileId);
        }

        try {
            Path filePath = Paths.get(fileInfo.getStoredPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new IllegalArgumentException("文件不存在: " + fileId);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("文件路径无效: " + fileId, e);
        }
    }

    /**
     * 获取文件路径
     *
     * @param fileId 文件ID
     * @return 文件访问路径
     */
    public String getFilePath(String fileId) {
        return "/api/file/download/" + fileId;
    }

    /**
     * 获取原始文件名
     *
     * @param fileId 文件ID
     * @return 原始文件名
     */
    public String getOriginalFilename(String fileId) {
        FileInfo fileInfo = fileInfoCache.get(fileId);
        return fileInfo != null ? fileInfo.getOriginalFilename() : fileId;
    }

    /**
     * 获取文件内容类型
     *
     * @param fileId 文件ID
     * @return 内容类型
     */
    public String getContentType(String fileId) {
        FileInfo fileInfo = fileInfoCache.get(fileId);
        return fileInfo != null ? fileInfo.getContentType() : "application/octet-stream";
    }

    /**
     * 获取文件大小
     *
     * @param fileId 文件ID
     * @return 文件大小（字节）
     */
    public long getFileSize(String fileId) {
        FileInfo fileInfo = fileInfoCache.get(fileId);
        return fileInfo != null ? fileInfo.getFileSize() : 0L;
    }

    /**
     * 检查文件是否存在
     *
     * @param fileId 文件ID
     * @return 是否存在
     */
    public boolean fileExists(String fileId) {
        return fileInfoCache.containsKey(fileId);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     */
    public void deleteFile(String fileId) {
        FileInfo fileInfo = fileInfoCache.remove(fileId);
        if (fileInfo == null) {
            return;
        }

        try {
            Path filePath = Paths.get(fileInfo.getStoredPath()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 忽略删除失败
        }
    }

    /**
     * 生成文件ID
     */
    private String generateFileId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 文件信息内部类
     */
    private static class FileInfo {
        private String fileId;
        private String originalFilename;
        private String storedPath;
        private String contentType;
        private long fileSize;

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getStoredPath() {
            return storedPath;
        }

        public void setStoredPath(String storedPath) {
            this.storedPath = storedPath;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
    }
}
