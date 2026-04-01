package com.example.demo.workflow.controller;

import com.example.demo.workflow.dto.ConnectionResponse;
import com.example.demo.workflow.dto.WorkflowResponse;
import com.example.demo.workflow.service.WorkflowConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/{workflowId}/connections")
@RequiredArgsConstructor
@Tag(name = "工作流连线管理", description = "工作流连线的增删改查接口")
public class WorkflowConnectionController {

    private final WorkflowConnectionService connectionService;

    /**
     * 获取工作流的所有连线
     *
     * @param workflowId 工作流ID
     * @return 连线列表
     */
    @GetMapping
    @Operation(summary = "获取连线列表", description = "获取指定工作流的所有连线")
    public ResponseEntity<List<ConnectionResponse>> getConnections(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId) {
        log.info("获取工作流连线列表: workflowId={}", workflowId);
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
    @Operation(summary = "获取连线详情", description = "根据UUID获取连线的详细信息")
    public ResponseEntity<ConnectionResponse> getConnection(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "连线UUID", required = true)
            @PathVariable String connectionUuid) {
        log.info("获取连线详情: workflowId={}, connectionUuid={}", workflowId, connectionUuid);
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
    @Operation(summary = "创建连线", description = "在工作流中创建一条新的连线")
    public ResponseEntity<ConnectionResponse> createConnection(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Valid @RequestBody WorkflowResponse.ConnectionDTO request) {
        log.info("创建连线: workflowId={}, sourceNode={}, targetNode={}",
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
    @Operation(summary = "批量创建连线", description = "批量在工作流中创建多条连线")
    public ResponseEntity<Void> batchCreateConnections(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Valid @RequestBody List<WorkflowResponse.ConnectionDTO> requests) {
        log.info("批量创建连线: workflowId={}, count={}", workflowId, requests.size());
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
    @Operation(summary = "删除连线", description = "删除指定的连线")
    public ResponseEntity<Void> deleteConnection(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "连线UUID", required = true)
            @PathVariable String connectionUuid) {
        log.info("删除连线: workflowId={}, connectionUuid={}", workflowId, connectionUuid);
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
    @Operation(summary = "批量删除连线", description = "批量删除多条连线")
    public ResponseEntity<Void> batchDeleteConnections(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @RequestBody List<String> connectionUuids) {
        log.info("批量删除连线: workflowId={}, count={}", workflowId, connectionUuids.size());
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
    @Operation(summary = "获取节点的出边", description = "获取以指定节点为源的所有连线")
    public ResponseEntity<List<ConnectionResponse>> getConnectionsBySourceNode(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "源节点ID", required = true)
            @PathVariable Long nodeId) {
        log.info("获取节点出边: workflowId={}, nodeId={}", workflowId, nodeId);
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
    @Operation(summary = "获取节点的入边", description = "获取以指定节点为目标的所有连线")
    public ResponseEntity<List<ConnectionResponse>> getConnectionsByTargetNode(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "目标节点ID", required = true)
            @PathVariable Long nodeId) {
        log.info("获取节点入边: workflowId={}, nodeId={}", workflowId, nodeId);
        List<ConnectionResponse> connections = connectionService.getConnectionsByTargetNode(nodeId);
        return ResponseEntity.ok(connections);
    }
}
