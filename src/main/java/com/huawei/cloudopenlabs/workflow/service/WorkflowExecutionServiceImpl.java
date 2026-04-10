/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huawei.cloudopenlabs.common.exception.BusinessException;
import com.huawei.cloudopenlabs.workflow.dto.ExecutionOutputResponse;
import com.huawei.cloudopenlabs.workflow.dto.ExecutionResponse;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowOutputParam;
import com.huawei.cloudopenlabs.workflow.entity.ExecutionStatus;
import com.huawei.cloudopenlabs.workflow.entity.TriggerType;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowExecutionEntity;
import com.huawei.cloudopenlabs.workflow.execution.engine.WorkflowScheduler;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowExecutionMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流执行服务实现类 (MyBatis-Plus版本)
 *
 * 集成了执行引擎框架，提供实际的工作流执行能力
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private final WorkflowExecutionMapper executionMapper;
    private final WorkflowMapper workflowMapper;
    private final WorkflowScheduler workflowScheduler;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public String executeWorkflow(String workflowId, String triggeredBy, String inputData) {
        log.info("开始执行工作流: workflowId={}, triggeredBy={}", workflowId, triggeredBy);

        // 1. 查询工作流定义
        WorkflowEntity workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw BusinessException.notFound("工作流", workflowId);
        }

        // 2. 检查工作流状态（允许 DRAFT 和 PUBLISHED 状态执行，用于编辑器中试运行）
        if ("ARCHIVED".equals(workflow.getStatus())) {
            throw BusinessException.bizError("工作流已归档，无法执行: " + workflow.getName());
        }

        // 3. 解析输入数据
        Map<String, Object> inputMap = new HashMap<>();
        if (inputData != null && !inputData.isEmpty()) {
            try {
                inputMap = objectMapper.readValue(inputData,
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("解析输入数据失败，使用空Map", e);
            }
        }

        // 4. 提交到执行调度器
        String executionUuid = workflowScheduler.submitExecution(
                workflowId,
                inputMap,
                triggeredBy,
                TriggerType.MANUAL.name()
        );

        // 5. 更新工作流状态
        workflow.setHasRun(true);
        workflowMapper.updateById(workflow);

        // 6. 获取执行记录ID
        WorkflowExecutionEntity execution = executionMapper.selectByExecutionUuid(executionUuid)
                .orElseThrow(() -> BusinessException.systemError("创建执行记录失败"));

        log.info("工作流执行已提交: workflowId={}, executionUuid={}, executionId={}",
                workflowId, executionUuid, execution.getId());

        return execution.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(String id) {
        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw BusinessException.notFound("执行记录", id);
        }
        return convertToResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionByUuid(String uuid) {
        WorkflowExecutionEntity execution = executionMapper.selectByExecutionUuid(uuid)
                .orElseThrow(() -> BusinessException.notFound("执行记录", uuid));
        return convertToResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ExecutionResponse> getExecutionsByWorkflowId(String workflowId, Pageable pageable) {
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
    public void abortExecution(String id) {
        log.info("中止执行: {}", id);

        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw BusinessException.notFound("执行记录", id);
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
    public void updateProgress(String id, int progress) {
        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw BusinessException.notFound("执行记录", id);
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
    public void completeExecution(String id, String outputData, String nodeExecutions) {
        log.info("完成执行: {}", id);

        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw BusinessException.notFound("执行记录", id);
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
    public void failExecution(String id, String errorMessage) {
        log.error("执行失败: {}, 错误: {}", id, errorMessage);

        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw BusinessException.notFound("执行记录", id);
        }

        execution.setStatus(ExecutionStatus.FAILED.name());
        execution.setErrorMessage(errorMessage);
        execution.setEndTime(LocalDateTime.now());
        execution.setDurationMs(calculateDuration(execution.getStartTime(), execution.getEndTime()));

        executionMapper.updateById(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionOutputResponse getExecutionOutputs(String id) {
        WorkflowExecutionEntity execution = executionMapper.selectById(id);
        if (execution == null) {
            throw BusinessException.notFound("执行记录", id);
        }

        List<WorkflowOutputParam> outputs = parseAndEnrichOutputs(execution.getOutputData());

        return ExecutionOutputResponse.builder()
                .executionId(execution.getId())
                .executionUuid(execution.getExecutionUuid())
                .status(execution.getStatus())
                .outputs(outputs)
                .build();
    }

    /**
     * 解析并增强输出数据
     */
    @SuppressWarnings("unchecked")
    private List<WorkflowOutputParam> parseAndEnrichOutputs(String outputData) {
        List<WorkflowOutputParam> result = new ArrayList<>();

        if (outputData == null || outputData.isEmpty()) {
            return result;
        }

        try {
            Map<String, Object> outputMap = objectMapper.readValue(outputData,
                    new TypeReference<Map<String, Object>>() {});

            for (Map.Entry<String, Object> entry : outputMap.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Map) {
                    // 增强格式（带类型信息）
                    Map<String, Object> paramData = (Map<String, Object>) value;
                    WorkflowOutputParam param = convertToOutputParam(paramData);
                    result.add(param);
                } else {
                    // 简单格式（兼容旧数据）
                    result.add(WorkflowOutputParam.builder()
                            .name(entry.getKey())
                            .type("String")
                            .category("BASIC")
                            .value(value)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("解析输出数据失败", e);
        }

        return result;
    }

    /**
     * 将 Map 转换为 WorkflowOutputParam
     */
    @SuppressWarnings("unchecked")
    private WorkflowOutputParam convertToOutputParam(Map<String, Object> paramData) {
        WorkflowOutputParam.WorkflowOutputParamBuilder builder = WorkflowOutputParam.builder()
                .name((String) paramData.get("name"))
                .label((String) paramData.getOrDefault("label", paramData.get("name")))
                .type((String) paramData.get("type"))
                .category((String) paramData.getOrDefault("category", "BASIC"))
                .fileType((String) paramData.get("fileType"))
                .value(paramData.get("value"))
                .fileName((String) paramData.get("fileName"))
                .fileSize(paramData.get("fileSize") != null ?
                        ((Number) paramData.get("fileSize")).longValue() : null)
                .downloadUrl((String) paramData.get("downloadUrl"));

        // 处理文件列表
        Object filesObj = paramData.get("files");
        if (filesObj instanceof List) {
            List<Map<String, Object>> filesData = (List<Map<String, Object>>) filesObj;
            List<WorkflowOutputParam.FileInfo> files = new ArrayList<>();
            for (Map<String, Object> fileData : filesData) {
                files.add(WorkflowOutputParam.FileInfo.builder()
                        .fileId((String) fileData.get("fileId"))
                        .fileName((String) fileData.get("fileName"))
                        .fileSize(fileData.get("fileSize") != null ?
                                ((Number) fileData.get("fileSize")).longValue() : null)
                        .downloadUrl((String) fileData.get("downloadUrl"))
                        .build());
            }
            builder.files(files);
        }

        return builder.build();
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
