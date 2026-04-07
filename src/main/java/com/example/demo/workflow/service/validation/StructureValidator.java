package com.example.demo.workflow.service.validation;

import com.example.demo.workflow.dto.ValidationResult;
import com.example.demo.workflow.entity.WorkflowConnectionEntity;
import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.mapper.WorkflowConnectionMapper;
import com.example.demo.workflow.mapper.WorkflowNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构验证器
 * 验证工作流的结构完整性，包括开始/结束节点、孤立节点等
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StructureValidator {

    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowConnectionMapper connectionMapper;

    // 错误码常量
    public static final String START_NODE_MISSING = "WF_STR_001";
    public static final String START_NODE_DUPLICATE = "WF_STR_002";
    public static final String END_NODE_MISSING = "WF_STR_003";
    public static final String END_NODE_DUPLICATE = "WF_STR_004";
    public static final String ORPHAN_NODE = "WF_STR_005";
    public static final String START_NODE_HAS_INPUT = "WF_STR_006";
    public static final String END_NODE_HAS_OUTPUT = "WF_STR_007";

    /**
     * 验证工作流结构
     *
     * @param workflowId 工作流ID
     * @return 验证结果
     */
    public ValidationResult validate(Long workflowId) {
        log.debug("开始结构验证: workflowId={}", workflowId);
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);

        if (nodes.isEmpty()) {
            result.addError(START_NODE_MISSING, "工作流没有任何节点", null, null);
            return result;
        }

        // 构建节点 UUID 到实体的映射
        // 使用数据库 ID 而非 UUID 进行连线映射
        Set<Long> nodesWithInput = new HashSet<>();
        Set<Long> nodesWithOutput = new HashSet<>();

        for (WorkflowConnectionEntity conn : connections) {
            nodesWithInput.add(conn.getTargetNodeId());
            nodesWithOutput.add(conn.getSourceNodeId());
        }

        // 1. 验证开始节点
        List<WorkflowNodeEntity> startNodes = nodes.stream()
                .filter(n -> "start".equals(n.getType()))
                .collect(Collectors.toList());

        if (startNodes.isEmpty()) {
            result.addError(START_NODE_MISSING, "缺少开始节点", null, null);
        } else if (startNodes.size() > 1) {
            result.addError(START_NODE_DUPLICATE, "存在多个开始节点，只能有一个开始节点", null, null);
        } else {
            WorkflowNodeEntity startNode = startNodes.get(0);
            if (nodesWithInput.contains(startNode.getId())) {
                result.addError(START_NODE_HAS_INPUT, "开始节点不能有入边", startNode.getNodeUuid(), null);
            }
        }

        // 2. 验证结束节点
        List<WorkflowNodeEntity> endNodes = nodes.stream()
                .filter(n -> "end".equals(n.getType()))
                .collect(Collectors.toList());

        if (endNodes.isEmpty()) {
            result.addError(END_NODE_MISSING, "缺少结束节点", null, null);
        } else {
            for (WorkflowNodeEntity endNode : endNodes) {
                if (nodesWithOutput.contains(endNode.getId())) {
                    result.addError(END_NODE_HAS_OUTPUT, "结束节点不能有出边", endNode.getNodeUuid(), null);
                }
            }
        }

        // 3. 验证孤立节点（无入边也无出边，且不是开始/结束节点）
        for (WorkflowNodeEntity node : nodes) {
            // 开始和结束节点不检查孤立
            if ("start".equals(node.getType()) || "end".equals(node.getType())) {
                continue;
            }

            boolean hasInput = nodesWithInput.contains(node.getId());
            boolean hasOutput = nodesWithOutput.contains(node.getId());

            if (!hasInput && !hasOutput) {
                result.addWarning(ORPHAN_NODE,
                        "节点 '" + node.getName() + "' 未连接到工作流（无入边也无出边）",
                        node.getNodeUuid());
            }
        }

        log.debug("结构验证完成: workflowId={}, valid={}, errors={}, warnings={}",
                workflowId, result.isValid(), result.getErrors().size(), result.getWarnings().size());

        return result;
    }

    /**
     * 获取孤立节点列表
     *
     * @param workflowId 工作流ID
     * @return 孤立节点UUID列表
     */
    public List<String> getOrphanNodes(Long workflowId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);

        Set<Long> connectedNodeIds = new HashSet<>();
        for (WorkflowConnectionEntity conn : connections) {
            connectedNodeIds.add(conn.getSourceNodeId());
            connectedNodeIds.add(conn.getTargetNodeId());
        }

        return nodes.stream()
                .filter(n -> !"start".equals(n.getType()) && !"end".equals(n.getType()))
                .filter(n -> !connectedNodeIds.contains(n.getId()))
                .map(WorkflowNodeEntity::getNodeUuid)
                .collect(Collectors.toList());
    }
}
