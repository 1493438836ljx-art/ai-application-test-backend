/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.context;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowConnectionEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import lombok.Data;

import java.util.List;

/**
 * 工作流定义
 * 封装工作流及其节点和连线的完整定义
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class WorkflowDefinition {

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 工作流UUID
     */
    private String workflowUuid;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 工作流版本
     */
    private Integer version;

    /**
     * 工作流实体
     */
    private WorkflowEntity workflow;

    /**
     * 所有节点列表
     */
    private List<WorkflowNodeEntity> nodes;

    /**
     * 所有连线列表
     */
    private List<WorkflowConnectionEntity> connections;

    /**
     * 从工作流实体构建定义
     */
    public static WorkflowDefinition from(WorkflowEntity workflow,
                                           List<WorkflowNodeEntity> nodes,
                                           List<WorkflowConnectionEntity> connections) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setWorkflowId(workflow.getId());
        definition.setWorkflowUuid(String.valueOf(workflow.getId()));
        definition.setWorkflowName(workflow.getName());
        definition.setDescription(workflow.getDescription());
        definition.setVersion(workflow.getVersion());
        definition.setWorkflow(workflow);
        definition.setNodes(nodes);
        definition.setConnections(connections);
        return definition;
    }

    /**
     * 根据UUID获取节点
     */
    public WorkflowNodeEntity getNodeByUuid(String nodeUuid) {
        if (nodes == null || nodeUuid == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> nodeUuid.equals(n.getNodeUuid()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据ID获取节点
     */
    public WorkflowNodeEntity getNodeById(String nodeId) {
        if (nodes == null || nodeId == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称获取节点
     */
    public WorkflowNodeEntity getNodeByName(String name) {
        if (nodes == null || name == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> name.equals(n.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取指定类型的所有节点
     */
    public List<WorkflowNodeEntity> getNodesByType(String type) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .filter(n -> type.equals(n.getType()))
                .toList();
    }

    /**
     * 获取开始节点
     */
    public WorkflowNodeEntity getStartNode() {
        return getNodeByType("start");
    }

    /**
     * 获取结束节点
     */
    public WorkflowNodeEntity getEndNode() {
        return getNodeByType("end");
    }

    /**
     * 获取指定类型的首个节点
     */
    private WorkflowNodeEntity getNodeByType(String type) {
        if (nodes == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> type.equals(n.getType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取节点的总数
     */
    public int getNodeCount() {
        return nodes != null ? nodes.size() : 0;
    }

    /**
     * 获取连线的总数
     */
    public int getConnectionCount() {
        return connections != null ? connections.size() : 0;
    }
}
