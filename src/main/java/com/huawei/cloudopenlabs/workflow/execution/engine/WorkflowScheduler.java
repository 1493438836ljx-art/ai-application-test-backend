/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.engine;

import com.huawei.cloudopenlabs.workflow.entity.ExecutionStatus;
import com.huawei.cloudopenlabs.workflow.entity.TriggerType;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowConnectionEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowExecutionEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.execution.context.ExecutionContext;
import com.huawei.cloudopenlabs.workflow.execution.context.WorkflowDefinition;
import com.huawei.cloudopenlabs.workflow.execution.error.ErrorCode;
import com.huawei.cloudopenlabs.workflow.execution.error.WorkflowExecutionException;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowConnectionMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowExecutionMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowNodeMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流执行调度器
 * 负责接收执行请求并调度执行资源
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowScheduler {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowConnectionMapper connectionMapper;
    private final WorkflowExecutionMapper executionMapper;
    private final ExecutionEngine executionEngine;
    private final ObjectMapper objectMapper;

    @Qualifier("workflowExecutor")
    private final ThreadPoolTaskExecutor workflowExecutor;

    /**
     * 提交工作流执行请求
     *
     * @param workflowId  工作流ID
     * @param inputData   输入参数
     * @param triggeredBy 触发人
     * @param triggerType 触发类型
     * @return 执行UUID
     */
    @Transactional
    public String submitExecution(String workflowId,
                                   Map<String, Object> inputData,
                                   String triggeredBy,
                                   String triggerType) {

        log.info("提交工作流执行请求: workflowId={}, triggeredBy={}", workflowId, triggeredBy);

        // 1. 查询工作流定义
        WorkflowEntity workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new WorkflowExecutionException(
                    ErrorCode.WORKFLOW_NOT_FOUND,
                    "工作流不存在: " + workflowId
            );
        }

        // 2. 检查工作流状态（允许 DRAFT 和 PUBLISHED 状态执行，用于编辑器中试运行）
        if ("ARCHIVED".equals(workflow.getStatus())) {
            throw new WorkflowExecutionException(
                    ErrorCode.WORKFLOW_NOT_PUBLISHED,
                    "工作流已归档，无法执行: " + workflow.getName()
            );
        }

        // 3. 创建执行记录
        String executionUuid = UUID.randomUUID().toString();
        WorkflowExecutionEntity execution = new WorkflowExecutionEntity();
        execution.setWorkflowId(workflowId);
        execution.setExecutionUuid(executionUuid);
        execution.setStatus(ExecutionStatus.PENDING.name());
        execution.setTriggerType(triggerType != null ? triggerType : TriggerType.MANUAL.name());
        execution.setTriggeredBy(triggeredBy);
        execution.setProgress(0);

        if (inputData != null) {
            try {
                execution.setInputData(objectMapper.writeValueAsString(inputData));
            } catch (JsonProcessingException e) {
                log.error("序列化输入数据失败", e);
            }
        }

        executionMapper.insert(execution);

        // 4. 查询节点和连线
        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);

        // 5. 构建工作流定义
        WorkflowDefinition definition = WorkflowDefinition.from(workflow, nodes, connections);

        // 6. 创建执行上下文
        ExecutionContext context = ExecutionContext.builder()
                .executionId(execution.getId())
                .executionUuid(executionUuid)
                .workflowId(workflowId)
                .triggeredBy(triggeredBy)
                .inputData(inputData != null ? inputData : new HashMap<>())
                .definition(definition)
                .build();

        // 7. 提交异步执行任务
        workflowExecutor.submit(() -> executeWorkflowAsync(context, workflow));

        log.info("工作流执行已提交: workflowId={}, executionUuid={}", workflowId, executionUuid);

        return executionUuid;
    }

    /**
     * 异步执行工作流
     */
    private void executeWorkflowAsync(ExecutionContext context, WorkflowEntity workflow) {
        String executionUuid = context.getExecutionUuid();
        String executionId = context.getExecutionId();

        try {
            // 1. 更新状态为 RUNNING
            updateExecutionStatus(executionId, ExecutionStatus.RUNNING);

            // 2. 调用执行引擎执行
            executionEngine.execute(context);

            // 3. 状态已由执行引擎更新

        } catch (WorkflowExecutionException e) {
            log.error("工作流执行失败: executionUuid={}, error={}", executionUuid, e.getMessage());

            // 更新执行状态为失败
            updateExecutionStatus(executionId, ExecutionStatus.FAILED, e.getMessage());

        } catch (Exception e) {
            log.error("工作流执行异常: executionUuid={}", executionUuid, e);

            // 更新执行状态为失败
            updateExecutionStatus(executionId, ExecutionStatus.FAILED, e.getMessage());
        }
    }

    /**
     * 更新执行状态
     */
    private void updateExecutionStatus(String executionId, ExecutionStatus status) {
        updateExecutionStatus(executionId, status, null);
    }

    /**
     * 更新执行状态（带错误信息）
     */
    private void updateExecutionStatus(String executionId, ExecutionStatus status, String errorMessage) {
        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("执行记录不存在: executionId={}", executionId);
            return;
        }

        execution.setStatus(status.name());

        if (errorMessage != null) {
            execution.setErrorMessage(errorMessage);
        }

        if (status == ExecutionStatus.RUNNING && execution.getStartTime() == null) {
            execution.setStartTime(LocalDateTime.now());
        }

        if (status.isTerminal()) {
            execution.setEndTime(LocalDateTime.now());
            if (execution.getStartTime() != null) {
                long duration = java.time.Duration.between(
                        execution.getStartTime(),
                        execution.getEndTime()
                ).toMillis();
                execution.setDurationMs(duration);
            }
        }

        executionMapper.updateById(execution);
    }

    /**
     * 中止执行
     *
     * @param executionId 执行记录ID
     */
    @Transactional
    public void abortExecution(String executionId) {
        log.info("中止执行: executionId={}", executionId);

        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new WorkflowExecutionException(
                    ErrorCode.NODE_NOT_FOUND,
                    "执行记录不存在: " + executionId
            );
        }

        if (ExecutionStatus.RUNNING.name().equals(execution.getStatus())) {
            execution.setStatus(ExecutionStatus.ABORTED.name());
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);

            log.info("执行已中止: executionId={}", executionId);
        } else {
            log.warn("执行状态不是运行中，无法中止: executionId={}, status={}",
                    executionId, execution.getStatus());
        }
    }

    /**
     * 获取执行状态
     *
     * @param executionUuid 执行UUID
     * @return 执行记录
     */
    public WorkflowExecutionEntity getExecution(String executionUuid) {
        return executionMapper.selectByExecutionUuid(executionUuid).orElse(null);
    }

    /**
     * 获取正在执行的工作流列表
     *
     * @return 执行记录列表
     */
    public List<WorkflowExecutionEntity> getRunningExecutions() {
        return executionMapper.selectRunningExecutions();
    }
}
