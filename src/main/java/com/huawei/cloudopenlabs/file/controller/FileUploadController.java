package com.huawei.cloudopenlabs.file.controller;

import com.huawei.cloudopenlabs.file.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 * 提供通用的文件上传、下载功能
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传下载接口")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件信息（包含文件ID和访问路径）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传单个文件，返回文件ID和访问路径")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        log.info("上传文件: {}", file.getOriginalFilename());

        String fileId = fileUploadService.uploadFile(file);
        String filePath = fileUploadService.getFilePath(fileId);
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();
        String contentType = file.getContentType();

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", originalFilename);
        result.put("filePath", filePath);
        result.put("fileSize", fileSize);
        result.put("contentType", contentType);

        return ResponseEntity.ok(result);
    }

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件资源
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件", description = "根据文件ID下载文件")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileId) {
        log.info("下载文件: {}", fileId);

        Resource resource = fileUploadService.loadFileAsResource(fileId);
        String filename = fileUploadService.getOriginalFilename(fileId);
        String contentType = fileUploadService.getContentType(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * 获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    @GetMapping("/info/{fileId}")
    @Operation(summary = "获取文件信息", description = "根据文件ID获取文件信息")
    public ResponseEntity<Map<String, Object>> getFileInfo(
            @PathVariable String fileId) {
        log.info("获取文件信息: {}", fileId);

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", fileUploadService.getOriginalFilename(fileId));
        result.put("filePath", fileUploadService.getFilePath(fileId));
        result.put("contentType", fileUploadService.getContentType(fileId));

        return ResponseEntity.ok(result);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件", description = "根据文件ID删除文件")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable String fileId) {
        log.info("删除文件: {}", fileId);

        fileUploadService.deleteFile(fileId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "文件删除成功");

        return ResponseEntity.ok(result);
    }
}
