/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.skill.controller;

import com.huawei.cloudopenlabs.skill.dto.*;
import com.huawei.cloudopenlabs.skill.dto.SkillCreateRequest;
import com.huawei.cloudopenlabs.skill.dto.SkillQueryRequest;
import com.huawei.cloudopenlabs.skill.dto.SkillResponse;
import com.huawei.cloudopenlabs.skill.dto.SkillUpdateRequest;
import com.huawei.cloudopenlabs.skill.service.SkillService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Skill管理控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/skill")
@RequiredArgsConstructor
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
    public ResponseEntity<SkillResponse> createSkill(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Valid @RequestPart("data") SkillCreateRequest request) {
        log.info("Creating skill: {}", request.getName());
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
    public ResponseEntity<Page<SkillResponse>> getSkillList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
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
    public ResponseEntity<Page<SkillResponse>> searchSkills(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String executionType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String accessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Boolean isContainer,
            @RequestParam(defaultValue = "1") int page,
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
    public ResponseEntity<SkillResponse> getSkill(
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
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable String id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Valid @RequestPart("data") SkillUpdateRequest request) {
        log.info("Updating skill: {}", id);
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
    public ResponseEntity<Void> deleteSkill(
            @PathVariable String id) {
        log.info("Deleting skill: {}", id);
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
    public ResponseEntity<SkillResponse> publishSkill(
            @PathVariable String id) {
        log.info("Publishing skill: {}", id);
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
    public ResponseEntity<SkillResponse> unpublishSkill(
            @PathVariable String id) {
        log.info("Unpublishing skill: {}", id);
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
    public ResponseEntity<SkillResponse> copySkill(
            @PathVariable String id) {
        log.info("Copying skill: {}", id);
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
    public ResponseEntity<org.springframework.core.io.Resource> downloadSuite(
            @PathVariable String id) {
        log.info("Downloading execution suite: {}", id);
        return skillService.downloadSuite(id);
    }
}
