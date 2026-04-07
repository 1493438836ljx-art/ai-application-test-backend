package com.huawei.cloudopenlabs.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作流执行输出响应 DTO
 * 用于返回工作流执行的结构化输出参数
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionOutputResponse {

    /**
     * 执行记录ID
     */
    private Long executionId;

    /**
     * 执行记录UUID
     */
    private String executionUuid;

    /**
     * 执行状态（SUCCESS/FAILED/RUNNING/PENDING）
     */
    private String status;

    /**
     * 输出参数列表
     */
    private List<WorkflowOutputParam> outputs;
}
