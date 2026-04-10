/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.service;

import com.huawei.cloudopenlabs.workflow.dto.*;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowResponse;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowUpdateRequest;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 工作流服务接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public interface WorkflowService {

    /**
     * 创建工作流（包含完整数据）
     *
     * @param request 创建请求（包含节点、连线、关联）
     * @return 工作流响应
     */
    WorkflowResponse createWorkflow(WorkflowCreateRequest request);

    /**
     * 获取工作流详情
     *
     * @param id 工作流ID
     * @return 工作流响应
     */
    WorkflowResponse getWorkflowById(String id);

    /**
     * 获取工作流列表
     *
     * @param pageable 分页参数
     * @return 工作流分页列表
     */
    Page<WorkflowResponse> getWorkflowList(Pageable pageable);

    /**
     * 根据状态获取工作流列表
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 工作流分页列表
     */
    Page<WorkflowResponse> getWorkflowListByStatus(WorkflowStatus status, Pageable pageable);

    /**
     * 搜索工作流
     *
     * @param name     名称关键字
     * @param pageable 分页参数
     * @return 工作流分页列表
     */
    Page<WorkflowResponse> searchWorkflows(String name, Pageable pageable);

    /**
     * 更新工作流
     *
     * @param id      工作流ID
     * @param request 更新请求
     * @return 工作流响应
     */
    WorkflowResponse updateWorkflow(String id, WorkflowUpdateRequest request);

    /**
     * 删除工作流
     *
     * @param id 工作流ID
     */
    void deleteWorkflow(String id);

    /**
     * 发布工作流
     *
     * @param id 工作流ID
     * @return 工作流响应
     */
    WorkflowResponse publishWorkflow(String id);

    /**
     * 取消发布工作流
     *
     * @param id 工作流ID
     * @return 工作流响应
     */
    WorkflowResponse unpublishWorkflow(String id);

    /**
     * 复制工作流
     *
     * @param id 工作流ID
     * @return 新工作流响应
     */
    WorkflowResponse copyWorkflow(String id);

    /**
     * 保存工作流完整数据（包括节点、连线、关联）
     *
     * @param id          工作流ID
     * @param nodes       节点列表
     * @param connections 连线列表
     * @param associations 关联列表
     * @return 工作流响应
     */
    WorkflowResponse saveWorkflowData(String id,
                                       List<WorkflowResponse.NodeDTO> nodes,
                                       List<WorkflowResponse.ConnectionDTO> connections,
                                       List<WorkflowResponse.AssociationDTO> associations);

    /**
     * 获取默认工作流详情
     *
     * @return 默认工作流响应
     */
    WorkflowResponse getDefaultWorkflow();
}
