/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.kafka.dto;

import com.huawei.cloudopenlabs.workflow.execution.executor.skill.dto.SkillExecutionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill执行Kafka请求消息
 * 包装SkillExecutionRequest，添加Kafka通信所需的关联字段
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillExecutionKafkaRequest {

    /**
     * 请求ID（UUID），用于关联请求和响应
     */
    private String requestId;

    /**
     * 工作流执行ID
     */
    private String executionId;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 节点UUID
     */
    private String nodeUuid;

    /**
     * 响应topic名称
     */
    private String callbackTopic;

    /**
     * 请求时间戳
     */
    private long timestamp;

    /**
     * Skill执行请求（包含所有执行所需信息）
     */
    private SkillExecutionRequest request;
}
