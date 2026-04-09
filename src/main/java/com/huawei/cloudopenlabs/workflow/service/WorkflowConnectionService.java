package com.huawei.cloudopenlabs.workflow.service;

import com.huawei.cloudopenlabs.workflow.dto.ConnectionResponse;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowResponse;

import java.util.List;

/**
 * 工作流连线服务接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public interface WorkflowConnectionService {

    // ========== CRUD ==========

    /**
     * 获取工作流的所有连线
     *
     * @param workflowId 工作流ID
     * @return 连线列表
     */
    List<ConnectionResponse> getConnections(String workflowId);

    /**
     * 获取单个连线
     *
     * @param workflowId      工作流ID
     * @param connectionUuid 连线UUID
     * @return 连线响应
     */
    ConnectionResponse getConnection(String workflowId, String connectionUuid);

    /**
     * 创建连线
     *
     * @param workflowId 工作流ID
     * @param request    连线数据
     * @return 连线响应
     */
    ConnectionResponse createConnection(String workflowId, WorkflowResponse.ConnectionDTO request);

    /**
     * 删除连线
     *
     * @param workflowId      工作流ID
     * @param connectionUuid 连线UUID
     */
    void deleteConnection(String workflowId, String connectionUuid);

    // ========== 批量操作 ==========

    /**
     * 批量创建连线
     *
     * @param workflowId 工作流ID
     * @param requests   连线数据列表
     */
    void batchCreateConnections(String workflowId, List<WorkflowResponse.ConnectionDTO> requests);

    /**
     * 批量删除连线
     *
     * @param workflowId       工作流ID
     * @param connectionUuids 连线UUID列表
     */
    void batchDeleteConnections(String workflowId, List<String> connectionUuids);

    // ========== 查询 ==========

    /**
     * 获取节点的所有出边
     *
     * @param nodeId 源节点ID
     * @return 连线列表
     */
    List<ConnectionResponse> getConnectionsBySourceNode(String nodeId);

    /**
     * 获取节点的所有入边
     *
     * @param nodeId 目标节点ID
     * @return 连线列表
     */
    List<ConnectionResponse> getConnectionsByTargetNode(String nodeId);
}
