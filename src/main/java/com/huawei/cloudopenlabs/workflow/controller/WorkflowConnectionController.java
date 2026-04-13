/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.controller;

import com.huawei.cloudopenlabs.workflow.dto.ConnectionResponse;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowResponse;
import com.huawei.cloudopenlabs.workflow.service.WorkflowConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流连线控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/{workflowId}/connections")
@RequiredArgsConstructor
public class WorkflowConnectionController {

    private final WorkflowConnectionService connectionService;

    /**
     * 获取工作流的所有连线
     *
     * @param workflowId 工作流ID
     * @return 连线列表
     */
    @GetMapping
    public ResponseEntity<List<ConnectionResponse>> getConnections(
            @PathVariable String workflowId) {
        log.info("Getting workflow connection list: workflowId={}", workflowId);
        List<ConnectionResponse> connections = connectionService.getConnections(workflowId);
        return ResponseEntity.ok(connections);
    }

    /**
     * 获取单个连线
     *
     * @param workflowId      工作流ID
     * @param connectionUuid 连线UUID
     * @return 连线响应
     */
    @GetMapping("/{connectionUuid}")
    public ResponseEntity<ConnectionResponse> getConnection(
            @PathVariable String workflowId,
            @PathVariable String connectionUuid) {
        log.info("Getting connection details: workflowId={}, connectionUuid={}", workflowId, connectionUuid);
        ConnectionResponse response = connectionService.getConnection(workflowId, connectionUuid);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建连线
     *
     * @param workflowId 工作流ID
     * @param request    连线数据
     * @return 连线响应
     */
    @PostMapping
    public ResponseEntity<ConnectionResponse> createConnection(
            @PathVariable String workflowId,
            @Valid @RequestBody WorkflowResponse.ConnectionDTO request) {
        log.info("Creating connection: workflowId={}, sourceNode={}, targetNode={}",
                workflowId, request.getSourceNodeUuid(), request.getTargetNodeUuid());
        ConnectionResponse response = connectionService.createConnection(workflowId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 批量创建连线
     *
     * @param workflowId 工作流ID
     * @param requests   连线数据列表
     * @return 无内容响应
     */
    @PostMapping("/batch")
    public ResponseEntity<Void> batchCreateConnections(
            @PathVariable String workflowId,
            @Valid @RequestBody List<WorkflowResponse.ConnectionDTO> requests) {
        log.info("Batch creating connections: workflowId={}, count={}", workflowId, requests.size());
        connectionService.batchCreateConnections(workflowId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 删除连线
     *
     * @param workflowId      工作流ID
     * @param connectionUuid 连线UUID
     * @return 无内容响应
     */
    @DeleteMapping("/{connectionUuid}")
    public ResponseEntity<Void> deleteConnection(
            @PathVariable String workflowId,
            @PathVariable String connectionUuid) {
        log.info("Deleting connection: workflowId={}, connectionUuid={}", workflowId, connectionUuid);
        connectionService.deleteConnection(workflowId, connectionUuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量删除连线
     *
     * @param workflowId       工作流ID
     * @param connectionUuids 连线UUID列表
     * @return 无内容响应
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDeleteConnections(
            @PathVariable String workflowId,
            @RequestBody List<String> connectionUuids) {
        log.info("Batch deleting connections: workflowId={}, count={}", workflowId, connectionUuids.size());
        connectionService.batchDeleteConnections(workflowId, connectionUuids);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取节点的出边
     *
     * @param workflowId 工作流ID
     * @param nodeId     源节点ID
     * @return 连线列表
     */
    @GetMapping("/source/{nodeId}")
    public ResponseEntity<List<ConnectionResponse>> getConnectionsBySourceNode(
            @PathVariable String workflowId,
            @PathVariable String nodeId) {
        log.info("Getting node outgoing edges: workflowId={}, nodeId={}", workflowId, nodeId);
        List<ConnectionResponse> connections = connectionService.getConnectionsBySourceNode(nodeId);
        return ResponseEntity.ok(connections);
    }

    /**
     * 获取节点的入边
     *
     * @param workflowId 工作流ID
     * @param nodeId     目标节点ID
     * @return 连线列表
     */
    @GetMapping("/target/{nodeId}")
    public ResponseEntity<List<ConnectionResponse>> getConnectionsByTargetNode(
            @PathVariable String workflowId,
            @PathVariable String nodeId) {
        log.info("Getting node incoming edges: workflowId={}, nodeId={}", workflowId, nodeId);
        List<ConnectionResponse> connections = connectionService.getConnectionsByTargetNode(nodeId);
        return ResponseEntity.ok(connections);
    }
}
