package com.huawei.cloudopenlabs.workflow.service.validation;

import com.huawei.cloudopenlabs.workflow.dto.ValidationResult;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowConnectionEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowConnectionMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 循环依赖检测器
 * 使用 Kahn 算法检测工作流中的循环依赖
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CyclicDependencyValidator {

    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowConnectionMapper connectionMapper;

    // 错误码常量
    public static final String CYCLIC_DEPENDENCY = "WF_CYCLE_001";

    /**
     * 验证工作流是否存在循环依赖
     *
     * @param workflowId 工作流ID
     * @return 验证结果
     */
    public ValidationResult validate(Long workflowId) {
        log.debug("开始循环依赖检测: workflowId={}", workflowId);
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);

        if (nodes.isEmpty()) {
            return result;
        }

        // 构建邻接表和入度表（使用数据库 ID）
        Map<Long, List<Long>> adjList = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, String> idToNameMap = new HashMap<>();

        // 初始化
        for (WorkflowNodeEntity node : nodes) {
            adjList.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
            idToNameMap.put(node.getId(), node.getName());
        }

        // 构建边
        for (WorkflowConnectionEntity conn : connections) {
            Long sourceId = conn.getSourceNodeId();
            Long targetId = conn.getTargetNodeId();

            if (sourceId != null && targetId != null) {
                adjList.get(sourceId).add(targetId);
                inDegree.merge(targetId, 1, Integer::sum);
            }
        }

        // Kahn 算法
        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int processedCount = 0;
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            processedCount++;

            for (Long next : adjList.get(current)) {
                int newDegree = inDegree.get(next) - 1;
                inDegree.put(next, newDegree);
                if (newDegree == 0) {
                    queue.add(next);
                }
            }
        }

        // 如果处理的节点数小于总节点数，说明存在环
        if (processedCount < nodes.size()) {
            // 找出参与循环的节点
            List<String> cycleNodeNames = nodes.stream()
                    .filter(n -> inDegree.get(n.getId()) > 0)
                    .map(WorkflowNodeEntity::getName)
                    .collect(Collectors.toList());

            result.addError(CYCLIC_DEPENDENCY,
                    "工作流存在循环依赖，涉及节点: " + String.join(", ", cycleNodeNames),
                    null, null);
        }

        log.debug("循环依赖检测完成: workflowId={}, valid={}, hasCycle={}",
                workflowId, result.isValid(), !result.isValid());

        return result;
    }

    /**
     * 获取节点拓扑排序
     *
     * @param workflowId 工作流ID
     * @return 节点名称的拓扑排序列表
     * @throws IllegalStateException 如果存在循环依赖
     */
    public List<String> getTopologicalOrder(Long workflowId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);

        Map<Long, List<Long>> adjList = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, String> idToNameMap = new HashMap<>();

        for (WorkflowNodeEntity node : nodes) {
            adjList.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
            idToNameMap.put(node.getId(), node.getName());
        }

        for (WorkflowConnectionEntity conn : connections) {
            Long sourceId = conn.getSourceNodeId();
            Long targetId = conn.getTargetNodeId();

            if (sourceId != null && targetId != null) {
                adjList.get(sourceId).add(targetId);
                inDegree.merge(targetId, 1, Integer::sum);
            }
        }

        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            order.add(idToNameMap.get(current));

            for (Long next : adjList.get(current)) {
                int newDegree = inDegree.get(next) - 1;
                inDegree.put(next, newDegree);
                if (newDegree == 0) {
                    queue.add(next);
                }
            }
        }

        if (order.size() != nodes.size()) {
            throw new IllegalStateException("工作流存在循环依赖，无法生成拓扑排序");
        }

        return order;
    }

    /**
     * 获取节点 ID 的拓扑排序
     *
     * @param workflowId 工作流ID
     * @return 节点ID的拓扑排序列表
     * @throws IllegalStateException 如果存在循环依赖
     */
    public List<Long> getTopologicalOrderByIds(Long workflowId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);

        Map<Long, List<Long>> adjList = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();

        for (WorkflowNodeEntity node : nodes) {
            adjList.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }

        for (WorkflowConnectionEntity conn : connections) {
            Long sourceId = conn.getSourceNodeId();
            Long targetId = conn.getTargetNodeId();

            if (sourceId != null && targetId != null) {
                adjList.get(sourceId).add(targetId);
                inDegree.merge(targetId, 1, Integer::sum);
            }
        }

        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Long> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            order.add(current);

            for (Long next : adjList.get(current)) {
                int newDegree = inDegree.get(next) - 1;
                inDegree.put(next, newDegree);
                if (newDegree == 0) {
                    queue.add(next);
                }
            }
        }

        if (order.size() != nodes.size()) {
            throw new IllegalStateException("工作流存在循环依赖，无法生成拓扑排序");
        }

        return order;
    }

    /**
     * 检查是否存在循环依赖
     *
     * @param workflowId 工作流ID
     * @return 是否存在循环
     */
    public boolean hasCycle(Long workflowId) {
        ValidationResult result = validate(workflowId);
        return !result.isValid();
    }
}
