/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.context;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行上下文
 * 管理工作流执行过程中的所有状态和数据
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@Builder
@Slf4j
public class ExecutionContext {

    // ==================== 基本信息 ====================

    /**
     * 执行记录ID
     */
    private String executionId;

    /**
     * 执行UUID
     */
    private String executionUuid;

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 触发人
     */
    private String triggeredBy;

    /**
     * 输入参数
     */
    private Map<String, Object> inputData;

    /**
     * 工作流定义
     */
    private WorkflowDefinition definition;

    /**
     * 执行图
     */
    private ExecutionGraph executionGraph;

    // ==================== 变量存储 ====================

    /**
     * 节点输出缓存
     * key: nodeUuid
     * value: {paramName: value}
     */
    @Builder.Default
    private Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();

    /**
     * 全局变量池
     * key: 变量名
     * value: 变量值
     */
    @Builder.Default
    private Map<String, Object> globalVariables = new ConcurrentHashMap<>();

    // ==================== 执行状态 ====================

    /**
     * 各节点执行状态
     */
    @Builder.Default
    private Map<String, String> nodeStates = new ConcurrentHashMap<>();

    /**
     * 当前执行节点UUID
     */
    private String currentNodeUuid;

    // ==================== 前置节点计算缓存 ====================

    /**
     * 节点前置节点缓存（传递闭包）
     */
    @Builder.Default
    private Map<String, Set<String>> predecessorCache = new ConcurrentHashMap<>();

    // ==================== 循环上下文 ====================

    /**
     * 当前循环上下文栈
     */
    @Builder.Default
    private Deque<LoopContext> loopContextStack = new ArrayDeque<>();

    // ==================== 工具方法：节点输出 ====================

    /**
     * 获取节点输出参数
     *
     * @param nodeUuid  节点UUID
     * @param paramName 参数名
     * @return 参数值
     */
    public Object getNodeOutput(String nodeUuid, String paramName) {
        Map<String, Object> outputs = nodeOutputs.get(nodeUuid);
        return outputs != null ? outputs.get(paramName) : null;
    }

    /**
     * 获取节点所有输出
     *
     * @param nodeUuid 节点UUID
     * @return 输出Map
     */
    public Map<String, Object> getNodeOutputs(String nodeUuid) {
        return nodeOutputs.getOrDefault(nodeUuid, Collections.emptyMap());
    }

    /**
     * 设置节点输出
     *
     * @param nodeUuid 节点UUID
     * @param outputs  输出Map
     */
    public void setNodeOutputs(String nodeUuid, Map<String, Object> outputs) {
        nodeOutputs.put(nodeUuid, new ConcurrentHashMap<>(outputs));
        log.debug("Node outputs saved: nodeUuid={}, outputs={}", nodeUuid, outputs.keySet());
    }

    /**
     * 添加单个节点输出
     */
    public void addNodeOutput(String nodeUuid, String paramName, Object value) {
        nodeOutputs.computeIfAbsent(nodeUuid, k -> new ConcurrentHashMap<>())
                .put(paramName, value);
    }

    // ==================== 工具方法：全局变量 ====================

    /**
     * 获取全局变量
     *
     * @param varName 变量名
     * @return 变量值
     */
    public Object getGlobalVariable(String varName) {
        return globalVariables.get(varName);
    }

    /**
     * 设置全局变量
     *
     * @param varName 变量名
     * @param value   变量值
     */
    public void setGlobalVariable(String varName, Object value) {
        globalVariables.put(varName, value);
    }

    /**
     * 批量设置全局变量
     */
    public void setGlobalVariables(Map<String, Object> variables) {
        if (variables != null) {
            globalVariables.putAll(variables);
        }
    }

    // ==================== 工具方法：前置节点 ====================

    /**
     * 获取节点的前置节点集合（传递闭包）
     *
     * @param nodeUuid 节点UUID
     * @return 所有前置节点UUID集合
     */
    public Set<String> getPredecessors(String nodeUuid) {
        return predecessorCache.computeIfAbsent(nodeUuid, this::calculatePredecessors);
    }

    /**
     * 递归计算所有前置节点
     */
    private Set<String> calculatePredecessors(String nodeUuid) {
        Set<String> predecessors = new HashSet<>();
        calculatePredecessorsRecursive(nodeUuid, predecessors);
        return predecessors;
    }

    private void calculatePredecessorsRecursive(String nodeUuid, Set<String> visited) {
        if (executionGraph == null) {
            return;
        }

        List<String> directPredecessors = executionGraph.getPredecessors(nodeUuid);
        for (String predUuid : directPredecessors) {
            if (!visited.contains(predUuid)) {
                visited.add(predUuid);
                calculatePredecessorsRecursive(predUuid, visited);
            }
        }
    }

    /**
     * 判断当前节点是否可以引用目标节点的输出
     * 规则：只能引用前置节点的输出
     *
     * @param currentNodeUuid 当前节点UUID
     * @param targetNodeUuid  目标节点UUID
     * @return 是否可引用
     */
    public boolean canReferenceNode(String currentNodeUuid, String targetNodeUuid) {
        if (targetNodeUuid == null) {
            return false;
        }
        // 自己不能引用自己
        if (targetNodeUuid.equals(currentNodeUuid)) {
            return false;
        }
        return getPredecessors(currentNodeUuid).contains(targetNodeUuid);
    }

    /**
     * 获取当前可引用的所有节点UUID
     *
     * @param currentNodeUuid 当前节点UUID
     * @return 可引用的节点UUID列表
     */
    public List<String> getReferenceableNodes(String currentNodeUuid) {
        return new ArrayList<>(getPredecessors(currentNodeUuid));
    }

    // ==================== 工具方法：节点状态 ====================

    /**
     * 设置节点执行状态
     */
    public void setNodeState(String nodeUuid, String state) {
        nodeStates.put(nodeUuid, state);
    }

    /**
     * 获取节点执行状态
     */
    public String getNodeState(String nodeUuid) {
        return nodeStates.get(nodeUuid);
    }

    // ==================== 工具方法：循环上下文 ====================

    /**
     * 进入循环
     *
     * @param loopNodeUuid 循环节点UUID
     * @param currentItem  当前元素
     * @param currentIndex 当前索引
     */
    public void enterLoop(String loopNodeUuid, Object currentItem, int currentIndex) {
        LoopContext loopContext = new LoopContext(loopNodeUuid, currentItem, currentIndex);
        loopContextStack.push(loopContext);
        log.debug("Entering loop: loopNodeUuid={}, index={}", loopNodeUuid, currentIndex);
    }

    /**
     * 退出循环
     */
    public void exitLoop() {
        if (!loopContextStack.isEmpty()) {
            LoopContext context = loopContextStack.pop();
            log.debug("Exiting loop: loopNodeUuid={}", context.getLoopNodeUuid());
        }
    }

    /**
     * 获取当前循环上下文
     *
     * @return 循环上下文，如果不在循环中则返回null
     */
    public LoopContext getCurrentLoopContext() {
        return loopContextStack.peek();
    }

    /**
     * 获取当前循环元素
     */
    public Object getCurrentLoopItem() {
        LoopContext context = getCurrentLoopContext();
        return context != null ? context.getCurrentItem() : null;
    }

    /**
     * 获取当前循环索引
     */
    public Integer getCurrentLoopIndex() {
        LoopContext context = getCurrentLoopContext();
        return context != null ? context.getCurrentIndex() : null;
    }

    /**
     * 是否在循环中
     */
    public boolean isInLoop() {
        return !loopContextStack.isEmpty();
    }

    // ==================== 工具方法：节点查找 ====================

    /**
     * 根据UUID获取节点
     */
    public WorkflowNodeEntity getNode(String nodeUuid) {
        if (definition == null) {
            return null;
        }
        return definition.getNodeByUuid(nodeUuid);
    }

    /**
     * 根据名称获取节点
     */
    public WorkflowNodeEntity getNodeByName(String name) {
        if (definition == null) {
            return null;
        }
        return definition.getNodeByName(name);
    }

    /**
     * 获取开始节点
     */
    public WorkflowNodeEntity getStartNode() {
        if (definition == null) {
            return null;
        }
        return definition.getStartNode();
    }

    /**
     * 获取结束节点
     */
    public WorkflowNodeEntity getEndNode() {
        if (definition == null) {
            return null;
        }
        return definition.getEndNode();
    }

    // ==================== 内部类 ====================

    /**
     * 循环上下文
     */
    @Data
    public static class LoopContext {
        private final String loopNodeUuid;
        private final Object currentItem;
        private final int currentIndex;

        public LoopContext(String loopNodeUuid, Object currentItem, int currentIndex) {
            this.loopNodeUuid = loopNodeUuid;
            this.currentItem = currentItem;
            this.currentIndex = currentIndex;
        }
    }
}
