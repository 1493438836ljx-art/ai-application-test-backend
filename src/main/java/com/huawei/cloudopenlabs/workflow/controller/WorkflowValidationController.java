/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.controller;

import com.huawei.cloudopenlabs.workflow.dto.AvailableVariable;
import com.huawei.cloudopenlabs.workflow.dto.NodeResponse;
import com.huawei.cloudopenlabs.workflow.dto.ValidationResult;
import com.huawei.cloudopenlabs.workflow.service.WorkflowValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流验证控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/{workflowId}")
@RequiredArgsConstructor
public class WorkflowValidationController {

    private final WorkflowValidationService validationService;

    /**
     * 验证工作流
     *
     * @param workflowId 工作流ID
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateWorkflow(
            @PathVariable String workflowId) {
        log.info("Validating workflow: workflowId={}", workflowId);
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
    public ResponseEntity<List<NodeResponse>> getPredecessors(
            @PathVariable String workflowId,
            @PathVariable String nodeUuid) {
        log.info("Getting prerequisite nodes: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
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
    public ResponseEntity<List<AvailableVariable>> getAvailableVariables(
            @PathVariable String workflowId,
            @PathVariable String nodeUuid) {
        log.info("Getting available variables: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
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
    public ResponseEntity<ReferenceCheckResult> checkReference(
            @PathVariable String workflowId,
            @PathVariable String nodeUuid,
            @RequestParam String reference) {
        log.info("Checking parameter reference: workflowId={}, nodeUuid={}, reference={}", workflowId, nodeUuid, reference);
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
    public ResponseEntity<List<NodeResponse>> getExecutionOrder(
            @PathVariable String workflowId) {
        log.info("Getting execution order: workflowId={}", workflowId);
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
