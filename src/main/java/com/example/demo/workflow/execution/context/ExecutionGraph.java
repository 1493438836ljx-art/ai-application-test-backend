package com.example.demo.workflow.execution.context;

import com.example.demo.workflow.entity.WorkflowConnectionEntity;
import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.error.ErrorCode;
import com.example.demo.workflow.execution.error.WorkflowExecutionException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 执行图 - 有向无环图（DAG）表示
 * 用于表示工作流的拓扑结构，支持拓扑排序和并行执行
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Slf4j
public class ExecutionGraph {

    /**
     * 工作流定义
     */
    private WorkflowDefinition definition;

    /**
     * 所有节点（UUID -> 节点实体）
     */
    private Map<String, WorkflowNodeEntity> nodes;

    /**
     * 节点入度（前置节点数量）
     */
    private Map<String, Integer> inDegree;

    /**
     * 节点出度（后续节点UUID列表）
     */
    private Map<String, List<String>> outDegree;

    /**
     * 起始节点列表（入度为0的节点）
     */
    private List<String> startNodes;

    /**
     * 结束节点列表（出度为0的节点）
     */
    private List<String> endNodes;

    /**
     * 连线与分支标签的映射（源节点UUID -> 分支标签 -> 目标节点UUID列表）
     */
    private Map<String, Map<String, List<String>>> branchConnections;

    /**
     * 从工作流定义构建执行图
     */
    public static ExecutionGraph build(WorkflowDefinition definition) {
        ExecutionGraph graph = new ExecutionGraph();
        graph.setDefinition(definition);
        graph.setNodes(new HashMap<>());
        graph.setInDegree(new HashMap<>());
        graph.setOutDegree(new HashMap<>());
        graph.setBranchConnections(new HashMap<>());

        // 1. 初始化所有节点
        for (WorkflowNodeEntity node : definition.getNodes()) {
            String nodeUuid = node.getNodeUuid();
            graph.getNodes().put(nodeUuid, node);
            graph.getInDegree().put(nodeUuid, 0);
            graph.getOutDegree().put(nodeUuid, new ArrayList<>());
            graph.getBranchConnections().put(nodeUuid, new HashMap<>());
        }

        // 2. 处理连线，构建入度和出度
        for (WorkflowConnectionEntity conn : definition.getConnections()) {
            // 获取源节点和目标节点的UUID
            String sourceUuid = graph.getNodeUuid(conn.getSourceNodeId());
            String targetUuid = graph.getNodeUuid(conn.getTargetNodeId());

            if (sourceUuid == null || targetUuid == null) {
                log.warn("连线引用了不存在的节点: sourceId={}, targetId={}",
                        conn.getSourceNodeId(), conn.getTargetNodeId());
                continue;
            }

            // 增加目标节点入度
            graph.getInDegree().merge(targetUuid, 1, Integer::sum);

            // 增加源节点出度
            graph.getOutDegree().get(sourceUuid).add(targetUuid);

            // 记录分支连线
            String branchLabel = conn.getBranchLabel();
            if (branchLabel != null && !branchLabel.isEmpty()) {
                graph.getBranchConnections()
                        .get(sourceUuid)
                        .computeIfAbsent(branchLabel, k -> new ArrayList<>())
                        .add(targetUuid);
            }
        }

        // 3. 找出起始节点（入度为0）
        List<String> startNodes = graph.getInDegree().entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        graph.setStartNodes(startNodes);

        // 4. 找出结束节点（出度为0）
        List<String> endNodes = graph.getOutDegree().entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        graph.setEndNodes(endNodes);

        // 5. 验证DAG（检测环）
        graph.validateDAG();

        return graph;
    }

    /**
     * 根据节点ID获取节点UUID
     */
    private String getNodeUuid(Long nodeId) {
        return nodes.values().stream()
                .filter(n -> nodeId.equals(n.getId()))
                .map(WorkflowNodeEntity::getNodeUuid)
                .findFirst()
                .orElse(null);
    }

    /**
     * 验证DAG - 使用Kahn算法检测是否存在环
     */
    private void validateDAG() {
        Map<String, Integer> inDegreeCopy = new HashMap<>(inDegree);
        Queue<String> queue = new LinkedList<>();
        int visitedCount = 0;

        // 将入度为0的节点加入队列
        inDegreeCopy.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .forEach(e -> queue.offer(e.getKey()));

        while (!queue.isEmpty()) {
            String nodeUuid = queue.poll();
            visitedCount++;

            for (String successor : getSuccessors(nodeUuid)) {
                int newInDegree = inDegreeCopy.get(successor) - 1;
                inDegreeCopy.put(successor, newInDegree);
                if (newInDegree == 0) {
                    queue.offer(successor);
                }
            }
        }

        // 如果访问的节点数不等于总节点数，说明存在环
        if (visitedCount != nodes.size()) {
            throw new WorkflowExecutionException(ErrorCode.WORKFLOW_CYCLIC_DEPENDENCY,
                    "工作流存在循环依赖，无法执行");
        }
    }

    /**
     * 获取节点的所有前置节点
     */
    public List<String> getPredecessors(String nodeUuid) {
        List<String> predecessors = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : outDegree.entrySet()) {
            if (entry.getValue().contains(nodeUuid)) {
                predecessors.add(entry.getKey());
            }
        }
        return predecessors;
    }

    /**
     * 获取节点的所有后续节点
     */
    public List<String> getSuccessors(String nodeUuid) {
        return outDegree.getOrDefault(nodeUuid, Collections.emptyList());
    }

    /**
     * 根据分支标签获取后续节点
     */
    public List<String> getSuccessorsByBranch(String nodeUuid, String branchLabel) {
        if (branchLabel == null || branchLabel.isEmpty()) {
            return getSuccessors(nodeUuid);
        }

        Map<String, List<String>> branches = branchConnections.get(nodeUuid);
        if (branches != null && branches.containsKey(branchLabel)) {
            return branches.get(branchLabel);
        }

        // 如果没有找到对应的分支，返回默认后续节点
        return getSuccessors(nodeUuid);
    }

    /**
     * 获取节点实体
     */
    public WorkflowNodeEntity getNode(String nodeUuid) {
        return nodes.get(nodeUuid);
    }

    /**
     * 判断节点是否存在
     */
    public boolean hasNode(String nodeUuid) {
        return nodes.containsKey(nodeUuid);
    }

    /**
     * 获取所有节点UUID
     */
    public Set<String> getAllNodeUuids() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    /**
     * 获取节点总数
     */
    public int size() {
        return nodes.size();
    }

    /**
     * 拓扑排序 - 返回按执行顺序排列的节点列表
     */
    public List<String> topologicalSort() {
        List<String> result = new ArrayList<>();
        Map<String, Integer> inDegreeCopy = new HashMap<>(inDegree);
        Queue<String> queue = new LinkedList<>();

        // 将入度为0的节点加入队列
        inDegreeCopy.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .forEach(e -> queue.offer(e.getKey()));

        while (!queue.isEmpty()) {
            String nodeUuid = queue.poll();
            result.add(nodeUuid);

            for (String successor : getSuccessors(nodeUuid)) {
                int newInDegree = inDegreeCopy.get(successor) - 1;
                inDegreeCopy.put(successor, newInDegree);
                if (newInDegree == 0) {
                    queue.offer(successor);
                }
            }
        }

        return result;
    }

    /**
     * 获取可以并行执行的节点层
     */
    public List<List<String>> getParallelLayers() {
        List<List<String>> layers = new ArrayList<>();
        Map<String, Integer> inDegreeCopy = new HashMap<>(inDegree);
        Set<String> processed = new HashSet<>();

        while (processed.size() < nodes.size()) {
            // 找出当前层（入度为0且未处理的节点）
            List<String> currentLayer = inDegreeCopy.entrySet().stream()
                    .filter(e -> e.getValue() == 0 && !processed.contains(e.getKey()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (currentLayer.isEmpty()) {
                // 不应该发生，因为已经验证过DAG
                break;
            }

            layers.add(currentLayer);

            // 更新入度
            for (String nodeUuid : currentLayer) {
                processed.add(nodeUuid);
                for (String successor : getSuccessors(nodeUuid)) {
                    inDegreeCopy.merge(successor, -1, Integer::sum);
                }
            }
        }

        return layers;
    }
}
