/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.controller;

import com.huawei.cloudopenlabs.workflow.dto.*;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowDataRequest;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowResponse;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowUpdateRequest;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowStatus;
import com.huawei.cloudopenlabs.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流管理控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * 创建工作流
     *
     * @param request 创建请求
     * @return 工作流响应
     */
    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(@Valid @RequestBody WorkflowCreateRequest request) {
        log.info("创建工作流: {}", request.getName());
        WorkflowResponse response = workflowService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取默认工作流详情
     *
     * @return 默认工作流响应
     */
    @GetMapping("/default")
    public ResponseEntity<WorkflowResponse> getDefaultWorkflow() {
        WorkflowResponse response = workflowService.getDefaultWorkflow();
        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作流列表
     *
     * @param page 页码（前端从1开始，后端自动转换为0开始）
     * @param size 每页大小
     * @param sort 排序字段
     * @return 工作流分页列表
     */
    @GetMapping("/list")
    public ResponseEntity<Page<WorkflowResponse>> getWorkflowList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        // 前端页码从1开始，Spring Data 页码从0开始，需要减1
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(sortDirection, sort));
        Page<WorkflowResponse> response = workflowService.getWorkflowList(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作流详情
     *
     * @param id 工作流ID
     * @return 工作流响应
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflow(
            @PathVariable String id) {
        WorkflowResponse response = workflowService.getWorkflowById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据状态获取工作流列表
     *
     * @param status    状态
     * @param page      页码
     * @param size      每页大小
     * @return 工作流分页列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<WorkflowResponse>> getWorkflowListByStatus(
            @PathVariable WorkflowStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkflowResponse> response = workflowService.getWorkflowListByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索工作流
     *
     * @param name 名称关键字
     * @param page 页码
     * @param size 每页大小
     * @return 工作流分页列表
     */
    @GetMapping("/search")
    public ResponseEntity<Page<WorkflowResponse>> searchWorkflows(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkflowResponse> response = workflowService.searchWorkflows(name, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新工作流
     *
     * @param id      工作流ID
     * @param request 更新请求
     * @return 工作流响应
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable String id,
            @Valid @RequestBody WorkflowUpdateRequest request) {
        log.info("更新工作流: {}", id);
        WorkflowResponse response = workflowService.updateWorkflow(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除工作流
     *
     * @param id 工作流ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable String id) {
        log.info("删除工作流: {}", id);
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布工作流
     *
     * @param id 工作流ID
     * @return 工作流响应
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<WorkflowResponse> publishWorkflow(
            @PathVariable String id) {
        log.info("发布工作流: {}", id);
        WorkflowResponse response = workflowService.publishWorkflow(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 取消发布工作流
     *
     * @param id 工作流ID
     * @return 工作流响应
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<WorkflowResponse> unpublishWorkflow(
            @PathVariable String id) {
        log.info("取消发布工作流: {}", id);
        WorkflowResponse response = workflowService.unpublishWorkflow(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 复制工作流
     *
     * @param id 工作流ID
     * @return 新工作流响应
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<WorkflowResponse> copyWorkflow(
            @PathVariable String id) {
        log.info("复制工作流: {}", id);
        WorkflowResponse response = workflowService.copyWorkflow(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 保存工作流完整数据
     *
     * @param id          工作流ID
     * @param nodes       节点列表
     * @param connections 连线列表
     * @param associations 关联列表
     * @return 工作流响应
     */
    @PostMapping("/{id}/data")
    public ResponseEntity<WorkflowResponse> saveWorkflowData(
            @PathVariable String id,
            @RequestParam(required = false) List<WorkflowResponse.NodeDTO> nodes,
            @RequestParam(required = false) List<WorkflowResponse.ConnectionDTO> connections,
            @RequestParam(required = false) List<WorkflowResponse.AssociationDTO> associations) {
        log.info("保存工作流数据: {}", id);
        if (nodes == null) nodes = List.of();
        if (connections == null) connections = List.of();
        if (associations == null) associations = List.of();
        WorkflowResponse response = workflowService.saveWorkflowData(id, nodes, connections, associations);
        return ResponseEntity.ok(response);
    }

    /**
     * 保存工作流完整数据（JSON请求体）
     *
     * @param id      工作流ID
     * @param request 工作流数据请求
     * @return 工作流响应
     */
    @PostMapping("/{id}/data/json")
    public ResponseEntity<WorkflowResponse> saveWorkflowDataJson(
            @PathVariable String id,
            @Valid @RequestBody WorkflowDataRequest request) {
        log.info("保存工作流数据(JSON): {}", id);
        WorkflowResponse response = workflowService.saveWorkflowData(
                id,
                request.getNodes(),
                request.getConnections(),
                request.getAssociations()
        );
        return ResponseEntity.ok(response);
    }
}
