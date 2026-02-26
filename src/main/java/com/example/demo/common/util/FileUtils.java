package com.example.demo.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
public final class FileUtils {

    private FileUtils() {
    }

    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    public static String generateUniqueFilename(String originalFilename) {
        String extension = getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }

    public static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

    public static String readString(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read file as string", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

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

    public static Path createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            log.error("Failed to create temp file", e);
            throw new RuntimeException("创建临时文件失败", e);
        }
    }

    public static void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", path, e);
        }
    }

    public static void writeBytes(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            log.error("Failed to write bytes to file", e);
            throw new RuntimeException("写入文件失败", e);
        }
    }

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
