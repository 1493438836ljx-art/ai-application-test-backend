package com.huawei.cloudopenlabs.workflow.dto;

import com.huawei.cloudopenlabs.workflow.entity.ExecutionStatus;
import com.huawei.cloudopenlabs.workflow.entity.TriggerType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行记录响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ExecutionResponse {

    private Long id;

    private Long workflowId;

    private String executionUuid;

    private ExecutionStatus status;

    private TriggerType triggerType;

    private String triggeredBy;

    private String inputData;

    private String outputData;

    private String errorMessage;

    private String nodeExecutions;

    private Integer progress;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
