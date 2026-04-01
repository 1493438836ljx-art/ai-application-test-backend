package com.example.demo.workflow.service;

import com.example.demo.workflow.dto.AssociationCreateRequest;
import com.example.demo.workflow.dto.AssociationResponse;

import java.util.List;

/**
 * 工作流关联服务接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface WorkflowAssociationService {

    // ========== CRUD ==========

    /**
     * 获取工作流的所有关联
     *
     * @param workflowId 工作流ID
     * @return 关联列表
     */
    List<AssociationResponse> getAssociations(Long workflowId);

    /**
     * 获取单个关联
     *
     * @param workflowId     工作流ID
     * @param associationId 关联ID
     * @return 关联响应
     */
    AssociationResponse getAssociation(Long workflowId, Long associationId);

    /**
     * 创建关联
     *
     * @param workflowId 工作流ID
     * @param request    创建请求
     * @return 关联响应
     */
    AssociationResponse createAssociation(Long workflowId, AssociationCreateRequest request);

    /**
     * 删除关联
     *
     * @param workflowId     工作流ID
     * @param associationId 关联ID
     */
    void deleteAssociation(Long workflowId, Long associationId);

    // ========== 批量操作 ==========

    /**
     * 批量创建关联
     *
     * @param workflowId 工作流ID
     * @param requests   创建请求列表
     */
    void batchCreateAssociations(Long workflowId, List<AssociationCreateRequest> requests);

    /**
     * 批量删除关联
     *
     * @param workflowId      工作流ID
     * @param associationIds 关联ID列表
     */
    void batchDeleteAssociations(Long workflowId, List<Long> associationIds);

    // ========== 查询 ==========

    /**
     * 根据容器节点查询关联
     *
     * @param workflowId        工作流ID
     * @param containerNodeUuid 容器节点UUID
     * @return 关联列表
     */
    List<AssociationResponse> getByContainerNode(Long workflowId, String containerNodeUuid);

    /**
     * 根据子节点查询关联
     *
     * @param workflowId   工作流ID
     * @param bodyNodeUuid 子节点UUID
     * @return 关联列表
     */
    List<AssociationResponse> getByBodyNode(Long workflowId, String bodyNodeUuid);
}
