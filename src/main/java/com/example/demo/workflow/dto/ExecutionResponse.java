package com.example.demo.workflow.dto;

import com.example.demo.workflow.entity.ExecutionStatus;
import com.example.demo.workflow.entity.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行记录响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "执行记录响应")
public class ExecutionResponse {

    @Schema(description = "执行记录ID")
    private Long id;

    @Schema(description = "工作流ID")
    private Long workflowId;

    @Schema(description = "执行UUID")
    private String executionUuid;

    @Schema(description = "执行状态")
    private ExecutionStatus status;

    @Schema(description = "触发类型")
    private TriggerType triggerType;

    @Schema(description = "触发人")
    private String triggeredBy;

    @Schema(description = "输入数据")
    private String inputData;

    @Schema(description = "输出数据")
    private String outputData;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "节点执行详情")
    private String nodeExecutions;

    @Schema(description = "执行进度")
    private Integer progress;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "执行耗时(毫秒)")
    private Long durationMs;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
