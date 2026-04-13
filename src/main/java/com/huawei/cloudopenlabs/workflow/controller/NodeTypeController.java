/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.controller;

import com.huawei.cloudopenlabs.workflow.dto.NodeTypeCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.NodeTypeResponse;
import com.huawei.cloudopenlabs.workflow.dto.NodeTypeUpdateRequest;
import com.huawei.cloudopenlabs.workflow.service.NodeTypeService;
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
 * 节点类型管理控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/node-types")
@RequiredArgsConstructor
public class NodeTypeController {

    private final NodeTypeService nodeTypeService;

    /**
     * 创建节点类型
     *
     * @param request 创建请求
     * @return 节点类型响应
     */
    @PostMapping
    public ResponseEntity<NodeTypeResponse> createNodeType(@Valid @RequestBody NodeTypeCreateRequest request) {
        log.info("创建节点类型: {}", request.getCode());
        NodeTypeResponse response = nodeTypeService.createNodeType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取所有启用的节点类型
     *
     * @return 节点类型列表
     */
    @GetMapping
    public ResponseEntity<List<NodeTypeResponse>> getAllNodeTypes() {
        List<NodeTypeResponse> nodeTypes = nodeTypeService.getAllEnabledNodeTypes();
        return ResponseEntity.ok(nodeTypes);
    }

    /**
     * 分页获取节点类型
     *
     * @param page 页码
     * @param size 每页大小
     * @return 节点类型分页列表
     */
    @GetMapping("/page")
    public ResponseEntity<Page<NodeTypeResponse>> getNodeTypeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sortOrder").ascending());
        Page<NodeTypeResponse> response = nodeTypeService.getNodeTypeList(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取节点类型
     *
     * @param id 节点类型ID
     * @return 节点类型
     */
    @GetMapping("/{id}")
    public ResponseEntity<NodeTypeResponse> getNodeTypeById(
            @PathVariable String id) {
        NodeTypeResponse response = nodeTypeService.getNodeTypeById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据编码获取节点类型
     *
     * @param code 节点类型编码
     * @return 节点类型
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<NodeTypeResponse> getNodeTypeByCode(
            @PathVariable String code) {
        NodeTypeResponse response = nodeTypeService.getNodeTypeByCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据分类获取节点类型
     *
     * @param category 分类
     * @return 节点类型列表
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<NodeTypeResponse>> getNodeTypesByCategory(
            @PathVariable String category) {
        List<NodeTypeResponse> nodeTypes = nodeTypeService.getNodeTypesByCategory(category);
        return ResponseEntity.ok(nodeTypes);
    }

    /**
     * 更新节点类型
     *
     * @param id      节点类型ID
     * @param request 更新请求
     * @return 节点类型响应
     */
    @PutMapping("/{id}")
    public ResponseEntity<NodeTypeResponse> updateNodeType(
            @PathVariable String id,
            @Valid @RequestBody NodeTypeUpdateRequest request) {
        log.info("更新节点类型: {}", id);
        NodeTypeResponse response = nodeTypeService.updateNodeType(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除节点类型
     *
     * @param id 节点类型ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNodeType(
            @PathVariable String id) {
        log.info("删除节点类型: {}", id);
        nodeTypeService.deleteNodeType(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用节点类型
     *
     * @param id 节点类型ID
     * @return 节点类型响应
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<NodeTypeResponse> enableNodeType(
            @PathVariable String id) {
        log.info("启用节点类型: {}", id);
        NodeTypeResponse response = nodeTypeService.toggleNodeType(id, true);
        return ResponseEntity.ok(response);
    }

    /**
     * 禁用节点类型
     *
     * @param id 节点类型ID
     * @return 节点类型响应
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<NodeTypeResponse> disableNodeType(
            @PathVariable String id) {
        log.info("禁用节点类型: {}", id);
        NodeTypeResponse response = nodeTypeService.toggleNodeType(id, false);
        return ResponseEntity.ok(response);
    }
}
