package com.example.demo.workflow.service.impl;

import com.example.demo.workflow.dto.ConnectionResponse;
import com.example.demo.workflow.dto.WorkflowResponse;
import com.example.demo.workflow.entity.WorkflowConnectionEntity;
import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.mapper.WorkflowConnectionMapper;
import com.example.demo.workflow.mapper.WorkflowNodeMapper;
import com.example.demo.workflow.service.WorkflowConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 工作流连线服务实现
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowConnectionServiceImpl implements WorkflowConnectionService {

    private final WorkflowConnectionMapper connectionMapper;
    private final WorkflowNodeMapper nodeMapper;

    // ========== CRUD ==========

    @Override
    public List<ConnectionResponse> getConnections(Long workflowId) {
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);
        return connections.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ConnectionResponse getConnection(Long workflowId, String connectionUuid) {
        // TODO: 需要添加按UUID查询的方法
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);
        WorkflowConnectionEntity connection = connections.stream()
                .filter(c -> connectionUuid.equals(c.getConnectionUuid()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("连线不存在: " + connectionUuid));
        return convertToResponse(connection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectionResponse createConnection(Long workflowId, WorkflowResponse.ConnectionDTO request) {
        // 获取源节点和目标节点的数据库ID
        Map<String, Long> uuidToIdMap = getUuidToIdMap(workflowId);

        Long sourceNodeId = uuidToIdMap.get(request.getSourceNodeUuid());
        Long targetNodeId = uuidToIdMap.get(request.getTargetNodeUuid());

        if (sourceNodeId == null) {
            throw new IllegalArgumentException("源节点不存在: " + request.getSourceNodeUuid());
        }
        if (targetNodeId == null) {
            throw new IllegalArgumentException("目标节点不存在: " + request.getTargetNodeUuid());
        }

        // 校验不能自连接
        if (sourceNodeId.equals(targetNodeId)) {
            throw new IllegalArgumentException("不能创建自连接");
        }

        // 创建连线实体
        WorkflowConnectionEntity connection = new WorkflowConnectionEntity();
        connection.setWorkflowId(workflowId);
        connection.setConnectionUuid(request.getConnectionUuid() != null ?
                request.getConnectionUuid() : UUID.randomUUID().toString());
        connection.setSourceNodeId(sourceNodeId);
        connection.setSourcePortId(request.getSourcePortId());
        connection.setTargetNodeId(targetNodeId);
        connection.setTargetPortId(request.getTargetPortId());
        connection.setSourceParamIndex(request.getSourceParamIndex());
        connection.setTargetParamIndex(request.getTargetParamIndex());
        connection.setLabel(request.getLabel());
        connection.setBranchLabel(request.getBranchLabel());
        connection.setBranchPriority(request.getBranchPriority());

        connectionMapper.insert(connection);
        log.info("创建连线成功: workflowId={}, connectionUuid={}", workflowId, connection.getConnectionUuid());

        return convertToResponse(connection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConnection(Long workflowId, String connectionUuid) {
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);
        WorkflowConnectionEntity connection = connections.stream()
                .filter(c -> connectionUuid.equals(c.getConnectionUuid()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("连线不存在: " + connectionUuid));

        connectionMapper.deleteById(connection.getId());
        log.info("删除连线成功: workflowId={}, connectionUuid={}", workflowId, connectionUuid);
    }

    // ========== 批量操作 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateConnections(Long workflowId, List<WorkflowResponse.ConnectionDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (WorkflowResponse.ConnectionDTO request : requests) {
            createConnection(workflowId, request);
        }
        log.info("批量创建连线成功: workflowId={}, count={}", workflowId, requests.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteConnections(Long workflowId, List<String> connectionUuids) {
        if (connectionUuids == null || connectionUuids.isEmpty()) {
            return;
        }
        for (String connectionUuid : connectionUuids) {
            deleteConnection(workflowId, connectionUuid);
        }
        log.info("批量删除连线成功: workflowId={}, count={}", workflowId, connectionUuids.size());
    }

    // ========== 查询 ==========

    @Override
    public List<ConnectionResponse> getConnectionsBySourceNode(Long nodeId) {
        List<WorkflowConnectionEntity> connections = connectionMapper.selectBySourceNodeId(nodeId);
        return connections.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConnectionResponse> getConnectionsByTargetNode(Long nodeId) {
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByTargetNodeId(nodeId);
        return connections.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 获取节点UUID到数据库ID的映射
     */
    private Map<String, Long> getUuidToIdMap(Long workflowId) {
        return nodeMapper.selectByWorkflowId(workflowId).stream()
                .collect(Collectors.toMap(
                        WorkflowNodeEntity::getNodeUuid,
                        WorkflowNodeEntity::getId,
                        (a, b) -> a
                ));
    }

    /**
     * 获取节点ID到UUID的映射
     */
    private Map<Long, String> getIdToUuidMap(Long workflowId) {
        return nodeMapper.selectByWorkflowId(workflowId).stream()
                .collect(Collectors.toMap(
                        WorkflowNodeEntity::getId,
                        WorkflowNodeEntity::getNodeUuid,
                        (a, b) -> a
                ));
    }

    /**
     * 转换为响应DTO
     */
    private ConnectionResponse convertToResponse(WorkflowConnectionEntity entity) {
        ConnectionResponse response = new ConnectionResponse();
        response.setId(entity.getId());
        response.setConnectionUuid(entity.getConnectionUuid());
        response.setSourcePort(entity.getSourcePortId());
        response.setTargetPort(entity.getTargetPortId());
        response.setBranchLabel(entity.getBranchLabel());
        response.setBranchPriority(entity.getBranchPriority());
        response.setCreatedAt(entity.getCreatedAt());

        // 需要通过节点ID查询UUID
        // 这里简化处理，在批量查询时通过映射设置
        return response;
    }

    /**
     * 转换为响应DTO（带UUID映射）
     */
    public ConnectionResponse convertToResponse(WorkflowConnectionEntity entity, Map<Long, String> idToUuidMap) {
        ConnectionResponse response = convertToResponse(entity);
        response.setSourceNodeUuid(idToUuidMap.get(entity.getSourceNodeId()));
        response.setTargetNodeUuid(idToUuidMap.get(entity.getTargetNodeId()));
        return response;
    }
}
