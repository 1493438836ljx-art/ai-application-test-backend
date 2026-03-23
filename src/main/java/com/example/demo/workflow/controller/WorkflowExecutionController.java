package com.example.demo.workflow.controller;

import com.example.demo.workflow.dto.ExecutionResponse;
import com.example.demo.workflow.service.WorkflowExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流执行", description = "工作流执行相关接口")
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
    @Operation(summary = "执行工作流", description = "触发工作流的执行")
    public ResponseEntity<Long> executeWorkflow(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "触发人")
            @RequestParam(required = false) String triggeredBy,
            @Parameter(description = "输入数据（JSON格式）")
            @RequestBody(required = false) String inputData) {
        log.info("执行工作流: {}", id);
        Long executionId = executionService.executeWorkflow(id, triggeredBy, inputData);
        return ResponseEntity.ok(executionId);
    }

    /**
     * 获取执行记录
     *
     * @param id 执行记录ID
     * @return 执行响应
     */
    @GetMapping("/execution/{id}")
    @Operation(summary = "获取执行记录", description = "根据ID获取执行记录详情")
    public ResponseEntity<ExecutionResponse> getExecution(
            @Parameter(description = "执行记录ID", required = true)
            @PathVariable Long id) {
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
    @Operation(summary = "根据UUID获取执行记录", description = "根据执行UUID获取执行记录详情")
    public ResponseEntity<ExecutionResponse> getExecutionByUuid(
            @Parameter(description = "执行UUID", required = true)
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
    @Operation(summary = "获取工作流执行记录", description = "获取指定工作流的所有执行记录")
    public ResponseEntity<Page<ExecutionResponse>> getWorkflowExecutions(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
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
    @Operation(summary = "获取正在执行的记录", description = "获取所有正在执行的记录")
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
    @Operation(summary = "中止执行", description = "中止正在执行的工作流")
    public ResponseEntity<Void> abortExecution(
            @Parameter(description = "执行记录ID", required = true)
            @PathVariable Long id) {
        log.info("中止执行: {}", id);
        executionService.abortExecution(id);
        return ResponseEntity.noContent().build();
    }
}
