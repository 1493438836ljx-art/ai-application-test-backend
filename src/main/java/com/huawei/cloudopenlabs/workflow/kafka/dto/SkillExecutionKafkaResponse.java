package com.huawei.cloudopenlabs.workflow.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Skill执行Kafka响应消息
 * Executor执行完成后通过Kafka返回的结果
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillExecutionKafkaResponse {

    /**
     * 请求ID，与SkillExecutionKafkaRequest.requestId对应
     */
    private String requestId;

    /**
     * 工作流执行ID
     */
    private String executionId;

    /**
     * 节点UUID
     */
    private String nodeUuid;

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 输出参数
     */
    private Map<String, Object> outputs;

    /**
     * 执行日志
     */
    private String logs;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * Executor处理时间戳
     */
    private long executorTimestamp;
}
