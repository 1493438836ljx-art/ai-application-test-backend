package com.example.demo.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.workflow.dto.ExecutionResponse;
import com.example.demo.workflow.entity.ExecutionStatus;
import com.example.demo.workflow.entity.TriggerType;
import com.example.demo.workflow.entity.WorkflowEntity;
import com.example.demo.workflow.entity.WorkflowExecutionEntity;
import com.example.demo.workflow.mapper.WorkflowExecutionMapper;
import com.example.demo.workflow.mapper.WorkflowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 工作流执行服务实现类 (MyBatis-Plus版本)
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private final WorkflowExecutionMapper executionMapper;
    private final WorkflowMapper workflowMapper;

    @Override
    @Transactional
    public Long executeWorkflow(Long workflowId, String triggeredBy, String inputData) {
        log.info("开始执行工作流: {}", workflowId);

        WorkflowEntity workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在: " + workflowId);
        }

        // 创建执行记录
        WorkflowExecutionEntity execution = new WorkflowExecutionEntity();
        execution.setWorkflowId(workflowId);
        execution.setExecutionUuid(UUID.randomUUID().toString());
        execution.setStatus(ExecutionStatus.PENDING.name());
        execution.setTriggerType(TriggerType.MANUAL.name());
        execution.setTriggeredBy(triggeredBy);
        execution.setInputData(inputData);

        executionMapper.insert(execution);

        // 更新工作流状态
        workflow.setHasRun(true);
        workflowMapper.updateById(workflow);

        // TODO: 实际执行工作流的逻辑（异步执行）
        // 这里可以集成Agent框架或其他执行引擎

        return execution.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(Long id) {
        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw new RuntimeException("执行记录不存在: " + id);
        }
        return convertToResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionByUuid(String uuid) {
        WorkflowExecutionEntity execution = executionMapper.selectByExecutionUuid(uuid)
                .orElseThrow(() -> new RuntimeException("执行记录不存在: " + uuid));
        return convertToResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ExecutionResponse> getExecutionsByWorkflowId(Long workflowId, Pageable pageable) {
        Page<WorkflowExecutionEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        IPage<WorkflowExecutionEntity> result = executionMapper.selectByWorkflowId(page, workflowId);

        List<ExecutionResponse> content = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, result.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionResponse> getRunningExecutions() {
        return executionMapper.selectRunningExecutions().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void abortExecution(Long id) {
        log.info("中止执行: {}", id);

        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw new RuntimeException("执行记录不存在: " + id);
        }

        if (ExecutionStatus.RUNNING.name().equals(execution.getStatus())) {
            execution.setStatus(ExecutionStatus.ABORTED.name());
            execution.setEndTime(LocalDateTime.now());
            execution.setDurationMs(calculateDuration(execution.getStartTime(), execution.getEndTime()));
            executionMapper.updateById(execution);
        }
    }

    @Override
    @Transactional
    public void updateProgress(Long id, int progress) {
        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw new RuntimeException("执行记录不存在: " + id);
        }

        execution.setProgress(Math.min(100, Math.max(0, progress)));

        // 如果进度为0且状态为PENDING，则开始执行
        if (progress > 0 && ExecutionStatus.PENDING.name().equals(execution.getStatus())) {
            execution.setStatus(ExecutionStatus.RUNNING.name());
            execution.setStartTime(LocalDateTime.now());
        }

        executionMapper.updateById(execution);
    }

    @Override
    @Transactional
    public void completeExecution(Long id, String outputData, String nodeExecutions) {
        log.info("完成执行: {}", id);

        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw new RuntimeException("执行记录不存在: " + id);
        }

        execution.setStatus(ExecutionStatus.SUCCESS.name());
        execution.setOutputData(outputData);
        execution.setNodeExecutions(nodeExecutions);
        execution.setProgress(100);
        execution.setEndTime(LocalDateTime.now());
        execution.setDurationMs(calculateDuration(execution.getStartTime(), execution.getEndTime()));

        executionMapper.updateById(execution);
    }

    @Override
    @Transactional
    public void failExecution(Long id, String errorMessage) {
        log.error("执行失败: {}, 错误: {}", id, errorMessage);

        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw new RuntimeException("执行记录不存在: " + id);
        }

        execution.setStatus(ExecutionStatus.FAILED.name());
        execution.setErrorMessage(errorMessage);
        execution.setEndTime(LocalDateTime.now());
        execution.setDurationMs(calculateDuration(execution.getStartTime(), execution.getEndTime()));

        executionMapper.updateById(execution);
    }

    private ExecutionResponse convertToResponse(WorkflowExecutionEntity execution) {
        ExecutionResponse response = new ExecutionResponse();
        response.setId(execution.getId());
        response.setWorkflowId(execution.getWorkflowId());
        response.setExecutionUuid(execution.getExecutionUuid());
        response.setStatus(ExecutionStatus.valueOf(execution.getStatus()));
        response.setTriggerType(TriggerType.valueOf(execution.getTriggerType()));
        response.setTriggeredBy(execution.getTriggeredBy());
        response.setInputData(execution.getInputData());
        response.setOutputData(execution.getOutputData());
        response.setErrorMessage(execution.getErrorMessage());
        response.setNodeExecutions(execution.getNodeExecutions());
        response.setProgress(execution.getProgress());
        response.setStartTime(execution.getStartTime());
        response.setEndTime(execution.getEndTime());
        response.setDurationMs(execution.getDurationMs());
        response.setCreatedAt(execution.getCreatedAt());
        response.setUpdatedAt(execution.getUpdatedAt());
        return response;
    }

    private Long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return java.time.Duration.between(start, end).toMillis();
    }
}
