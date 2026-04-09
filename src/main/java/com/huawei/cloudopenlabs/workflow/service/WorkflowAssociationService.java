package com.huawei.cloudopenlabs.workflow.service;

import com.huawei.cloudopenlabs.workflow.dto.AssociationCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.AssociationResponse;

import java.util.List;

/**
 * 工作流关联服务接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public interface WorkflowAssociationService {

    // ========== CRUD ==========

    /**
     * 获取工作流的所有关联
     *
     * @param workflowId 工作流ID
     * @return 关联列表
     */
    List<AssociationResponse> getAssociations(String workflowId);

    /**
     * 获取单个关联
     *
     * @param workflowId     工作流ID
     * @param associationId 关联ID
     * @return 关联响应
     */
    AssociationResponse getAssociation(String workflowId, String associationId);

    /**
     * 创建关联
     *
     * @param workflowId 工作流ID
     * @param request    创建请求
     * @return 关联响应
     */
    AssociationResponse createAssociation(String workflowId, AssociationCreateRequest request);

    /**
     * 删除关联
     *
     * @param workflowId     工作流ID
     * @param associationId 关联ID
     */
    void deleteAssociation(String workflowId, String associationId);

    // ========== 批量操作 ==========

    /**
     * 批量创建关联
     *
     * @param workflowId 工作流ID
     * @param requests   创建请求列表
     */
    void batchCreateAssociations(String workflowId, List<AssociationCreateRequest> requests);

    /**
     * 批量删除关联
     *
     * @param workflowId      工作流ID
     * @param associationIds 关联ID列表
     */
    void batchDeleteAssociations(String workflowId, List<String> associationIds);

    // ========== 查询 ==========

    /**
     * 根据容器节点查询关联
     *
     * @param workflowId        工作流ID
     * @param containerNodeUuid 容器节点UUID
     * @return 关联列表
     */
    List<AssociationResponse> getByContainerNode(String workflowId, String containerNodeUuid);

    /**
     * 根据子节点查询关联
     *
     * @param workflowId   工作流ID
     * @param bodyNodeUuid 子节点UUID
     * @return 关联列表
     */
    List<AssociationResponse> getByBodyNode(String workflowId, String bodyNodeUuid);
}
