/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.service;

import com.huawei.cloudopenlabs.workflow.controller.WorkflowValidationController.ReferenceCheckResult;
import com.huawei.cloudopenlabs.workflow.dto.AvailableVariable;
import com.huawei.cloudopenlabs.workflow.dto.NodeResponse;
import com.huawei.cloudopenlabs.workflow.dto.ValidationResult;

import java.util.List;

/**
 * 工作流验证服务接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
public interface WorkflowValidationService {

    /**
     * 验证工作流
     *
     * @param workflowId 工作流ID
     * @return 验证结果
     */
    ValidationResult validate(String workflowId);

    /**
     * 获取节点的前置节点列表
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 前置节点列表
     */
    List<NodeResponse> getPredecessors(String workflowId, String nodeUuid);

    /**
     * 获取节点可引用的变量列表
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 可用变量列表
     */
    List<AvailableVariable> getAvailableVariables(String workflowId, String nodeUuid);

    /**
     * 检查参数引用是否有效
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @param reference  参数引用表达式
     * @return 检查结果
     */
    ReferenceCheckResult checkReference(String workflowId, String nodeUuid, String reference);

    /**
     * 获取工作流的执行顺序（拓扑排序）
     *
     * @param workflowId 工作流ID
     * @return 节点执行顺序
     */
    List<NodeResponse> getExecutionOrder(String workflowId);
}
