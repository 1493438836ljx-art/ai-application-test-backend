package com.huawei.cloudopenlabs.workflow.service;

import com.huawei.cloudopenlabs.workflow.dto.NodeCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.NodeResponse;
import com.huawei.cloudopenlabs.workflow.dto.NodeUpdateRequest;

import java.util.List;

/**
 * 工作流节点服务接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public interface WorkflowNodeService {

    // ========== CRUD ==========

    /**
     * 获取工作流的所有节点
     *
     * @param workflowId 工作流ID
     * @return 节点列表
     */
    List<NodeResponse> getNodes(String workflowId);

    /**
     * 获取单个节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 节点响应
     */
    NodeResponse getNode(String workflowId, String nodeUuid);

    /**
     * 创建节点
     *
     * @param workflowId 工作流ID
     * @param request    创建请求
     * @return 节点响应
     */
    NodeResponse createNode(String workflowId, NodeCreateRequest request);

    /**
     * 更新节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @param request    更新请求
     * @return 节点响应
     */
    NodeResponse updateNode(String workflowId, String nodeUuid, NodeUpdateRequest request);

    /**
     * 删除节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     */
    void deleteNode(String workflowId, String nodeUuid);

    // ========== 批量操作 ==========

    /**
     * 批量创建节点
     *
     * @param workflowId 工作流ID
     * @param requests   创建请求列表
     */
    void batchCreateNodes(String workflowId, List<NodeCreateRequest> requests);

    /**
     * 批量更新节点
     *
     * @param workflowId 工作流ID
     * @param requests   更新请求列表
     */
    void batchUpdateNodes(String workflowId, List<NodeUpdateRequest> requests);

    /**
     * 批量删除节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuids  节点UUID列表
     */
    void batchDeleteNodes(String workflowId, List<String> nodeUuids);

    // ========== 查询 ==========

    /**
     * 根据Skill ID查询所有引用该Skill的节点
     *
     * @param skillId Skill ID
     * @return 节点列表
     */
    List<NodeResponse> getNodesBySkillId(String skillId);

    /**
     * 批量更新节点兼容性状态
     *
     * @param nodeIds 节点ID列表
     * @param status  兼容性状态
     */
    void batchUpdateCompatibilityStatus(List<String> nodeIds, String status);
}
