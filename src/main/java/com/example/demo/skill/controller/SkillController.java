package com.example.demo.skill.controller;

import com.example.demo.skill.dto.*;
import com.example.demo.skill.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Skill管理控制器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/skill")
@RequiredArgsConstructor
@Tag(name = "Skill管理", description = "Skill的增删改查接口")
public class SkillController {

    private final SkillService skillService;

    /**
     * 创建Skill（支持文件上传）
     *
     * @param file    执行套件文件
     * @param request 创建请求
     * @return Skill响应
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "创建Skill", description = "创建一个新的Skill，支持上传执行套件文件")
    public ResponseEntity<SkillResponse> createSkill(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Valid @RequestPart("data") SkillCreateRequest request) {
        log.info("创建Skill: {}", request.getName());
        SkillResponse response = skillService.createSkill(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取Skill列表
     *
     * @param page      页码（前端从1开始）
     * @param size      每页大小
     * @param sort      排序字段
     * @param direction 排序方向
     * @return Skill分页列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取Skill列表", description = "分页获取Skill列表")
    public ResponseEntity<Page<SkillResponse>> getSkillList(
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "排序方向", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(sortDirection, sort));
        Page<SkillResponse> response = skillService.getSkillList(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索Skill
     *
     * @param name         名称关键字
     * @param executionType 执行方式
     * @param category     分类
     * @param accessType   访问控制类型
     * @param status       状态
     * @param createdBy    创建人
     * @param isContainer  是否容器
     * @param page         页码
     * @param size         每页大小
     * @return Skill分页列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索Skill", description = "根据条件搜索Skill")
    public ResponseEntity<Page<SkillResponse>> searchSkills(
            @Parameter(description = "名称关键字")
            @RequestParam(required = false) String name,
            @Parameter(description = "执行方式：AUTOMATED/AI")
            @RequestParam(required = false) String executionType,
            @Parameter(description = "分类：SYSTEM/USER")
            @RequestParam(required = false) String category,
            @Parameter(description = "访问控制类型：PUBLIC/PRIVATE/WHITELIST/PROJECT")
            @RequestParam(required = false) String accessType,
            @Parameter(description = "状态：PUBLISHED/DRAFT")
            @RequestParam(required = false) String status,
            @Parameter(description = "创建人")
            @RequestParam(required = false) String createdBy,
            @Parameter(description = "是否容器")
            @RequestParam(required = false) Boolean isContainer,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        SkillQueryRequest query = new SkillQueryRequest();
        query.setName(name);
        query.setExecutionType(executionType);
        query.setCategory(category);
        query.setAccessType(accessType);
        query.setStatus(status);
        query.setCreatedBy(createdBy);
        query.setIsContainer(isContainer);

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SkillResponse> response = skillService.searchSkills(query, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取Skill详情
     *
     * @param id Skill ID
     * @return Skill响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取Skill详情", description = "根据ID获取Skill的详细信息，包括入参、出参和访问控制")
    public ResponseEntity<SkillResponse> getSkill(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String id) {
        SkillResponse response = skillService.getSkillById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新Skill（支持文件上传）
     *
     * @param id      Skill ID
     * @param file    执行套件文件（可选）
     * @param request 更新请求
     * @return Skill响应
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "更新Skill", description = "更新Skill的基本信息，支持上传新的执行套件文件")
    public ResponseEntity<SkillResponse> updateSkill(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Valid @RequestPart("data") SkillUpdateRequest request) {
        log.info("更新Skill: {}", id);
        SkillResponse response = skillService.updateSkill(id, request, file);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除Skill
     *
     * @param id Skill ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除Skill", description = "删除指定的Skill（逻辑删除）")
    public ResponseEntity<Void> deleteSkill(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String id) {
        log.info("删除Skill: {}", id);
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布Skill
     *
     * @param id Skill ID
     * @return Skill响应
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布Skill", description = "将Skill状态设置为已发布")
    public ResponseEntity<SkillResponse> publishSkill(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String id) {
        log.info("发布Skill: {}", id);
        SkillResponse response = skillService.publishSkill(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 取消发布Skill
     *
     * @param id Skill ID
     * @return Skill响应
     */
    @PostMapping("/{id}/unpublish")
    @Operation(summary = "取消发布Skill", description = "将Skill状态设置为草稿")
    public ResponseEntity<SkillResponse> unpublishSkill(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String id) {
        log.info("取消发布Skill: {}", id);
        SkillResponse response = skillService.unpublishSkill(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 复制Skill
     *
     * @param id Skill ID
     * @return 新Skill响应
     */
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制Skill", description = "复制一个Skill及其所有参数和访问控制")
    public ResponseEntity<SkillResponse> copySkill(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String id) {
        log.info("复制Skill: {}", id);
        SkillResponse response = skillService.copySkill(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 下载执行套件
     *
     * @param id Skill ID
     * @return 执行套件文件
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载执行套件", description = "下载Skill的执行套件文件")
    public ResponseEntity<org.springframework.core.io.Resource> downloadSuite(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String id) {
        log.info("下载执行套件: {}", id);
        return skillService.downloadSuite(id);
    }
}
