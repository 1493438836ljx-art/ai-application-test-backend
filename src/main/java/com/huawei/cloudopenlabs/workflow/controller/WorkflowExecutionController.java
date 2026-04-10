/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.controller;

import com.huawei.cloudopenlabs.workflow.dto.ExecutionOutputResponse;
import com.huawei.cloudopenlabs.workflow.dto.ExecutionResponse;
import com.huawei.cloudopenlabs.workflow.service.WorkflowExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流执行控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowExecutionController {

    private final WorkflowExecutionService executionService;

    /**
     * 执行工作流
     *
     * @param id          工作流ID
     * @param triggeredBy 触发人
     * @param inputData   输入数据（JSON格式）
     * @return 执行记录ID
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<String> executeWorkflow(
            @PathVariable String id,
            @RequestParam(required = false) String triggeredBy,
            @RequestBody(required = false) String inputData) {
        log.info("执行工作流: {}", id);
        String executionId = executionService.executeWorkflow(id, triggeredBy, inputData);
        return ResponseEntity.ok(executionId);
    }

    /**
     * 获取执行记录
     *
     * @param id 执行记录ID
     * @return 执行响应
     */
    @GetMapping("/execution/{id}")
    public ResponseEntity<ExecutionResponse> getExecution(
            @PathVariable String id) {
        ExecutionResponse response = executionService.getExecution(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据UUID获取执行记录
     *
     * @param uuid 执行UUID
     * @return 执行响应
     */
    @GetMapping("/execution/uuid/{uuid}")
    public ResponseEntity<ExecutionResponse> getExecutionByUuid(
            @PathVariable String uuid) {
        ExecutionResponse response = executionService.getExecutionByUuid(uuid);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作流的执行记录列表
     *
     * @param workflowId 工作流ID
     * @param page       页码
     * @param size       每页大小
     * @return 执行记录分页列表
     */
    @GetMapping("/{workflowId}/executions")
    public ResponseEntity<Page<ExecutionResponse>> getWorkflowExecutions(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ExecutionResponse> response = executionService.getExecutionsByWorkflowId(workflowId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取正在执行的记录
     *
     * @return 执行记录列表
     */
    @GetMapping("/executions/running")
    public ResponseEntity<List<ExecutionResponse>> getRunningExecutions() {
        List<ExecutionResponse> response = executionService.getRunningExecutions();
        return ResponseEntity.ok(response);
    }

    /**
     * 中止执行
     *
     * @param id 执行记录ID
     * @return 无内容响应
     */
    @PostMapping("/execution/{id}/abort")
    public ResponseEntity<Void> abortExecution(
            @PathVariable String id) {
        log.info("中止执行: {}", id);
        executionService.abortExecution(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取执行输出参数
     *
     * @param id 执行记录ID
     * @return 输出参数响应
     */
    @GetMapping("/execution/{id}/outputs")
    public ResponseEntity<ExecutionOutputResponse> getExecutionOutputs(
            @PathVariable String id) {
        ExecutionOutputResponse response = executionService.getExecutionOutputs(id);
        return ResponseEntity.ok(response);
    }
}
