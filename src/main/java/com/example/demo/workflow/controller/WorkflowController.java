package com.example.demo.workflow.controller;

import com.example.demo.workflow.dto.*;
import com.example.demo.workflow.entity.WorkflowStatus;
import com.example.demo.workflow.service.WorkflowService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流管理控制器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流管理", description = "工作流的增删改查接口")
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * 创建工作流
     *
     * @param request 创建请求
     * @return 工作流响应
     */
    @PostMapping
    @Operation(summary = "创建工作流", description = "创建一个新的工作流")
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
    @Operation(summary = "获取默认工作流详情", description = "获取系统默认工作流的详细信息，包括节点、连线和关联")
    public ResponseEntity<WorkflowResponse> getDefaultWorkflow() {
        WorkflowResponse response = workflowService.getDefaultWorkflow();
        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作流详情
     *
     * @param id 工作流ID
     * @return 工作流响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取工作流详情", description = "根据ID获取工作流的详细信息，包括节点、连线和关联")
    public ResponseEntity<WorkflowResponse> getWorkflow(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id) {
        WorkflowResponse response = workflowService.getWorkflowById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作流列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param sort 排序字段
     * @return 工作流分页列表
     */
    @GetMapping
    @Operation(summary = "获取工作流列表", description = "分页获取工作流列表")
    public ResponseEntity<Page<WorkflowResponse>> getWorkflowList(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "排序方向", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<WorkflowResponse> response = workflowService.getWorkflowList(pageable);
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
    @Operation(summary = "根据状态获取工作流", description = "根据状态分页获取工作流列表")
    public ResponseEntity<Page<WorkflowResponse>> getWorkflowListByStatus(
            @Parameter(description = "工作流状态", required = true)
            @PathVariable WorkflowStatus status,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
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
    @Operation(summary = "搜索工作流", description = "根据名称关键字搜索工作流")
    public ResponseEntity<Page<WorkflowResponse>> searchWorkflows(
            @Parameter(description = "名称关键字", required = true)
            @RequestParam String name,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
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
    @Operation(summary = "更新工作流", description = "更新工作流的基本信息")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id,
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
    @Operation(summary = "删除工作流", description = "删除指定的工作流（逻辑删除）")
    public ResponseEntity<Void> deleteWorkflow(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id) {
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
    @Operation(summary = "发布工作流", description = "将工作流状态设置为已发布")
    public ResponseEntity<WorkflowResponse> publishWorkflow(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id) {
        log.info("发布工作流: {}", id);
        WorkflowResponse response = workflowService.publishWorkflow(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 复制工作流
     *
     * @param id 工作流ID
     * @return 新工作流响应
     */
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制工作流", description = "复制一个工作流及其所有节点、连线和关联")
    public ResponseEntity<WorkflowResponse> copyWorkflow(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id) {
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
    @Operation(summary = "保存工作流数据", description = "保存工作流的节点、连线和关联数据")
    public ResponseEntity<WorkflowResponse> saveWorkflowData(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "节点列表")
            @RequestParam(required = false) List<WorkflowResponse.NodeDTO> nodes,
            @Parameter(description = "连线列表")
            @RequestParam(required = false) List<WorkflowResponse.ConnectionDTO> connections,
            @Parameter(description = "关联列表")
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
    @Operation(summary = "保存工作流数据（JSON）", description = "通过JSON请求体保存工作流的节点、连线和关联数据")
    public ResponseEntity<WorkflowResponse> saveWorkflowDataJson(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id,
            @RequestBody WorkflowDataRequest request) {
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
