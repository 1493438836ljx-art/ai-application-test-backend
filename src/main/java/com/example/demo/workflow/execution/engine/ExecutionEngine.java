package com.example.demo.workflow.execution.engine;

import com.example.demo.workflow.entity.ExecutionStatus;
import com.example.demo.workflow.entity.WorkflowConnectionEntity;
import com.example.demo.workflow.entity.WorkflowEntity;
import com.example.demo.workflow.entity.WorkflowExecutionEntity;
import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;
import com.example.demo.workflow.execution.context.ExecutionGraph;
import com.example.demo.workflow.execution.context.WorkflowDefinition;
import com.example.demo.workflow.execution.error.ErrorCode;
import com.example.demo.workflow.execution.error.WorkflowExecutionException;
import com.example.demo.workflow.execution.executor.NodeExecutionResult;
import com.example.demo.workflow.execution.executor.NodeExecutor;
import com.example.demo.workflow.execution.executor.NodeExecutorRegistry;
import com.example.demo.workflow.execution.state.NodeExecutionStatus;
import com.example.demo.workflow.execution.state.StateManager;
import com.example.demo.workflow.mapper.WorkflowConnectionMapper;
import com.example.demo.workflow.mapper.WorkflowExecutionMapper;
import com.example.demo.workflow.mapper.WorkflowMapper;
import com.example.demo.workflow.mapper.WorkflowNodeMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工作流执行引擎
 * 负责流程编排和节点调度
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutionEngine {

    private final NodeExecutorRegistry executorRegistry;
    private final StateManager stateManager;
    private final ParameterResolver parameterResolver;
    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowConnectionMapper connectionMapper;
    private final WorkflowExecutionMapper executionMapper;
    private final ObjectMapper objectMapper;

    private ThreadPoolTaskExecutor workflowExecutor;

    @Autowired(required = false)
    public void setWorkflowExecutor(@Qualifier("workflowExecutor") ThreadPoolTaskExecutor workflowExecutor) {
        this.workflowExecutor = workflowExecutor;
    }

    /**
     * 执行工作流
     *
     * @param context 执行上下文
     */
    public void execute(ExecutionContext context) {
        Long executionId = context.getExecutionId();
        String executionUuid = context.getExecutionUuid();

        log.info("开始执行工作流: executionId={}, workflowId={}", executionId, context.getWorkflowId());

        try {
            // 1. 构建执行图
            ExecutionGraph graph = ExecutionGraph.build(context.getDefinition());
            context.setExecutionGraph(graph);

            // 2. 验证执行图
            validateExecutionGraph(graph);

            // 3. 获取起始节点
            List<String> startNodes = graph.getStartNodes();
            if (startNodes.isEmpty()) {
                throw new WorkflowExecutionException(
                        ErrorCode.WORKFLOW_NO_START_NODE,
                        "工作流没有起始节点"
                );
            }

            // 4. 初始化所有节点状态
            initializeNodeStates(context, graph);

            // 5. 执行节点
            executeNodes(graph, startNodes, context);

            // 6. 检查是否所有节点都执行完成
            verifyAllNodesCompleted(context);

            // 7. 更新工作流最终状态
            updateFinalStatus(context, ExecutionStatus.SUCCESS);

            log.info("工作流执行完成: executionId={}", executionId);

        } catch (WorkflowExecutionException e) {
            log.error("工作流执行失败: executionId={}, error={}", executionId, e.getMessage());
            handleExecutionError(context, e);
            throw e;
        } catch (Exception e) {
            log.error("工作流执行异常: executionId={}", executionId, e);
            handleExecutionError(context, e);
            throw new WorkflowExecutionException(ErrorCode.WORKFLOW_EXECUTION_FAILED, e);
        }
    }

    /**
     * 验证执行图
     */
    private void validateExecutionGraph(ExecutionGraph graph) {
        // 检查起始节点
        if (graph.getStartNodes().isEmpty()) {
            throw new WorkflowExecutionException(
                    ErrorCode.WORKFLOW_NO_START_NODE,
                    "工作流缺少开始节点"
            );
        }

        // 检查结束节点
        if (graph.getEndNodes().isEmpty()) {
            throw new WorkflowExecutionException(
                    ErrorCode.WORKFLOW_NO_END_NODE,
                    "工作流缺少结束节点"
            );
        }

        // DAG验证已在ExecutionGraph构建时完成（环检测）
    }

    /**
     * 初始化所有节点状态
     */
    private void initializeNodeStates(ExecutionContext context, ExecutionGraph graph) {
        for (String nodeUuid : graph.getAllNodeUuids()) {
            context.setNodeState(nodeUuid, NodeExecutionStatus.PENDING.name());
            stateManager.updateNodeStatus(context.getExecutionId(), nodeUuid, NodeExecutionStatus.PENDING);
        }
    }

    /**
     * 执行节点（从指定节点开始）
     */
    private void executeNodes(ExecutionGraph graph,
                              List<String> startNodeUuids,
                              ExecutionContext context) {

        // 待执行节点队列
        Queue<String> pendingNodes = new LinkedList<>(startNodeUuids);

        // 正在执行的节点计数器
        AtomicInteger runningCount = new AtomicInteger(0);

        // 已完成节点集合
        Set<String> completedNodes = ConcurrentHashMap.newKeySet();

        // 执行锁
        Object lock = new Object();

        while (!pendingNodes.isEmpty() || runningCount.get() > 0) {

            // 获取可执行节点（所有前置节点已完成）
            List<String> executableNodes = new ArrayList<>();
            Iterator<String> iterator = pendingNodes.iterator();

            while (iterator.hasNext()) {
                String nodeUuid = iterator.next();
                List<String> predecessors = graph.getPredecessors(nodeUuid);

                // 检查所有前置节点是否完成
                boolean allPredecessorsCompleted = predecessors.isEmpty() ||
                        completedNodes.containsAll(predecessors);

                if (allPredecessorsCompleted) {
                    executableNodes.add(nodeUuid);
                    iterator.remove();
                }
            }

            if (executableNodes.isEmpty() && runningCount.get() == 0 && !pendingNodes.isEmpty()) {
                // 没有可执行节点，也没有正在执行的节点，但还有待执行节点
                // 说明存在死锁
                throw new WorkflowExecutionException(
                        ErrorCode.WORKFLOW_EXECUTION_FAILED,
                        "执行死锁：无法继续执行剩余节点"
                );
            }

            // 并行执行可执行节点
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (String nodeUuid : executableNodes) {
                runningCount.incrementAndGet();

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        WorkflowNodeEntity node = graph.getNode(nodeUuid);
                        executeNode(node, context);

                        completedNodes.add(nodeUuid);

                        // 将后续节点加入待执行队列
                        List<String> successors = graph.getSuccessors(nodeUuid);
                        synchronized (lock) {
                            pendingNodes.addAll(successors);
                        }

                    } catch (Exception e) {
                        log.error("节点执行异常: nodeUuid={}", nodeUuid, e);
                        handleNodeExecutionError(nodeUuid, context, e);
                    } finally {
                        runningCount.decrementAndGet();
                    }
                }, getExecutor());

                futures.add(future);
            }

            // 等待当前批次完成
            if (!futures.isEmpty()) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            // 短暂等待，避免忙等待
            if (pendingNodes.isEmpty() && runningCount.get() > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 执行单个节点
     */
    private void executeNode(WorkflowNodeEntity node, ExecutionContext context) {
        String nodeUuid = node.getNodeUuid();
        String nodeName = node.getName();

        log.info("开始执行节点: nodeUuid={}, nodeName={}", nodeUuid, nodeName);

        // 1. 更新节点状态为 RUNNING
        context.setNodeState(nodeUuid, NodeExecutionStatus.RUNNING.name());
        stateManager.updateNodeStatus(context.getExecutionId(), nodeUuid, NodeExecutionStatus.RUNNING);

        long startTime = System.currentTimeMillis();

        try {
            // 2. 解析输入参数
            Map<String, Object> inputs = parameterResolver.resolveInputs(node, context);

            // 3. 获取节点执行器
            NodeExecutor executor = executorRegistry.getExecutor(node.getType());

            // 4. 执行节点
            NodeExecutionResult result = executor.execute(node, inputs, context);

            long durationMs = System.currentTimeMillis() - startTime;

            // 5. 处理执行结果
            if (result.isSuccess()) {
                // 存储节点输出到上下文
                context.setNodeOutputs(nodeUuid, result.getOutputs());

                // 更新节点状态为 SUCCESS
                context.setNodeState(nodeUuid, NodeExecutionStatus.SUCCESS.name());
                result.setDurationMs(durationMs);
                stateManager.updateNodeStatus(context.getExecutionId(), nodeUuid,
                        NodeExecutionStatus.SUCCESS, result);

                log.info("节点执行成功: nodeUuid={}, nodeName={}, durationMs={}",
                        nodeUuid, nodeName, durationMs);

            } else {
                // 处理执行失败
                handleNodeFailure(node, context, result);
            }

        } catch (Exception e) {
            log.error("节点执行异常: nodeUuid={}, nodeName={}", nodeUuid, nodeName, e);
            handleNodeExecutionError(nodeUuid, context, e);
            throw e;
        }
    }

    /**
     * 处理节点执行失败
     */
    private void handleNodeFailure(WorkflowNodeEntity node,
                                    ExecutionContext context,
                                    NodeExecutionResult result) {
        String nodeUuid = node.getNodeUuid();

        log.error("节点执行失败: nodeUuid={}, error={}", nodeUuid, result.getErrorMessage());

        // 更新节点状态为 FAILED
        context.setNodeState(nodeUuid, NodeExecutionStatus.FAILED.name());
        stateManager.updateNodeStatus(context.getExecutionId(), nodeUuid,
                NodeExecutionStatus.FAILED, result);

        // 获取节点的错误策略
        String errorStrategy = node.getErrorStrategy();
        if (errorStrategy == null || errorStrategy.isEmpty()) {
            errorStrategy = "STOP";
        }

        switch (errorStrategy) {
            case "SKIP":
                log.info("节点错误策略为跳过，继续执行: nodeUuid={}", nodeUuid);
                context.setNodeState(nodeUuid, NodeExecutionStatus.SKIPPED.name());
                stateManager.updateNodeStatus(context.getExecutionId(), nodeUuid,
                        NodeExecutionStatus.SKIPPED, result);
                break;

            case "STOP":
            default:
                throw new WorkflowExecutionException(
                        ErrorCode.NODE_EXECUTION_FAILED,
                        nodeUuid,
                        node.getName(),
                        result.getErrorMessage()
                );
        }
    }

    /**
     * 处理节点执行错误
     */
    private void handleNodeExecutionError(String nodeUuid, ExecutionContext context, Exception e) {
        context.setNodeState(nodeUuid, NodeExecutionStatus.FAILED.name());

        NodeExecutionResult result = NodeExecutionResult.failure(
                e.getMessage() != null ? e.getMessage() : "节点执行异常",
                e instanceof Exception ? (Exception) e : new RuntimeException(e)
        );

        stateManager.updateNodeStatus(context.getExecutionId(), nodeUuid,
                NodeExecutionStatus.FAILED, result);
    }

    /**
     * 验证所有节点是否执行完成
     */
    private void verifyAllNodesCompleted(ExecutionContext context) {
        ExecutionGraph graph = context.getExecutionGraph();

        for (String nodeUuid : graph.getAllNodeUuids()) {
            String state = context.getNodeState(nodeUuid);
            if (state == null || NodeExecutionStatus.PENDING.name().equals(state)) {
                log.warn("节点未执行: nodeUuid={}", nodeUuid);
            }
        }
    }

    /**
     * 更新最终状态
     */
    private void updateFinalStatus(ExecutionContext context, ExecutionStatus status) {
        // 统计节点执行结果
        Map<String, String> nodeStates = context.getNodeStates();

        long successCount = nodeStates.values().stream()
                .filter(s -> NodeExecutionStatus.SUCCESS.name().equals(s) ||
                        NodeExecutionStatus.SKIPPED.name().equals(s))
                .count();

        long failedCount = nodeStates.values().stream()
                .filter(s -> NodeExecutionStatus.FAILED.name().equals(s))
                .count();

        // 如果有失败的节点，但策略允许部分成功
        if (status == ExecutionStatus.SUCCESS && failedCount > 0) {
            status = ExecutionStatus.PARTIAL_SUCCESS;
        }

        // 更新工作流状态
        stateManager.updateWorkflowStatus(
                context.getExecutionId(),
                status,
                100,
                collectWorkflowOutputs(context)
        );
    }

    /**
     * 收集工作流输出
     */
    private Map<String, Object> collectWorkflowOutputs(ExecutionContext context) {
        Map<String, Object> outputs = new HashMap<>();

        // 获取结束节点的输出
        ExecutionGraph graph = context.getExecutionGraph();
        for (String endNodeUuid : graph.getEndNodes()) {
            Map<String, Object> nodeOutputs = context.getNodeOutputs(endNodeUuid);
            if (nodeOutputs != null) {
                outputs.putAll(nodeOutputs);
            }
        }

        return outputs;
    }

    /**
     * 处理执行错误
     */
    private void handleExecutionError(ExecutionContext context, Exception e) {
        ExecutionStatus status = ExecutionStatus.FAILED;

        if (e instanceof WorkflowExecutionException) {
            WorkflowExecutionException wee = (WorkflowExecutionException) e;
            if (wee.getErrorCode() == ErrorCode.NODE_TIMEOUT ||
                    wee.getErrorCode() == ErrorCode.WORKFLOW_TIMEOUT) {
                status = ExecutionStatus.TIMEOUT;
            }
        }

        stateManager.updateWorkflowStatus(
                context.getExecutionId(),
                status,
                null,
                Map.of("error", e.getMessage())
        );
    }

    /**
     * 获取执行器
     */
    private java.util.concurrent.Executor getExecutor() {
        return workflowExecutor != null ? workflowExecutor : Runnable::run;
    }
}
