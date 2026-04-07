package com.huawei.cloudopenlabs.workflow.service.impl;

import com.huawei.cloudopenlabs.workflow.dto.NodeCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.NodeResponse;
import com.huawei.cloudopenlabs.workflow.dto.NodeUpdateRequest;
import com.huawei.cloudopenlabs.workflow.entity.*;
import com.huawei.cloudopenlabs.workflow.entity.*;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowConnectionMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowNodeMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowAssociationMapper;
import com.huawei.cloudopenlabs.workflow.service.WorkflowNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 工作流节点服务实现
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowNodeServiceImpl implements WorkflowNodeService {

    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowConnectionMapper connectionMapper;
    private final WorkflowAssociationMapper associationMapper;

    // ========== CRUD ==========

    @Override
    public List<NodeResponse> getNodes(Long workflowId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        return nodes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NodeResponse getNode(Long workflowId, String nodeUuid) {
        WorkflowNodeEntity node = nodeMapper.selectByWorkflowIdAndNodeUuid(workflowId, nodeUuid)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在: " + nodeUuid));
        return convertToResponse(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NodeResponse createNode(Long workflowId, NodeCreateRequest request) {
        // 校验工作流状态（通过 WorkflowService 进行）

        // 创建节点实体
        WorkflowNodeEntity node = new WorkflowNodeEntity();
        node.setWorkflowId(workflowId);
        node.setNodeUuid(request.getNodeUuid() != null ? request.getNodeUuid() : UUID.randomUUID().toString());
        node.setType(request.getType());
        node.setName(request.getName());
        node.setPositionX(request.getPositionX());
        node.setPositionY(request.getPositionY());
        node.setInputPorts(request.getInputPorts());
        node.setOutputPorts(request.getOutputPorts());
        node.setInputParams(request.getInputParams());
        node.setOutputParams(request.getOutputParams());
        node.setConfig(request.getConfig());

        // 设置 Skill 引用
        if (request.getSkillId() != null) {
            node.setSkillId(request.getSkillId());
        }
        if (request.getSkillSnapshot() != null) {
            node.setSkillSnapshot(request.getSkillSnapshot());
        }

        // 设置节点分类
        node.setNodeCategory(determineNodeCategory(request.getType()));

        // 设置默认值
        if (node.getExecutionLocation() == null) {
            node.setExecutionLocation(ExecutionLocation.SERVICE.name());
        }
        if (node.getErrorStrategy() == null) {
            node.setErrorStrategy(ErrorStrategy.STOP.name());
        }
        if (node.getCompatibilityStatus() == null) {
            node.setCompatibilityStatus(CompatibilityStatus.COMPATIBLE.name());
        }

        nodeMapper.insert(node);
        log.info("创建节点成功: workflowId={}, nodeUuid={}", workflowId, node.getNodeUuid());

        return convertToResponse(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NodeResponse updateNode(Long workflowId, String nodeUuid, NodeUpdateRequest request) {
        WorkflowNodeEntity node = nodeMapper.selectByWorkflowIdAndNodeUuid(workflowId, nodeUuid)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在: " + nodeUuid));

        // 更新字段
        if (request.getNodeName() != null) {
            node.setName(request.getNodeName());
        }
        if (request.getPositionX() != null) {
            node.setPositionX(request.getPositionX());
        }
        if (request.getPositionY() != null) {
            node.setPositionY(request.getPositionY());
        }
        if (request.getSkillId() != null) {
            node.setSkillId(request.getSkillId());
        }
        if (request.getSkillSnapshot() != null) {
            node.setSkillSnapshot(request.getSkillSnapshot());
        }
        if (request.getInputPorts() != null) {
            node.setInputPorts(request.getInputPorts());
        }
        if (request.getOutputPorts() != null) {
            node.setOutputPorts(request.getOutputPorts());
        }
        if (request.getInputParams() != null) {
            node.setInputParams(request.getInputParams());
        }
        if (request.getOutputParams() != null) {
            node.setOutputParams(request.getOutputParams());
        }
        if (request.getExecutionLocation() != null) {
            node.setExecutionLocation(request.getExecutionLocation());
        }
        if (request.getErrorStrategy() != null) {
            node.setErrorStrategy(request.getErrorStrategy());
        }
        if (request.getRetryCount() != null) {
            node.setRetryCount(request.getRetryCount());
        }
        if (request.getRetryInterval() != null) {
            node.setRetryInterval(request.getRetryInterval());
        }
        if (request.getErrorBranchId() != null) {
            node.setErrorBranchId(request.getErrorBranchId());
        }
        if (request.getConditionType() != null) {
            node.setConditionType(request.getConditionType());
        }
        if (request.getConditions() != null) {
            node.setConditions(request.getConditions());
        }
        if (request.getLoopType() != null) {
            node.setLoopType(request.getLoopType());
        }
        if (request.getLoopConfig() != null) {
            node.setLoopConfig(request.getLoopConfig());
        }
        if (request.getBatchConfig() != null) {
            node.setBatchConfig(request.getBatchConfig());
        }
        if (request.getAsyncConfig() != null) {
            node.setAsyncConfig(request.getAsyncConfig());
        }
        if (request.getCollectConfig() != null) {
            node.setCollectConfig(request.getCollectConfig());
        }
        if (request.getConfig() != null) {
            node.setConfig(request.getConfig());
        }

        nodeMapper.updateById(node);
        log.info("更新节点成功: workflowId={}, nodeUuid={}", workflowId, nodeUuid);

        return convertToResponse(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long workflowId, String nodeUuid) {
        WorkflowNodeEntity node = nodeMapper.selectByWorkflowIdAndNodeUuid(workflowId, nodeUuid)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在: " + nodeUuid));

        // 不允许删除开始或结束节点
        if ("start".equals(node.getType()) || "end".equals(node.getType())) {
            throw new IllegalArgumentException("不能删除开始或结束节点");
        }

        // 删除相关连线
        connectionMapper.deleteBySourceNodeId(node.getId());
        connectionMapper.deleteByTargetNodeId(node.getId());

        // 删除相关关联（作为容器或子节点）
        // TODO: 需要添加按节点ID删除关联的方法

        // 删除节点
        nodeMapper.deleteById(node.getId());
        log.info("删除节点成功: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
    }

    // ========== 批量操作 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateNodes(Long workflowId, List<NodeCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (NodeCreateRequest request : requests) {
            createNode(workflowId, request);
        }
        log.info("批量创建节点成功: workflowId={}, count={}", workflowId, requests.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateNodes(Long workflowId, List<NodeUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (NodeUpdateRequest request : requests) {
            updateNode(workflowId, request.getNodeUuid(), request);
        }
        log.info("批量更新节点成功: workflowId={}, count={}", workflowId, requests.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteNodes(Long workflowId, List<String> nodeUuids) {
        if (nodeUuids == null || nodeUuids.isEmpty()) {
            return;
        }
        for (String nodeUuid : nodeUuids) {
            deleteNode(workflowId, nodeUuid);
        }
        log.info("批量删除节点成功: workflowId={}, count={}", workflowId, nodeUuids.size());
    }

    // ========== 查询 ==========

    @Override
    public List<NodeResponse> getNodesBySkillId(String skillId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectBySkillId(skillId);
        return nodes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateCompatibilityStatus(List<Long> nodeIds, String status) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }
        nodeMapper.batchUpdateCompatibilityStatus(nodeIds, status);
        log.info("批量更新节点兼容性状态成功: count={}, status={}", nodeIds.size(), status);
    }

    // ========== 私有方法 ==========

    /**
     * 根据节点类型确定节点分类
     */
    private String determineNodeCategory(String nodeType) {
        if (nodeType == null) {
            return NodeCategory.BASIC.name();
        }
        switch (nodeType) {
            case "start":
            case "end":
            case "skill":
                return NodeCategory.BASIC.name();
            case "condition_simple":
            case "condition_multi":
            case "loop":
                return NodeCategory.LOGIC.name();
            case "batch":
            case "async":
            case "collect":
                return NodeCategory.EXECUTION.name();
            default:
                return NodeCategory.BASIC.name();
        }
    }

    /**
     * 转换为响应DTO
     */
    private NodeResponse convertToResponse(WorkflowNodeEntity entity) {
        NodeResponse response = new NodeResponse();
        response.setId(entity.getId());
        response.setNodeUuid(entity.getNodeUuid());
        response.setNodeName(entity.getName());
        response.setNodeType(entity.getType());
        response.setNodeCategory(entity.getNodeCategory());
        response.setPositionX(entity.getPositionX());
        response.setPositionY(entity.getPositionY());
        response.setSkillId(entity.getSkillId());
        response.setSkillSnapshot(entity.getSkillSnapshot());
        response.setInputPorts(entity.getInputPorts());
        response.setOutputPorts(entity.getOutputPorts());
        response.setInputParams(entity.getInputParams());
        response.setOutputParams(entity.getOutputParams());
        response.setExecutionLocation(entity.getExecutionLocation());
        response.setErrorStrategy(entity.getErrorStrategy());
        response.setRetryCount(entity.getRetryCount());
        response.setRetryInterval(entity.getRetryInterval());
        response.setErrorBranchId(entity.getErrorBranchId());
        response.setConditionType(entity.getConditionType());
        response.setConditions(entity.getConditions());
        response.setLoopType(entity.getLoopType());
        response.setLoopConfig(entity.getLoopConfig());
        response.setBatchConfig(entity.getBatchConfig());
        response.setAsyncConfig(entity.getAsyncConfig());
        response.setCollectConfig(entity.getCollectConfig());
        response.setCompatibilityStatus(entity.getCompatibilityStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
