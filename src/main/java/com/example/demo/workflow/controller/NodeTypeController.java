package com.example.demo.workflow.controller;

import com.example.demo.workflow.entity.WorkflowNodeTypeEntity;
import com.example.demo.workflow.mapper.WorkflowNodeTypeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Tag(name = "节点类型管理", description = "工作流节点类型的查询接口")
public class NodeTypeController {

    private final WorkflowNodeTypeMapper nodeTypeMapper;

    /**
     * 获取所有启用的节点类型
     *
     * @return 节点类型列表
     */
    @GetMapping
    @Operation(summary = "获取所有节点类型", description = "获取所有启用的节点类型列表")
    public ResponseEntity<List<WorkflowNodeTypeEntity>> getAllNodeTypes() {
        List<WorkflowNodeTypeEntity> nodeTypes = nodeTypeMapper.selectEnabled();
        return ResponseEntity.ok(nodeTypes);
    }

    /**
     * 根据ID获取节点类型
     *
     * @param id 节点类型ID
     * @return 节点类型
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取节点类型详情", description = "根据ID获取节点类型详情")
    public ResponseEntity<WorkflowNodeTypeEntity> getNodeTypeById(
            @Parameter(description = "节点类型ID", required = true)
            @PathVariable Long id) {
        WorkflowNodeTypeEntity nodeType = nodeTypeMapper.selectById(id);
        if (nodeType == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(nodeType);
    }

    /**
     * 根据编码获取节点类型
     *
     * @param code 节点类型编码
     * @return 节点类型
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "根据编码获取节点类型", description = "根据编码获取节点类型")
    public ResponseEntity<WorkflowNodeTypeEntity> getNodeTypeByCode(
            @Parameter(description = "节点类型编码", required = true)
            @PathVariable String code) {
        return nodeTypeMapper.selectByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据分类获取节点类型
     *
     * @param category 分类
     * @return 节点类型列表
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据分类获取节点类型", description = "根据分类获取节点类型列表")
    public ResponseEntity<List<WorkflowNodeTypeEntity>> getNodeTypesByCategory(
            @Parameter(description = "分类", required = true)
            @PathVariable String category) {
        List<WorkflowNodeTypeEntity> nodeTypes = nodeTypeMapper.selectByCategory(category);
        return ResponseEntity.ok(nodeTypes);
    }
}
