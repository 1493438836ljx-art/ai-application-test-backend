package com.example.demo.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.workflow.dto.*;
import com.example.demo.workflow.entity.*;
import com.example.demo.workflow.mapper.*;
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

import org.springframework.data.domain.PageImpl;

/**
 * 工作流服务实现类 (MyBatis-Plus版本)
 *
 * @author AI Test Platform Team
 * @version 1.0.0
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
        log.info("创建工作流: {}, 节点数: {}, 连线数: {}, 关联数: {}",
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
        Long workflowId = workflow.getId();

        // 2. 保存节点数据
        Map<String, Long> nodeUuidToIdMap = new HashMap<>();
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
                    Long nodeId = nodeUuidToIdMap.get(nodeData.getNodeUuid());
                    Long parentNodeId = nodeUuidToIdMap.get(nodeData.getParentNodeUuid());
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
                Long sourceNodeId = nodeUuidToIdMap.get(connData.getSourceNodeUuid());
                Long targetNodeId = nodeUuidToIdMap.get(connData.getTargetNodeUuid());

                if (sourceNodeId == null || targetNodeId == null) {
                    log.warn("连线节点ID映射失败: sourceUuid={}, targetUuid={}",
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
                Long loopNodeId = nodeUuidToIdMap.get(assocData.getLoopNodeUuid());
                Long bodyNodeId = nodeUuidToIdMap.get(assocData.getBodyNodeUuid());

                if (loopNodeId == null || bodyNodeId == null) {
                    log.warn("关联节点ID映射失败: loopNodeUuid={}, bodyNodeUuid={}",
                            assocData.getLoopNodeUuid(), assocData.getBodyNodeUuid());
                    continue;
                }

                WorkflowAssociationEntity association = new WorkflowAssociationEntity();
                association.setWorkflowId(workflowId);
                association.setLoopNodeId(loopNodeId);
                association.setBodyNodeId(bodyNodeId);
                association.setAssociationType(assocData.getAssociationType() != null ?
                        assocData.getAssociationType() : "LOOP");

                associationMapper.insert(association);
            }
        }

        log.info("工作流创建成功: ID={}", workflowId);
        return getWorkflowById(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(Long id) {
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
    public WorkflowResponse updateWorkflow(Long id, WorkflowUpdateRequest request) {
        log.info("更新工作流: {}", id);

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
    public void deleteWorkflow(Long id) {
        log.info("删除工作流: {}", id);
        workflowMapper.deleteById(id);
    }

    @Override
    @Transactional
    public WorkflowResponse publishWorkflow(Long id) {
        log.info("发布工作流: {}", id);

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
    public WorkflowResponse copyWorkflow(Long id) {
        log.info("复制工作流: {}", id);

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
        Map<Long, Long> oldToNewNodeIdMap = new HashMap<>();

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
            Long newSourceNodeId = oldToNewNodeIdMap.get(originalConn.getSourceNodeId());
            Long newTargetNodeId = oldToNewNodeIdMap.get(originalConn.getTargetNodeId());

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
    public WorkflowResponse saveWorkflowData(Long id,
                                              List<WorkflowResponse.NodeDTO> nodes,
                                              List<WorkflowResponse.ConnectionDTO> connections,
                                              List<WorkflowResponse.AssociationDTO> associations) {
        log.info("保存工作流数据: {}, 节点数: {}, 连线数: {}, 关联数: {}",
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
        // uuidToIdMap: key 是前端 nodeUuid（字符串），value 是数据库 ID（Long）
        Map<String, Long> uuidToIdMap = new HashMap<>();
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
            nodeMapper.insert(node);
            // 使用 nodeUuid 作为 key
            uuidToIdMap.put(nodeDTO.getNodeUuid(), node.getId());
        }

        // 保存连线
        for (WorkflowResponse.ConnectionDTO connDTO : connections) {
            // 使用 nodeUuid 映射到数据库 ID
            Long sourceNodeId = uuidToIdMap.get(connDTO.getSourceNodeUuid());
            Long targetNodeId = uuidToIdMap.get(connDTO.getTargetNodeUuid());

            // 如果没有找到，尝试使用 sourceNodeId/targetNodeId（兼容旧格式）
            if (sourceNodeId == null && connDTO.getSourceNodeId() != null) {
                sourceNodeId = uuidToIdMap.get(String.valueOf(connDTO.getSourceNodeId()));
            }
            if (targetNodeId == null && connDTO.getTargetNodeId() != null) {
                targetNodeId = uuidToIdMap.get(String.valueOf(connDTO.getTargetNodeId()));
            }

            if (sourceNodeId == null || targetNodeId == null) {
                log.warn("连线节点ID映射失败: sourceUuid={}, targetUuid={}",
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
            // 使用 loopNodeUuid/bodyNodeUuid 映射到数据库 ID
            Long loopNodeDbId = uuidToIdMap.get(assocDTO.getLoopNodeUuid());
            Long bodyNodeDbId = uuidToIdMap.get(assocDTO.getBodyNodeUuid());

            if (loopNodeDbId == null || bodyNodeDbId == null) {
                log.warn("关联节点ID映射失败: loopNodeUuid={}, bodyNodeUuid={}",
                        assocDTO.getLoopNodeUuid(), assocDTO.getBodyNodeUuid());
                continue;
            }

            WorkflowAssociationEntity association = new WorkflowAssociationEntity();
            association.setWorkflowId(id);
            association.setLoopNodeId(loopNodeDbId);
            association.setBodyNodeId(bodyNodeDbId);
            association.setAssociationType(assocDTO.getAssociationType());
            associationMapper.insert(association);
        }

        return getWorkflowById(id);
    }

    @Override
    public WorkflowResponse getDefaultWorkflow() {
        log.info("获取默认工作流详情");

        WorkflowResponse response = new WorkflowResponse();
        response.setId(0L);
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
        startNode.setId(1L);
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

        // 2. textClean (文本清洗) - 位置(400, 250)
        WorkflowResponse.NodeDTO textCleanNode = new WorkflowResponse.NodeDTO();
        textCleanNode.setId(2L);
        textCleanNode.setNodeUuid("node-text-clean");
        textCleanNode.setType("textClean");
        textCleanNode.setName("文本清洗");
        textCleanNode.setPositionX(400);
        textCleanNode.setPositionY(250);
        textCleanNode.setInputPorts("[{\"id\":\"input-1\",\"name\":\"输入\"}]");
        textCleanNode.setOutputPorts("[{\"id\":\"output-1\",\"name\":\"输出\"}]");
        textCleanNode.setInputParams("[]");
        textCleanNode.setOutputParams("[]");
        textCleanNode.setConfig("{}");
        textCleanNode.setParentNodeId(null);
        nodes.add(textCleanNode);

        // 3. tableExtract (表格提取) - 位置(525, 250) - 在文本清洗和循环节点之间
        WorkflowResponse.NodeDTO tableExtractNode = new WorkflowResponse.NodeDTO();
        tableExtractNode.setId(3L);
        tableExtractNode.setNodeUuid("node-table-extract");
        tableExtractNode.setType("tableExtract");
        tableExtractNode.setName("表格提取");
        tableExtractNode.setPositionX(525);
        tableExtractNode.setPositionY(250);
        tableExtractNode.setInputPorts("[{\"id\":\"input-1\",\"name\":\"输入\"}]");
        tableExtractNode.setOutputPorts("[{\"id\":\"output-1\",\"name\":\"输出\"}]");
        tableExtractNode.setInputParams("[{\"name\":\"file\",\"type\":\"File\",\"fileType\":\"Excel\",\"required\":true,\"description\":\"需要提取数据的Excel文件\"}]");
        tableExtractNode.setOutputParams("[{\"name\":\"output\",\"type\":\"Array\",\"elementType\":\"Object\",\"description\":\"提取的表格数据数组\"}]");
        tableExtractNode.setConfig("{\"file\":null,\"sheetName\":\"\",\"headerRow\":1,\"startRow\":2,\"endRow\":null,\"columns\":[]}");
        tableExtractNode.setParentNodeId(null);
        nodes.add(tableExtractNode);

        // 4. loop (循环) - 位置(775, 250)
        WorkflowResponse.NodeDTO loopNode = new WorkflowResponse.NodeDTO();
        loopNode.setId(4L);
        loopNode.setNodeUuid("node-loop");
        loopNode.setType("loop");
        loopNode.setName("循环");
        loopNode.setPositionX(775);
        loopNode.setPositionY(250);
        loopNode.setInputPorts("[{\"id\":\"input-1\",\"name\":\"输入\"}]");
        loopNode.setOutputPorts("[{\"id\":\"output-1\",\"name\":\"输出\"}]");
        loopNode.setInputParams("[]");
        loopNode.setOutputParams("[]");
        loopNode.setConfig("{}");
        loopNode.setParentNodeId(null);
        nodes.add(loopNode);

        // 5. judgeModel (裁判模型) - 位置(1025, 250)
        WorkflowResponse.NodeDTO judgeModelNode = new WorkflowResponse.NodeDTO();
        judgeModelNode.setId(5L);
        judgeModelNode.setNodeUuid("node-judge-model");
        judgeModelNode.setType("judgeModel");
        judgeModelNode.setName("裁判模型");
        judgeModelNode.setPositionX(1025);
        judgeModelNode.setPositionY(250);
        judgeModelNode.setInputPorts("[{\"id\":\"input-1\",\"name\":\"输入\"}]");
        judgeModelNode.setOutputPorts("[{\"id\":\"output-1\",\"name\":\"输出\"}]");
        judgeModelNode.setInputParams("[]");
        judgeModelNode.setOutputParams("[]");
        judgeModelNode.setConfig("{}");
        judgeModelNode.setParentNodeId(null);
        nodes.add(judgeModelNode);

        // 6. tableGenerate (表格生成) - 位置(1275, 250)
        WorkflowResponse.NodeDTO tableGenerateNode = new WorkflowResponse.NodeDTO();
        tableGenerateNode.setId(6L);
        tableGenerateNode.setNodeUuid("node-table-generate");
        tableGenerateNode.setType("tableGenerate");
        tableGenerateNode.setName("表格生成");
        tableGenerateNode.setPositionX(1275);
        tableGenerateNode.setPositionY(250);
        tableGenerateNode.setInputPorts("[{\"id\":\"input-1\",\"name\":\"输入\"}]");
        tableGenerateNode.setOutputPorts("[{\"id\":\"output-1\",\"name\":\"输出\"}]");
        tableGenerateNode.setInputParams("[]");
        tableGenerateNode.setOutputParams("[]");
        tableGenerateNode.setConfig("{}");
        tableGenerateNode.setParentNodeId(null);
        nodes.add(tableGenerateNode);

        // 7. end (结束) - 位置(1525, 250)
        WorkflowResponse.NodeDTO endNode = new WorkflowResponse.NodeDTO();
        endNode.setId(7L);
        endNode.setNodeUuid("node-end");
        endNode.setType("end");
        endNode.setName("结束");
        endNode.setPositionX(1525);
        endNode.setPositionY(250);
        endNode.setInputPorts("[{\"id\":\"input-1\",\"name\":\"输入\"}]");
        endNode.setOutputPorts("[]");
        endNode.setInputParams("[]");
        endNode.setOutputParams("[]");
        endNode.setConfig("{}");
        endNode.setParentNodeId(null);
        nodes.add(endNode);

        // 8. loopBodyCanvas (循环体) - 位置(775, 470)，宽度500，高度400
        WorkflowResponse.NodeDTO loopBodyNode = new WorkflowResponse.NodeDTO();
        loopBodyNode.setId(8L);
        loopBodyNode.setNodeUuid("node-loop-body");
        loopBodyNode.setType("loopBodyCanvas");
        loopBodyNode.setName("循环体");
        loopBodyNode.setPositionX(775);
        loopBodyNode.setPositionY(470);
        loopBodyNode.setInputPorts("[]");
        loopBodyNode.setOutputPorts("[]");
        loopBodyNode.setInputParams("[]");
        loopBodyNode.setOutputParams("[]");
        // 循环体配置：包含内部画布信息，包括一个默认的HTTP接口调用节点和连线
        loopBodyNode.setConfig("{\"width\":500,\"height\":400,\"belongsTo\":\"node-loop\",\"loopBody\":{\"canvas\":{\"scale\":1,\"offsetX\":0,\"offsetY\":0},\"nodes\":[{\"id\":\"node-api-auto-1\",\"type\":\"apiAuto\",\"name\":\"HTTPS/HTTP接口调用\",\"x\":200,\"y\":150,\"inputs\":[{\"id\":\"input-1\",\"name\":\"输入\"}],\"outputs\":[{\"id\":\"output-1\",\"name\":\"输出\"}],\"config\":{\"url\":\"\",\"method\":\"GET\",\"headers\":[],\"body\":\"\",\"timeout\":30000}}],\"connections\":[{\"id\":\"conn-left-to-api\",\"sourceId\":\"port-left\",\"sourcePort\":\"out\",\"targetId\":\"node-api-auto-1\",\"targetPort\":\"input-1\"},{\"id\":\"conn-api-to-right\",\"sourceId\":\"node-api-auto-1\",\"sourcePort\":\"output-1\",\"targetId\":\"port-right\",\"targetPort\":\"in\"}],\"leftPort\":{\"id\":\"port-left\",\"name\":\"输入\",\"type\":\"input\",\"y\":200,\"params\":[]},\"rightPort\":{\"id\":\"port-right\",\"name\":\"输出\",\"type\":\"output\",\"y\":200,\"params\":[]}}}");
        loopBodyNode.setParentNodeId(null);
        nodes.add(loopBodyNode);

        return nodes;
    }

    private List<WorkflowResponse.ConnectionDTO> createDefaultConnections(List<WorkflowResponse.NodeDTO> nodes) {
        List<WorkflowResponse.ConnectionDTO> connections = new java.util.ArrayList<>();

        // 节点ID到UUID的映射
        Map<Long, String> nodeIdToUuidMap = new java.util.HashMap<>();
        for (WorkflowResponse.NodeDTO node : nodes) {
            nodeIdToUuidMap.put(node.getId(), node.getNodeUuid());
        }

        // 节点ID映射：start(1), textClean(2), tableExtract(3), loop(4), judgeModel(5), tableGenerate(6), end(7)
        // 循环体节点ID：loopBody(8)

        // 1. start -> textClean
        WorkflowResponse.ConnectionDTO conn1 = new WorkflowResponse.ConnectionDTO();
        conn1.setId(1L);
        conn1.setConnectionUuid("conn-start-textclean");
        conn1.setSourceNodeId(1L);
        conn1.setSourceNodeUuid(nodeIdToUuidMap.get(1L));
        conn1.setSourcePortId("output-1");
        conn1.setTargetNodeId(2L);
        conn1.setTargetNodeUuid(nodeIdToUuidMap.get(2L));
        conn1.setTargetPortId("input-1");
        conn1.setSourceParamIndex(null);
        conn1.setTargetParamIndex(null);
        conn1.setLabel(null);
        connections.add(conn1);

        // 2. textClean -> tableExtract
        WorkflowResponse.ConnectionDTO conn2 = new WorkflowResponse.ConnectionDTO();
        conn2.setId(2L);
        conn2.setConnectionUuid("conn-textclean-tableextract");
        conn2.setSourceNodeId(2L);
        conn2.setSourceNodeUuid(nodeIdToUuidMap.get(2L));
        conn2.setSourcePortId("output-1");
        conn2.setTargetNodeId(3L);
        conn2.setTargetNodeUuid(nodeIdToUuidMap.get(3L));
        conn2.setTargetPortId("input-1");
        conn2.setSourceParamIndex(null);
        conn2.setTargetParamIndex(null);
        conn2.setLabel(null);
        connections.add(conn2);

        // 3. tableExtract -> loop
        WorkflowResponse.ConnectionDTO conn3 = new WorkflowResponse.ConnectionDTO();
        conn3.setId(3L);
        conn3.setConnectionUuid("conn-tableextract-loop");
        conn3.setSourceNodeId(3L);
        conn3.setSourceNodeUuid(nodeIdToUuidMap.get(3L));
        conn3.setSourcePortId("output-1");
        conn3.setTargetNodeId(4L);
        conn3.setTargetNodeUuid(nodeIdToUuidMap.get(4L));
        conn3.setTargetPortId("input-1");
        conn3.setSourceParamIndex(null);
        conn3.setTargetParamIndex(null);
        conn3.setLabel(null);
        connections.add(conn3);

        // 4. loop -> judgeModel
        WorkflowResponse.ConnectionDTO conn4 = new WorkflowResponse.ConnectionDTO();
        conn4.setId(4L);
        conn4.setConnectionUuid("conn-loop-judgemodel");
        conn4.setSourceNodeId(4L);
        conn4.setSourceNodeUuid(nodeIdToUuidMap.get(4L));
        conn4.setSourcePortId("output-1");
        conn4.setTargetNodeId(5L);
        conn4.setTargetNodeUuid(nodeIdToUuidMap.get(5L));
        conn4.setTargetPortId("input-1");
        conn4.setSourceParamIndex(null);
        conn4.setTargetParamIndex(null);
        conn4.setLabel(null);
        connections.add(conn4);

        // 5. judgeModel -> tableGenerate
        WorkflowResponse.ConnectionDTO conn5 = new WorkflowResponse.ConnectionDTO();
        conn5.setId(5L);
        conn5.setConnectionUuid("conn-judgemodel-tablegenerate");
        conn5.setSourceNodeId(5L);
        conn5.setSourceNodeUuid(nodeIdToUuidMap.get(5L));
        conn5.setSourcePortId("output-1");
        conn5.setTargetNodeId(6L);
        conn5.setTargetNodeUuid(nodeIdToUuidMap.get(6L));
        conn5.setTargetPortId("input-1");
        conn5.setSourceParamIndex(null);
        conn5.setTargetParamIndex(null);
        conn5.setLabel(null);
        connections.add(conn5);

        // 6. tableGenerate -> end
        WorkflowResponse.ConnectionDTO conn6 = new WorkflowResponse.ConnectionDTO();
        conn6.setId(6L);
        conn6.setConnectionUuid("conn-tablegenerate-end");
        conn6.setSourceNodeId(6L);
        conn6.setSourceNodeUuid(nodeIdToUuidMap.get(6L));
        conn6.setSourcePortId("output-1");
        conn6.setTargetNodeId(7L);
        conn6.setTargetNodeUuid(nodeIdToUuidMap.get(7L));
        conn6.setTargetPortId("input-1");
        conn6.setSourceParamIndex(null);
        conn6.setTargetParamIndex(null);
        conn6.setLabel(null);
        connections.add(conn6);

        return connections;
    }

    private List<WorkflowResponse.AssociationDTO> createDefaultAssociations() {
        List<WorkflowResponse.AssociationDTO> associations = new java.util.ArrayList<>();

        // 循环节点(ID=4, UUID="node-loop") 与 循环体节点(ID=8, UUID="node-loop-body") 的关联
        WorkflowResponse.AssociationDTO association = new WorkflowResponse.AssociationDTO();
        association.setId(1L);
        association.setLoopNodeId(4L);
        association.setLoopNodeUuid("node-loop");
        association.setBodyNodeId(8L);
        association.setBodyNodeUuid("node-loop-body");
        association.setAssociationType("loop-body");
        associations.add(association);

        return associations;
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
        Map<Long, String> nodeIdToUuidMap = nodes.stream()
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
        return dto;
    }

    private WorkflowResponse.ConnectionDTO convertToConnectionDTO(WorkflowConnectionEntity conn, Map<Long, String> nodeIdToUuidMap) {
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

    private WorkflowResponse.AssociationDTO convertToAssociationDTO(WorkflowAssociationEntity assoc, Map<Long, String> nodeIdToUuidMap) {
        WorkflowResponse.AssociationDTO dto = new WorkflowResponse.AssociationDTO();
        dto.setId(assoc.getId());
        dto.setLoopNodeId(assoc.getLoopNodeId());
        dto.setLoopNodeUuid(nodeIdToUuidMap.get(assoc.getLoopNodeId()));
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
