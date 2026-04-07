package com.example.demo.workflow.controller;

import com.example.demo.workflow.dto.NodeCreateRequest;
import com.example.demo.workflow.dto.NodeResponse;
import com.example.demo.workflow.dto.NodeUpdateRequest;
import com.example.demo.workflow.service.WorkflowNodeService;
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
 * 工作流节点控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/{workflowId}/nodes")
@RequiredArgsConstructor
@Tag(name = "工作流节点管理", description = "工作流节点的增删改查接口")
public class WorkflowNodeController {

    private final WorkflowNodeService nodeService;

    /**
     * 获取工作流的所有节点
     *
     * @param workflowId 工作流ID
     * @return 节点列表
     */
    @GetMapping
    @Operation(summary = "获取节点列表", description = "获取指定工作流的所有节点")
    public ResponseEntity<List<NodeResponse>> getNodes(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId) {
        log.info("获取工作流节点列表: workflowId={}", workflowId);
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
    @Operation(summary = "获取节点详情", description = "根据UUID获取节点的详细信息")
    public ResponseEntity<NodeResponse> getNode(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "节点UUID", required = true)
            @PathVariable String nodeUuid) {
        log.info("获取节点详情: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
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
    @Operation(summary = "创建节点", description = "在工作流中创建一个新节点")
    public ResponseEntity<NodeResponse> createNode(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Valid @RequestBody NodeCreateRequest request) {
        log.info("创建节点: workflowId={}, nodeName={}", workflowId, request.getName());
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
    @Operation(summary = "批量创建节点", description = "批量在工作流中创建多个节点")
    public ResponseEntity<Void> batchCreateNodes(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Valid @RequestBody List<NodeCreateRequest> requests) {
        log.info("批量创建节点: workflowId={}, count={}", workflowId, requests.size());
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
    @Operation(summary = "更新节点", description = "更新指定节点的配置信息")
    public ResponseEntity<NodeResponse> updateNode(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "节点UUID", required = true)
            @PathVariable String nodeUuid,
            @Valid @RequestBody NodeUpdateRequest request) {
        log.info("更新节点: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
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
    @Operation(summary = "批量更新节点", description = "批量更新多个节点的配置信息")
    public ResponseEntity<Void> batchUpdateNodes(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Valid @RequestBody List<NodeUpdateRequest> requests) {
        log.info("批量更新节点: workflowId={}, count={}", workflowId, requests.size());
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
    @Operation(summary = "删除节点", description = "删除指定的节点及其相关连线")
    public ResponseEntity<Void> deleteNode(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @Parameter(description = "节点UUID", required = true)
            @PathVariable String nodeUuid) {
        log.info("删除节点: workflowId={}, nodeUuid={}", workflowId, nodeUuid);
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
    @Operation(summary = "批量删除节点", description = "批量删除多个节点及其相关连线")
    public ResponseEntity<Void> batchDeleteNodes(
            @Parameter(description = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @RequestBody List<String> nodeUuids) {
        log.info("批量删除节点: workflowId={}, count={}", workflowId, nodeUuids.size());
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
    @Operation(summary = "根据Skill查询节点", description = "查询所有引用了指定Skill的节点")
    public ResponseEntity<List<NodeResponse>> getNodesBySkillId(
            @Parameter(description = "Skill ID", required = true)
            @PathVariable String skillId) {
        log.info("根据Skill查询节点: skillId={}", skillId);
        List<NodeResponse> nodes = nodeService.getNodesBySkillId(skillId);
        return ResponseEntity.ok(nodes);
    }
}
