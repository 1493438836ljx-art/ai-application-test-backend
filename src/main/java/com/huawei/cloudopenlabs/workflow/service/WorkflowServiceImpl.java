/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huawei.cloudopenlabs.common.exception.BusinessException;
import com.huawei.cloudopenlabs.workflow.dto.*;
import com.huawei.cloudopenlabs.workflow.entity.*;
import com.huawei.cloudopenlabs.workflow.mapper.*;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowResponse;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowUpdateRequest;
import com.huawei.cloudopenlabs.workflow.entity.*;
import com.huawei.cloudopenlabs.workflow.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 工作流服务实现类 (MyBatis-Plus版本)
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowConnectionMapper connectionMapper;
    private final WorkflowAssociationMapper associationMapper;
    private final WorkflowNodeTypeMapper nodeTypeMapper;

    @Override
    @Transactional
    public WorkflowResponse createWorkflow(WorkflowCreateRequest request) {
        log.info("Creating workflow: {}, nodeCount: {}, connectionCount: {}, associationCount: {}",
                request.getName(),
                request.getNodes() != null ? request.getNodes().size() : 0,
                request.getConnections() != null ? request.getConnections().size() : 0,
                request.getAssociations() != null ? request.getAssociations().size() : 0);

        // 1. 创建工作流主表记录
        WorkflowEntity workflow = new WorkflowEntity();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setCreatedBy(request.getCreatedBy());
        workflow.setPublished(false);
        workflow.setHasRun(false);
        workflow.setVersion(1);
        workflow.setStatus(WorkflowStatus.DRAFT.name());
        workflow.setDeleted(false);

        workflowMapper.insert(workflow);
        String workflowId = workflow.getId();

        // 2. 保存节点数据
        Map<String, String> nodeUuidToIdMap = new HashMap<>();
        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            for (WorkflowCreateRequest.NodeData nodeData : request.getNodes()) {
                WorkflowNodeEntity node = new WorkflowNodeEntity();
                node.setWorkflowId(workflowId);
                node.setNodeUuid(nodeData.getNodeUuid() != null ? nodeData.getNodeUuid() : UUID.randomUUID().toString());
                node.setType(nodeData.getType());
                node.setName(nodeData.getName());
                node.setPositionX(nodeData.getPositionX() != null ? nodeData.getPositionX() : 0);
                node.setPositionY(nodeData.getPositionY() != null ? nodeData.getPositionY() : 0);
                node.setInputPorts(nodeData.getInputPorts() != null ? nodeData.getInputPorts() : "[]");
                node.setOutputPorts(nodeData.getOutputPorts() != null ? nodeData.getOutputPorts() : "[]");
                node.setInputParams(nodeData.getInputParams() != null ? nodeData.getInputParams() : "[]");
                node.setOutputParams(nodeData.getOutputParams() != null ? nodeData.getOutputParams() : "[]");
                node.setConfig(nodeData.getConfig() != null ? nodeData.getConfig() : "{}");
                node.setParentNodeId(null); // 先设为null，后面处理父节点关系

                nodeMapper.insert(node);
                nodeUuidToIdMap.put(node.getNodeUuid(), node.getId());
            }

            // 处理父节点关系（循环体内节点）
            for (WorkflowCreateRequest.NodeData nodeData : request.getNodes()) {
                if (nodeData.getParentNodeUuid() != null && nodeData.getNodeUuid() != null) {
                    String nodeId = nodeUuidToIdMap.get(nodeData.getNodeUuid());
                    String parentNodeId = nodeUuidToIdMap.get(nodeData.getParentNodeUuid());
                    if (nodeId != null && parentNodeId != null) {
                        WorkflowNodeEntity node = nodeMapper.selectById(nodeId);
                        if (node != null) {
                            node.setParentNodeId(parentNodeId);
                            nodeMapper.updateById(node);
                        }
                    }
                }
            }
        }

        // 3. 保存连线数据
        if (request.getConnections() != null && !request.getConnections().isEmpty()) {
            for (WorkflowCreateRequest.ConnectionData connData : request.getConnections()) {
                String sourceNodeId = nodeUuidToIdMap.get(connData.getSourceNodeUuid());
                String targetNodeId = nodeUuidToIdMap.get(connData.getTargetNodeUuid());

                if (sourceNodeId == null || targetNodeId == null) {
                    log.warn("Connection node ID mapping failed: sourceUuid={}, targetUuid={}",
                            connData.getSourceNodeUuid(), connData.getTargetNodeUuid());
                    continue;
                }

                WorkflowConnectionEntity connection = new WorkflowConnectionEntity();
                connection.setWorkflowId(workflowId);
                connection.setConnectionUuid(connData.getConnectionUuid() != null ?
                        connData.getConnectionUuid() : UUID.randomUUID().toString());
                connection.setSourceNodeId(sourceNodeId);
                connection.setTargetNodeId(targetNodeId);
                connection.setSourcePortId(connData.getSourcePortId());
                connection.setTargetPortId(connData.getTargetPortId());
                connection.setSourceParamIndex(connData.getSourceParamIndex());
                connection.setTargetParamIndex(connData.getTargetParamIndex());
                connection.setLabel(connData.getLabel());

                connectionMapper.insert(connection);
            }
        }

        // 4. 保存关联数据（循环与循环体关系）
        if (request.getAssociations() != null && !request.getAssociations().isEmpty()) {
            for (WorkflowCreateRequest.AssociationData assocData : request.getAssociations()) {
                String containerNodeId = nodeUuidToIdMap.get(assocData.getContainerNodeUuid());
                String bodyNodeId = nodeUuidToIdMap.get(assocData.getBodyNodeUuid());

                if (containerNodeId == null || bodyNodeId == null) {
                    log.warn("Association node ID mapping failed: containerNodeUuid={}, bodyNodeUuid={}",
                            assocData.getContainerNodeUuid(), assocData.getBodyNodeUuid());
                    continue;
                }

                WorkflowAssociationEntity association = new WorkflowAssociationEntity();
                association.setWorkflowId(workflowId);
                association.setContainerNodeId(containerNodeId);
                association.setBodyNodeId(bodyNodeId);
                association.setAssociationType(assocData.getAssociationType() != null ?
                        assocData.getAssociationType() : "LOOP_BODY");

                associationMapper.insert(association);
            }
        }

        log.info("Workflow created successfully: ID={}", workflowId);
        return getWorkflowById(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(String id) {
        WorkflowEntity workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw BusinessException.notFound("工作流", id);
        }
        return convertToResponseWithDetails(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<WorkflowResponse> getWorkflowList(org.springframework.data.domain.Pageable pageable) {
        Page<WorkflowEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        IPage<WorkflowEntity> result = workflowMapper.selectPage(page, new LambdaQueryWrapper<WorkflowEntity>().eq(WorkflowEntity::getDeleted, false).orderByDesc(WorkflowEntity::getCreatedAt));
        return convertToSpringPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<WorkflowResponse> getWorkflowListByStatus(WorkflowStatus status, org.springframework.data.domain.Pageable pageable) {
        Page<WorkflowEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        IPage<WorkflowEntity> result = workflowMapper.selectByStatus(page, status.name());
        return convertToSpringPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<WorkflowResponse> searchWorkflows(String name, org.springframework.data.domain.Pageable pageable) {
        Page<WorkflowEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        IPage<WorkflowEntity> result = workflowMapper.selectByNameLike(page, name);
        return convertToSpringPage(result);
    }

    @Override
    @Transactional
    public WorkflowResponse updateWorkflow(String id, WorkflowUpdateRequest request) {
        log.info("Updating workflow: {}", id);

        WorkflowEntity workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw BusinessException.notFound("工作流", id);
        }

        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.getUpdatedBy() != null) {
            workflow.setUpdatedBy(request.getUpdatedBy());
        }

        workflowMapper.updateById(workflow);
        return convertToResponse(workflow);
    }

    @Override
    @Transactional
    public void deleteWorkflow(String id) {
        log.info("Deleting workflow: {}", id);
        workflowMapper.deleteById(id);
    }

    @Override
    @Transactional
    public WorkflowResponse publishWorkflow(String id) {
        log.info("Publishing workflow: {}", id);

        WorkflowEntity workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw BusinessException.notFound("工作流", id);
        }

        workflow.setPublished(true);
        workflow.setStatus(WorkflowStatus.PUBLISHED.name());
        workflowMapper.updateById(workflow);

        return convertToResponse(workflow);
    }

    @Override
    @Transactional
    public WorkflowResponse unpublishWorkflow(String id) {
        log.info("Unpublishing workflow: {}", id);

        WorkflowEntity workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw BusinessException.notFound("工作流", id);
        }

        // 检查是否有正在执行的任务
        // TODO: 添加执行中的任务检查

        workflow.setPublished(false);
        workflow.setStatus(WorkflowStatus.DRAFT.name());
        workflowMapper.updateById(workflow);

        return convertToResponse(workflow);
    }

    @Override
    @Transactional
    public WorkflowResponse copyWorkflow(String id) {
        log.info("Copying workflow: {}", id);

        WorkflowEntity original = workflowMapper.selectById(id);
        if (original == null) {
            throw new RuntimeException("工作流不存在: " + id);
        }

        // 创建新工作流
        WorkflowEntity newWorkflow = new WorkflowEntity();
        newWorkflow.setName(original.getName() + " (副本)");
        newWorkflow.setDescription(original.getDescription());
        newWorkflow.setCreatedBy(original.getCreatedBy());
        newWorkflow.setPublished(false);
        newWorkflow.setHasRun(false);
        newWorkflow.setVersion(1);
        newWorkflow.setStatus(WorkflowStatus.DRAFT.name());
        newWorkflow.setDeleted(false);
        workflowMapper.insert(newWorkflow);

        // 获取原工作流的节点
        List<WorkflowNodeEntity> originalNodes = nodeMapper.selectByWorkflowId(id);
        Map<String, String> oldToNewNodeIdMap = new HashMap<>();

        // 复制节点
        for (WorkflowNodeEntity originalNode : originalNodes) {
            WorkflowNodeEntity newNode = new WorkflowNodeEntity();
            newNode.setWorkflowId(newWorkflow.getId());
            newNode.setNodeUuid(UUID.randomUUID().toString());
            newNode.setType(originalNode.getType());
            newNode.setName(originalNode.getName());
            newNode.setPositionX(originalNode.getPositionX());
            newNode.setPositionY(originalNode.getPositionY());
            newNode.setInputPorts(originalNode.getInputPorts());
            newNode.setOutputPorts(originalNode.getOutputPorts());
            newNode.setInputParams(originalNode.getInputParams());
            newNode.setOutputParams(originalNode.getOutputParams());
            newNode.setConfig(originalNode.getConfig());
            newNode.setParentNodeId(originalNode.getParentNodeId());
            nodeMapper.insert(newNode);
            oldToNewNodeIdMap.put(originalNode.getId(), newNode.getId());
        }

        // 复制连线
        List<WorkflowConnectionEntity> originalConnections = connectionMapper.selectByWorkflowId(id);
        for (WorkflowConnectionEntity originalConn : originalConnections) {
            String newSourceNodeId = oldToNewNodeIdMap.get(originalConn.getSourceNodeId());
            String newTargetNodeId = oldToNewNodeIdMap.get(originalConn.getTargetNodeId());

            if (newSourceNodeId != null && newTargetNodeId != null) {
                WorkflowConnectionEntity newConn = new WorkflowConnectionEntity();
                newConn.setWorkflowId(newWorkflow.getId());
                newConn.setConnectionUuid(UUID.randomUUID().toString());
                newConn.setSourceNodeId(newSourceNodeId);
                newConn.setTargetNodeId(newTargetNodeId);
                newConn.setSourcePortId(originalConn.getSourcePortId());
                newConn.setTargetPortId(originalConn.getTargetPortId());
                newConn.setSourceParamIndex(originalConn.getSourceParamIndex());
                newConn.setTargetParamIndex(originalConn.getTargetParamIndex());
                newConn.setLabel(originalConn.getLabel());
                connectionMapper.insert(newConn);
            }
        }

        return convertToResponse(newWorkflow);
    }

    @Override
    @Transactional
    public WorkflowResponse saveWorkflowData(String id,
                                              List<WorkflowResponse.NodeDTO> nodes,
                                              List<WorkflowResponse.ConnectionDTO> connections,
                                              List<WorkflowResponse.AssociationDTO> associations) {
        log.info("Saving workflow data: {}, nodeCount: {}, connectionCount: {}, associationCount: {}",
                id, nodes.size(), connections.size(), associations.size());

        WorkflowEntity workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw BusinessException.notFound("工作流", id);
        }

        // 删除旧的节点、连线、关联
        nodeMapper.deleteByWorkflowId(id);
        connectionMapper.deleteByWorkflowId(id);
        associationMapper.deleteByWorkflowId(id);

        // 保存新节点
        // uuidToIdMap: key 是前端 nodeUuid（字符串），value 是数据库 ID（String）
        Map<String, String> uuidToIdMap = new HashMap<>();
        for (WorkflowResponse.NodeDTO nodeDTO : nodes) {
            WorkflowNodeEntity node = new WorkflowNodeEntity();
            node.setWorkflowId(id);
            node.setNodeUuid(nodeDTO.getNodeUuid());
            node.setType(nodeDTO.getType());
            node.setName(nodeDTO.getName());
            node.setPositionX(nodeDTO.getPositionX());
            node.setPositionY(nodeDTO.getPositionY());
            node.setInputPorts(nodeDTO.getInputPorts());
            node.setOutputPorts(nodeDTO.getOutputPorts());
            node.setInputParams(nodeDTO.getInputParams());
            node.setOutputParams(nodeDTO.getOutputParams());
            node.setConfig(nodeDTO.getConfig());
            node.setParentNodeId(nodeDTO.getParentNodeId());
            // Skill节点需要保存skillId和skillSnapshot
            node.setSkillId(nodeDTO.getSkillId());
            node.setSkillSnapshot(nodeDTO.getSkillSnapshot());
            node.setNodeCategory(nodeDTO.getNodeCategory());
            nodeMapper.insert(node);
            // 使用 nodeUuid 作为 key
            uuidToIdMap.put(nodeDTO.getNodeUuid(), node.getId());
        }

        // 保存连线
        for (WorkflowResponse.ConnectionDTO connDTO : connections) {
            // 使用 nodeUuid 映射到数据库 ID
            String sourceNodeId = uuidToIdMap.get(connDTO.getSourceNodeUuid());
            String targetNodeId = uuidToIdMap.get(connDTO.getTargetNodeUuid());

            // 如果没有找到，尝试使用 sourceNodeId/targetNodeId（兼容旧格式）
            if (sourceNodeId == null && connDTO.getSourceNodeId() != null) {
                sourceNodeId = uuidToIdMap.get(String.valueOf(connDTO.getSourceNodeId()));
            }
            if (targetNodeId == null && connDTO.getTargetNodeId() != null) {
                targetNodeId = uuidToIdMap.get(String.valueOf(connDTO.getTargetNodeId()));
            }

            if (sourceNodeId == null || targetNodeId == null) {
                log.warn("Connection node ID mapping failed: sourceUuid={}, targetUuid={}",
                        connDTO.getSourceNodeUuid(), connDTO.getTargetNodeUuid());
                continue;
            }

            WorkflowConnectionEntity connection = new WorkflowConnectionEntity();
            connection.setWorkflowId(id);
            connection.setConnectionUuid(connDTO.getConnectionUuid());
            connection.setSourceNodeId(sourceNodeId);
            connection.setTargetNodeId(targetNodeId);
            connection.setSourcePortId(connDTO.getSourcePortId());
            connection.setTargetPortId(connDTO.getTargetPortId());
            connection.setSourceParamIndex(connDTO.getSourceParamIndex());
            connection.setTargetParamIndex(connDTO.getTargetParamIndex());
            connection.setLabel(connDTO.getLabel());
            connectionMapper.insert(connection);
        }

        // 保存关联（使用 UUID 到 ID 的映射）
        for (WorkflowResponse.AssociationDTO assocDTO : associations) {
            // 使用 containerNodeUuid/bodyNodeUuid 映射到数据库 ID
            String containerNodeDbId = uuidToIdMap.get(assocDTO.getContainerNodeUuid());
            String bodyNodeDbId = uuidToIdMap.get(assocDTO.getBodyNodeUuid());

            if (containerNodeDbId == null || bodyNodeDbId == null) {
                log.warn("Association node ID mapping failed: containerNodeUuid={}, bodyNodeUuid={}",
                        assocDTO.getContainerNodeUuid(), assocDTO.getBodyNodeUuid());
                continue;
            }

            WorkflowAssociationEntity association = new WorkflowAssociationEntity();
            association.setWorkflowId(id);
            association.setContainerNodeId(containerNodeDbId);
            association.setBodyNodeId(bodyNodeDbId);
            association.setAssociationType(assocDTO.getAssociationType());
            associationMapper.insert(association);
        }

        return getWorkflowById(id);
    }

    @Override
    public WorkflowResponse getDefaultWorkflow() {
        log.info("Getting default workflow details");

        WorkflowResponse response = new WorkflowResponse();
        response.setId("0");
        response.setName("默认工作流");
        response.setDescription("系统默认工作流模板");
        response.setPublished(false);
        response.setHasRun(false);
        response.setVersion(1);
        response.setStatus(WorkflowStatus.DRAFT);
        response.setCreatedBy("system");
        response.setCreatedAt(LocalDateTime.now());

        // 创建默认节点
        List<WorkflowResponse.NodeDTO> nodes = createDefaultNodes();
        response.setNodes(nodes);

        // 创建默认连线
        List<WorkflowResponse.ConnectionDTO> connections = createDefaultConnections(nodes);
        response.setConnections(connections);

        // 创建默认关联（循环节点与循环体）
        List<WorkflowResponse.AssociationDTO> associations = createDefaultAssociations();
        response.setAssociations(associations);

        return response;
    }

    private List<WorkflowResponse.NodeDTO> createDefaultNodes() {
        List<WorkflowResponse.NodeDTO> nodes = new java.util.ArrayList<>();

        // 1. start (开始) - 位置(150, 250)
        WorkflowResponse.NodeDTO startNode = new WorkflowResponse.NodeDTO();
        startNode.setId("1");
        startNode.setNodeUuid("node-start");
        startNode.setType("start");
        startNode.setName("开始");
        startNode.setPositionX(150);
        startNode.setPositionY(250);
        startNode.setInputPorts("[]");
        startNode.setOutputPorts("[{\"id\":\"output-1\",\"name\":\"输出\"}]");
        startNode.setInputParams("[]");
        startNode.setOutputParams("[]");
        startNode.setConfig("{}");
        startNode.setParentNodeId(null);
        nodes.add(startNode);

        // 2. end (结束) - 位置(400, 250)
        WorkflowResponse.NodeDTO endNode = new WorkflowResponse.NodeDTO();
        endNode.setId("2");
        endNode.setNodeUuid("node-end");
        endNode.setType("end");
        endNode.setName("结束");
        endNode.setPositionX(400);
        endNode.setPositionY(250);
        endNode.setInputPorts("[{\"id\":\"input-1\",\"name\":\"输入\"}]");
        endNode.setOutputPorts("[]");
        endNode.setInputParams("[]");
        endNode.setOutputParams("[]");
        endNode.setConfig("{}");
        endNode.setParentNodeId(null);
        nodes.add(endNode);

        return nodes;
    }

    private List<WorkflowResponse.ConnectionDTO> createDefaultConnections(List<WorkflowResponse.NodeDTO> nodes) {
        List<WorkflowResponse.ConnectionDTO> connections = new java.util.ArrayList<>();

        // 节点ID到UUID的映射
        Map<String, String> nodeIdToUuidMap = new java.util.HashMap<>();
        for (WorkflowResponse.NodeDTO node : nodes) {
            nodeIdToUuidMap.put(node.getId(), node.getNodeUuid());
        }

        // start(1) -> end(2)
        WorkflowResponse.ConnectionDTO conn = new WorkflowResponse.ConnectionDTO();
        conn.setId("1");
        conn.setConnectionUuid("conn-start-end");
        conn.setSourceNodeId("1");
        conn.setSourceNodeUuid(nodeIdToUuidMap.get("1"));
        conn.setSourcePortId("output-1");
        conn.setTargetNodeId("2");
        conn.setTargetNodeUuid(nodeIdToUuidMap.get("2"));
        conn.setTargetPortId("input-1");
        conn.setSourceParamIndex(null);
        conn.setTargetParamIndex(null);
        conn.setLabel(null);
        connections.add(conn);

        return connections;
    }

    private List<WorkflowResponse.AssociationDTO> createDefaultAssociations() {
        return new java.util.ArrayList<>();
    }

    private WorkflowResponse convertToResponse(WorkflowEntity workflow) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(workflow.getId());
        response.setName(workflow.getName());
        response.setDescription(workflow.getDescription());
        response.setPublished(workflow.getPublished());
        response.setHasRun(workflow.getHasRun());
        response.setVersion(workflow.getVersion());
        response.setStatus(WorkflowStatus.valueOf(workflow.getStatus()));
        response.setCreatedBy(workflow.getCreatedBy());
        response.setCreatedAt(workflow.getCreatedAt());
        response.setUpdatedBy(workflow.getUpdatedBy());
        response.setUpdatedAt(workflow.getUpdatedAt());
        return response;
    }

    private WorkflowResponse convertToResponseWithDetails(WorkflowEntity workflow) {
        WorkflowResponse response = convertToResponse(workflow);

        // 获取节点
        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflow.getId());
        response.setNodes(nodes.stream().map(this::convertToNodeDTO).collect(Collectors.toList()));

        // 构建 ID -> UUID 映射
        Map<String, String> nodeIdToUuidMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeEntity::getId, WorkflowNodeEntity::getNodeUuid));

        // 获取连线
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflow.getId());
        response.setConnections(connections.stream()
                .map(conn -> convertToConnectionDTO(conn, nodeIdToUuidMap))
                .collect(Collectors.toList()));

        // 获取关联
        List<WorkflowAssociationEntity> associations = associationMapper.selectByWorkflowId(workflow.getId());
        response.setAssociations(associations.stream()
                .map(assoc -> convertToAssociationDTO(assoc, nodeIdToUuidMap))
                .collect(Collectors.toList()));

        return response;
    }

    private WorkflowResponse.NodeDTO convertToNodeDTO(WorkflowNodeEntity node) {
        WorkflowResponse.NodeDTO dto = new WorkflowResponse.NodeDTO();
        dto.setId(node.getId());
        dto.setNodeUuid(node.getNodeUuid());
        dto.setType(node.getType());
        dto.setName(node.getName());
        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());
        dto.setInputPorts(node.getInputPorts());
        dto.setOutputPorts(node.getOutputPorts());
        dto.setInputParams(node.getInputParams());
        dto.setOutputParams(node.getOutputParams());
        dto.setConfig(node.getConfig());
        dto.setParentNodeId(node.getParentNodeId());
        // Skill节点字段
        dto.setSkillId(node.getSkillId());
        dto.setSkillSnapshot(node.getSkillSnapshot());
        dto.setNodeCategory(node.getNodeCategory());
        return dto;
    }

    private WorkflowResponse.ConnectionDTO convertToConnectionDTO(WorkflowConnectionEntity conn, Map<String, String> nodeIdToUuidMap) {
        WorkflowResponse.ConnectionDTO dto = new WorkflowResponse.ConnectionDTO();
        dto.setId(conn.getId());
        dto.setConnectionUuid(conn.getConnectionUuid());
        dto.setSourceNodeId(conn.getSourceNodeId());
        dto.setSourceNodeUuid(nodeIdToUuidMap.get(conn.getSourceNodeId()));
        dto.setSourcePortId(conn.getSourcePortId());
        dto.setTargetNodeId(conn.getTargetNodeId());
        dto.setTargetNodeUuid(nodeIdToUuidMap.get(conn.getTargetNodeId()));
        dto.setTargetPortId(conn.getTargetPortId());
        dto.setSourceParamIndex(conn.getSourceParamIndex());
        dto.setTargetParamIndex(conn.getTargetParamIndex());
        dto.setLabel(conn.getLabel());
        return dto;
    }

    private WorkflowResponse.AssociationDTO convertToAssociationDTO(WorkflowAssociationEntity assoc, Map<String, String> nodeIdToUuidMap) {
        WorkflowResponse.AssociationDTO dto = new WorkflowResponse.AssociationDTO();
        dto.setId(assoc.getId());
        dto.setContainerNodeId(assoc.getContainerNodeId());
        dto.setContainerNodeUuid(nodeIdToUuidMap.get(assoc.getContainerNodeId()));
        dto.setBodyNodeId(assoc.getBodyNodeId());
        dto.setBodyNodeUuid(nodeIdToUuidMap.get(assoc.getBodyNodeId()));
        dto.setAssociationType(assoc.getAssociationType());
        return dto;
    }

    private org.springframework.data.domain.Page<WorkflowResponse> convertToSpringPage(IPage<WorkflowEntity> mybatisPage) {
        List<WorkflowResponse> content = mybatisPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(
                        (int) mybatisPage.getCurrent() - 1,
                        (int) mybatisPage.getSize()
                ),
                mybatisPage.getTotal()
        );
    }
}
