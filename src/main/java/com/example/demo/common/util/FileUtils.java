package com.example.demo.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件工具类
 * <p>
 * 提供文件操作相关的通用方法，包括文件扩展名获取、
 * 文件读写、临时文件创建等功能。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
public final class FileUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private FileUtils() {
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名（小写），无扩展名时返回空字符串
     */
    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        // 没有扩展名或点在末尾
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 生成唯一文件名
     * <p>
     * 使用UUID生成唯一标识，保留原始文件扩展名
     * </p>
     *
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    public static String generateUniqueFilename(String originalFilename) {
        String extension = getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }

    /**
     * 读取上传文件的字节数组
     *
     * @param file 上传的文件
     * @return 文件字节数组
     * @throws RuntimeException 读取失败时抛出
     */
    public static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

    /**
     * 读取上传文件内容为字符串
     *
     * @param file 上传的文件
     * @return 文件内容字符串（UTF-8编码）
     * @throws RuntimeException 读取失败时抛出
     */
    public static String readString(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read file as string", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

    /**
     * 读取输入流内容为字符串
     *
     * @param inputStream 输入流
     * @return 内容字符串（UTF-8编码）
     * @throws RuntimeException 读取失败时抛出
     */
    public static String readString(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return builder.toString();
        } catch (IOException e) {
            log.error("Failed to read input stream as string", e);
            throw new RuntimeException("读取输入流失败", e);
        }
    }

    /**
     * 创建临时文件
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件路径
     * @throws RuntimeException 创建失败时抛出
     */
    public static Path createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            log.error("Failed to create temp file", e);
            throw new RuntimeException("创建临时文件失败", e);
        }
    }

    /**
     * 删除文件
     * <p>
     * 文件不存在或删除失败时仅记录警告日志，不抛出异常
     * </p>
     *
     * @param path 文件路径
     */
    public static void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", path, e);
        }
    }

    /**
     * 写入字节数组到文件
     *
     * @param path  文件路径
     * @param bytes 字节数组
     * @throws RuntimeException 写入失败时抛出
     */
    public static void writeBytes(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            log.error("Failed to write bytes to file", e);
            throw new RuntimeException("写入文件失败", e);
        }
    }

    /**
     * 检查文件扩展名是否在允许的扩展名列表中
     *
     * @param filename           文件名
     * @param allowedExtensions  允许的扩展名列表
     * @return 是否为有效扩展名
     */
    public static boolean isValidExtension(String filename, String... allowedExtensions) {
        String extension = getExtension(filename);
        for (String allowed : allowedExtensions) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
