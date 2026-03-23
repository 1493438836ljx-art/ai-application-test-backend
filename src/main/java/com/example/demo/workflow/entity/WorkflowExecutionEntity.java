package com.example.demo.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流执行记录实体类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_execution")
public class WorkflowExecutionEntity extends BaseEntity {

    /**
     * 工作流ID
     */
    @TableField("workflow_id")
    private Long workflowId;

    /**
     * 执行UUID
     */
    @TableField("execution_uuid")
    private String executionUuid;

    /**
     * 执行状态
     */
    @TableField("status")
    private String status;

    /**
     * 触发类型
     */
    @TableField("trigger_type")
    private String triggerType;

    /**
     * 触发人
     */
    @TableField("triggered_by")
    private String triggeredBy;

    /**
     * 输入数据(JSON)
     */
    @TableField("input_data")
    private String inputData;

    /**
     * 输出数据(JSON)
     */
    @TableField("output_data")
    private String outputData;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 节点执行详情(JSON)
     */
    @TableField("node_executions")
    private String nodeExecutions;

    /**
     * 执行进度
     */
    @TableField("progress")
    private Integer progress;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 执行耗时(毫秒)
     */
    @TableField("duration_ms")
    private Long durationMs;
}
