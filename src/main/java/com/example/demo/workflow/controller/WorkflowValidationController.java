package com.example.demo.workflow.controller;

import com.example.demo.workflow.dto.AvailableVariable;
import com.example.demo.workflow.dto.NodeResponse;
import com.example.demo.workflow.dto.ValidationResult;
import com.example.demo.workflow.service.WorkflowValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流验证控制器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/{workflowId}")
@RequiredArgsConstructor
@Tag(name = "工作流验证", description = "工作流验证相关接口")
public class WorkflowValidationController {

    private final WorkflowValidationService validationService;

    /**
     * 验证工作流
     *
     * @param workflowId 工作流ID
     * @return 验证结果
     */
    @PostMapping("/validate")
    @Operation(summary = "验证工作流", description = "验证工作流的结构、参数引用、循环依赖等")
    public ResponseEntity<ValidationResult> validateWorkflow(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId) {
        log.info("验证工作流: workflowId={}", workflowId);
        ValidationResult result = validationService.validate(workflowId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取节点的前置节点列表
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 前置节点列表
     */
    @GetMapping("/predecessors/{nodeUuid}")
    @Operation(summary = "获取前置节点", description = "获取指定节点的所有前置节点（直接和间接）")
    public ResponseEntity<List<NodeResponse>> getPredecessors(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "节点UUID", required = true)
            @PathVariable String nodeUuid) {
        log.info("获取前置节点: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
        List<NodeResponse> predecessors = validationService.getPredecessors(workflowId, nodeUuid);
        return ResponseEntity.ok(predecessors);
    }

    /**
     * 获取节点可引用的变量列表
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 可用变量列表
     */
    @GetMapping("/available-variables/{nodeUuid}")
    @Operation(summary = "获取可用变量", description = "获取指定节点可以引用的所有变量（来自前置节点的输出参数）")
    public ResponseEntity<List<AvailableVariable>> getAvailableVariables(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "节点UUID", required = true)
            @PathVariable String nodeUuid) {
        log.info("获取可用变量: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
        List<AvailableVariable> variables = validationService.getAvailableVariables(workflowId, nodeUuid);
        return ResponseEntity.ok(variables);
    }

    /**
     * 检查参数引用是否有效
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @param reference  参数引用表达式（如：${节点名称.参数名}）
     * @return 是否有效
     */
    @GetMapping("/check-reference/{nodeUuid}")
    @Operation(summary = "检查参数引用", description = "检查指定的参数引用表达式是否有效")
    public ResponseEntity<ReferenceCheckResult> checkReference(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "节点UUID", required = true)
            @PathVariable String nodeUuid,
            @Parameter(description = "参数引用表达式", required = true)
            @RequestParam String reference) {
        log.info("检查参数引用: workflowId={}, nodeUuid={}, reference={}", workflowId, nodeUuid, reference);
        ReferenceCheckResult result = validationService.checkReference(workflowId, nodeUuid, reference);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取工作流的拓扑排序
     *
     * @param workflowId 工作流ID
     * @return 节点执行顺序（拓扑排序）
     */
    @GetMapping("/execution-order")
    @Operation(summary = "获取执行顺序", description = "获取工作流节点的执行顺序（拓扑排序）")
    public ResponseEntity<List<NodeResponse>> getExecutionOrder(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId) {
        log.info("获取执行顺序: workflowId={}", workflowId);
        List<NodeResponse> order = validationService.getExecutionOrder(workflowId);
        return ResponseEntity.ok(order);
    }

    /**
     * 引用检查结果
     */
    @lombok.Data
    public static class ReferenceCheckResult {
        private boolean valid;
        private String message;
        private String sourceNodeUuid;
        private String sourceNodeName;
        private String paramName;
        private String paramType;
    }
}
