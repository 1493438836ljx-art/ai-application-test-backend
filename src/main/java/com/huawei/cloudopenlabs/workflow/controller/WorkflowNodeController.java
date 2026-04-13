/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.controller;

import com.huawei.cloudopenlabs.workflow.dto.NodeCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.NodeResponse;
import com.huawei.cloudopenlabs.workflow.dto.NodeUpdateRequest;
import com.huawei.cloudopenlabs.workflow.service.WorkflowNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流节点控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/{workflowId}/nodes")
@RequiredArgsConstructor
public class WorkflowNodeController {

    private final WorkflowNodeService nodeService;

    /**
     * 获取工作流的所有节点
     *
     * @param workflowId 工作流ID
     * @return 节点列表
     */
    @GetMapping
    public ResponseEntity<List<NodeResponse>> getNodes(
            @PathVariable String workflowId) {
        log.info("Getting workflow node list: workflowId={}", workflowId);
        List<NodeResponse> nodes = nodeService.getNodes(workflowId);
        return ResponseEntity.ok(nodes);
    }

    /**
     * 获取单个节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 节点响应
     */
    @GetMapping("/{nodeUuid}")
    public ResponseEntity<NodeResponse> getNode(
            @PathVariable String workflowId,
            @PathVariable String nodeUuid) {
        log.info("Getting node details: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
        NodeResponse response = nodeService.getNode(workflowId, nodeUuid);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建节点
     *
     * @param workflowId 工作流ID
     * @param request    创建请求
     * @return 节点响应
     */
    @PostMapping
    public ResponseEntity<NodeResponse> createNode(
            @PathVariable String workflowId,
            @Valid @RequestBody NodeCreateRequest request) {
        log.info("Creating node: workflowId={}, nodeName={}", workflowId, request.getName());
        NodeResponse response = nodeService.createNode(workflowId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 批量创建节点
     *
     * @param workflowId 工作流ID
     * @param requests   创建请求列表
     * @return 无内容响应
     */
    @PostMapping("/batch")
    public ResponseEntity<Void> batchCreateNodes(
            @PathVariable String workflowId,
            @Valid @RequestBody List<NodeCreateRequest> requests) {
        log.info("Batch creating nodes: workflowId={}, count={}", workflowId, requests.size());
        nodeService.batchCreateNodes(workflowId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 更新节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @param request    更新请求
     * @return 节点响应
     */
    @PutMapping("/{nodeUuid}")
    public ResponseEntity<NodeResponse> updateNode(
            @PathVariable String workflowId,
            @PathVariable String nodeUuid,
            @Valid @RequestBody NodeUpdateRequest request) {
        log.info("Updating node: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
        NodeResponse response = nodeService.updateNode(workflowId, nodeUuid, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 批量更新节点
     *
     * @param workflowId 工作流ID
     * @param requests   更新请求列表
     * @return 无内容响应
     */
    @PutMapping("/batch")
    public ResponseEntity<Void> batchUpdateNodes(
            @PathVariable String workflowId,
            @Valid @RequestBody List<NodeUpdateRequest> requests) {
        log.info("Batch updating nodes: workflowId={}, count={}", workflowId, requests.size());
        nodeService.batchUpdateNodes(workflowId, requests);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 无内容响应
     */
    @DeleteMapping("/{nodeUuid}")
    public ResponseEntity<Void> deleteNode(
            @PathVariable String workflowId,
            @PathVariable String nodeUuid) {
        log.info("Deleting node: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
        nodeService.deleteNode(workflowId, nodeUuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量删除节点
     *
     * @param workflowId 工作流ID
     * @param nodeUuids  节点UUID列表
     * @return 无内容响应
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDeleteNodes(
            @PathVariable String workflowId,
            @RequestBody List<String> nodeUuids) {
        log.info("Batch deleting nodes: workflowId={}, count={}", workflowId, nodeUuids.size());
        nodeService.batchDeleteNodes(workflowId, nodeUuids);
        return ResponseEntity.noContent().build();
    }

    /**
     * 根据Skill ID查询节点
     *
     * @param skillId Skill ID
     * @return 节点列表
     */
    @GetMapping("/by-skill/{skillId}")
    public ResponseEntity<List<NodeResponse>> getNodesBySkillId(
            @PathVariable String skillId) {
        log.info("Querying nodes by skill: skillId={}", skillId);
        List<NodeResponse> nodes = nodeService.getNodesBySkillId(skillId);
        return ResponseEntity.ok(nodes);
    }
}
