package com.example.demo.workflow.controller;

import com.example.demo.workflow.dto.NodeTypeCreateRequest;
import com.example.demo.workflow.dto.NodeTypeResponse;
import com.example.demo.workflow.dto.NodeTypeUpdateRequest;
import com.example.demo.workflow.service.NodeTypeService;
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
 * 节点类型管理控制器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/node-types")
@RequiredArgsConstructor
@Tag(name = "节点类型管理", description = "工作流节点类型的增删改查接口")
public class NodeTypeController {

    private final NodeTypeService nodeTypeService;

    /**
     * 创建节点类型
     *
     * @param request 创建请求
     * @return 节点类型响应
     */
    @PostMapping
    @Operation(summary = "创建节点类型", description = "创建一个新的节点类型")
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
    @Operation(summary = "获取所有节点类型", description = "获取所有启用的节点类型列表")
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
    @Operation(summary = "分页获取节点类型", description = "分页获取节点类型列表（包含禁用的）")
    public ResponseEntity<Page<NodeTypeResponse>> getNodeTypeList(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
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
    @Operation(summary = "获取节点类型详情", description = "根据ID获取节点类型详情")
    public ResponseEntity<NodeTypeResponse> getNodeTypeById(
            @Parameter(description = "节点类型ID", required = true)
            @PathVariable Long id) {
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
    @Operation(summary = "根据编码获取节点类型", description = "根据编码获取节点类型")
    public ResponseEntity<NodeTypeResponse> getNodeTypeByCode(
            @Parameter(description = "节点类型编码", required = true)
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
    @Operation(summary = "根据分类获取节点类型", description = "根据分类获取节点类型列表")
    public ResponseEntity<List<NodeTypeResponse>> getNodeTypesByCategory(
            @Parameter(description = "分类", required = true)
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
    @Operation(summary = "更新节点类型", description = "更新节点类型信息")
    public ResponseEntity<NodeTypeResponse> updateNodeType(
            @Parameter(description = "节点类型ID", required = true)
            @PathVariable Long id,
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
    @Operation(summary = "删除节点类型", description = "删除指定的节点类型")
    public ResponseEntity<Void> deleteNodeType(
            @Parameter(description = "节点类型ID", required = true)
            @PathVariable Long id) {
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
    @Operation(summary = "启用节点类型", description = "启用指定的节点类型")
    public ResponseEntity<NodeTypeResponse> enableNodeType(
            @Parameter(description = "节点类型ID", required = true)
            @PathVariable Long id) {
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
    @Operation(summary = "禁用节点类型", description = "禁用指定的节点类型")
    public ResponseEntity<NodeTypeResponse> disableNodeType(
            @Parameter(description = "节点类型ID", required = true)
            @PathVariable Long id) {
        log.info("禁用节点类型: {}", id);
        NodeTypeResponse response = nodeTypeService.toggleNodeType(id, false);
        return ResponseEntity.ok(response);
    }
}
