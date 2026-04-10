/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.service.impl;

import com.huawei.cloudopenlabs.workflow.dto.AssociationCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.AssociationResponse;
import com.huawei.cloudopenlabs.workflow.entity.AssociationType;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowAssociationEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowAssociationMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowNodeMapper;
import com.huawei.cloudopenlabs.workflow.service.WorkflowAssociationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流关联服务实现
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAssociationServiceImpl implements WorkflowAssociationService {

    private final WorkflowAssociationMapper associationMapper;
    private final WorkflowNodeMapper nodeMapper;

    // ========== CRUD ==========

    @Override
    public List<AssociationResponse> getAssociations(String workflowId) {
        List<WorkflowAssociationEntity> associations = associationMapper.selectByWorkflowId(workflowId);
        Map<String, String> idToUuidMap = getIdToUuidMap(workflowId);
        return associations.stream()
                .map(assoc -> convertToResponse(assoc, idToUuidMap))
                .collect(Collectors.toList());
    }

    @Override
    public AssociationResponse getAssociation(String workflowId, String associationId) {
        // TODO: 需要添加按ID查询的方法
        List<WorkflowAssociationEntity> associations = associationMapper.selectByWorkflowId(workflowId);
        WorkflowAssociationEntity association = associations.stream()
                .filter(a -> associationId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("关联不存在: " + associationId));

        Map<String, String> idToUuidMap = getIdToUuidMap(workflowId);
        return convertToResponse(association, idToUuidMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssociationResponse createAssociation(String workflowId, AssociationCreateRequest request) {
        // 获取节点UUID到ID的映射
        Map<String, String> uuidToIdMap = getUuidToIdMap(workflowId);

        String containerNodeId = uuidToIdMap.get(request.getContainerNodeUuid());
        String bodyNodeId = uuidToIdMap.get(request.getBodyNodeUuid());

        if (containerNodeId == null) {
            throw new IllegalArgumentException("容器节点不存在: " + request.getContainerNodeUuid());
        }
        if (bodyNodeId == null) {
            throw new IllegalArgumentException("子节点不存在: " + request.getBodyNodeUuid());
        }

        // 校验关联类型
        String associationType = request.getAssociationType();
        if (associationType == null) {
            associationType = AssociationType.LOOP_BODY.name();
        }
        validateAssociationType(associationType);

        // 创建关联实体
        WorkflowAssociationEntity association = new WorkflowAssociationEntity();
        association.setWorkflowId(workflowId);
        association.setContainerNodeId(containerNodeId);
        association.setContainerNodeUuid(request.getContainerNodeUuid());
        association.setBodyNodeId(bodyNodeId);
        association.setBodyNodeUuid(request.getBodyNodeUuid());
        association.setAssociationType(associationType);

        associationMapper.insert(association);
        log.info("创建关联成功: workflowId={}, containerNodeUuid={}, bodyNodeUuid={}",
                workflowId, request.getContainerNodeUuid(), request.getBodyNodeUuid());

        Map<String, String> idToUuidMap = getIdToUuidMap(workflowId);
        return convertToResponse(association, idToUuidMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssociation(String workflowId, String associationId) {
        // 验证关联存在
        List<WorkflowAssociationEntity> associations = associationMapper.selectByWorkflowId(workflowId);
        WorkflowAssociationEntity association = associations.stream()
                .filter(a -> associationId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("关联不存在: " + associationId));

        associationMapper.deleteById(associationId);
        log.info("删除关联成功: workflowId={}, associationId={}", workflowId, associationId);
    }

    // ========== 批量操作 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateAssociations(String workflowId, List<AssociationCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (AssociationCreateRequest request : requests) {
            createAssociation(workflowId, request);
        }
        log.info("批量创建关联成功: workflowId={}, count={}", workflowId, requests.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteAssociations(String workflowId, List<String> associationIds) {
        if (associationIds == null || associationIds.isEmpty()) {
            return;
        }
        for (String associationId : associationIds) {
            deleteAssociation(workflowId, associationId);
        }
        log.info("批量删除关联成功: workflowId={}, count={}", workflowId, associationIds.size());
    }

    // ========== 查询 ==========

    @Override
    public List<AssociationResponse> getByContainerNode(String workflowId, String containerNodeUuid) {
        Map<String, String> uuidToIdMap = getUuidToIdMap(workflowId);
        String containerNodeId = uuidToIdMap.get(containerNodeUuid);

        if (containerNodeId == null) {
            throw new IllegalArgumentException("容器节点不存在: " + containerNodeUuid);
        }

        // TODO: 需要添加按容器节点ID查询的方法
        List<WorkflowAssociationEntity> associations = associationMapper.selectByWorkflowId(workflowId);
        List<WorkflowAssociationEntity> filtered = associations.stream()
                .filter(a -> containerNodeId.equals(a.getContainerNodeId()))
                .collect(Collectors.toList());

        Map<String, String> idToUuidMap = getIdToUuidMap(workflowId);
        return filtered.stream()
                .map(assoc -> convertToResponse(assoc, idToUuidMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssociationResponse> getByBodyNode(String workflowId, String bodyNodeUuid) {
        Map<String, String> uuidToIdMap = getUuidToIdMap(workflowId);
        String bodyNodeId = uuidToIdMap.get(bodyNodeUuid);

        if (bodyNodeId == null) {
            throw new IllegalArgumentException("子节点不存在: " + bodyNodeUuid);
        }

        // TODO: 需要添加按子节点ID查询的方法
        List<WorkflowAssociationEntity> associations = associationMapper.selectByWorkflowId(workflowId);
        List<WorkflowAssociationEntity> filtered = associations.stream()
                .filter(a -> bodyNodeId.equals(a.getBodyNodeId()))
                .collect(Collectors.toList());

        Map<String, String> idToUuidMap = getIdToUuidMap(workflowId);
        return filtered.stream()
                .map(assoc -> convertToResponse(assoc, idToUuidMap))
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 获取节点UUID到数据库ID的映射
     */
    private Map<String, String> getUuidToIdMap(String workflowId) {
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
    private Map<String, String> getIdToUuidMap(String workflowId) {
        return nodeMapper.selectByWorkflowId(workflowId).stream()
                .collect(Collectors.toMap(
                        WorkflowNodeEntity::getId,
                        WorkflowNodeEntity::getNodeUuid,
                        (a, b) -> a
                ));
    }

    /**
     * 校验关联类型
     */
    private void validateAssociationType(String associationType) {
        try {
            AssociationType.valueOf(associationType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的关联类型: " + associationType);
        }
    }

    /**
     * 转换为响应DTO
     */
    private AssociationResponse convertToResponse(WorkflowAssociationEntity entity, Map<String, String> idToUuidMap) {
        AssociationResponse response = new AssociationResponse();
        response.setId(entity.getId());
        response.setContainerNodeUuid(entity.getContainerNodeUuid() != null ?
                entity.getContainerNodeUuid() : idToUuidMap.get(entity.getContainerNodeId()));
        response.setBodyNodeUuid(entity.getBodyNodeUuid() != null ?
                entity.getBodyNodeUuid() : idToUuidMap.get(entity.getBodyNodeId()));
        response.setAssociationType(entity.getAssociationType());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
