/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.executor.logic;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.execution.context.ExecutionContext;
import com.huawei.cloudopenlabs.workflow.execution.engine.ParameterResolver;
import com.huawei.cloudopenlabs.workflow.execution.error.ErrorCode;
import com.huawei.cloudopenlabs.workflow.execution.error.WorkflowExecutionException;
import com.huawei.cloudopenlabs.workflow.execution.evaluator.ConditionEvaluator;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutionResult;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutor;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutorRegistry;
import com.huawei.cloudopenlabs.workflow.execution.executor.ValidationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 循环节点执行器
 * 支持三种循环类型：计数循环、数组遍历、条件循环
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LoopNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;
    private final ConditionEvaluator conditionEvaluator;
    private final ParameterResolver parameterResolver;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private NodeExecutorRegistry executorRegistry;

    @Override
    public String getNodeType() {
        return "loop";
    }

    @Override
    public NodeExecutionResult execute(WorkflowNodeEntity node,
                                        Map<String, Object> inputs,
                                        ExecutionContext context) {
        log.info("Executing loop node: nodeUuid={}, nodeName={}",
                node.getNodeUuid(), node.getName());

        try {
            // 1. 解析循环配置
            LoopConfig config = parseLoopConfig(node.getLoopConfig());

            // 2. 获取循环体节点
            List<WorkflowNodeEntity> bodyNodes = getLoopBodyNodes(node, context);

            if (bodyNodes.isEmpty()) {
                log.warn("Loop body is empty: nodeUuid={}", node.getNodeUuid());
                return NodeExecutionResult.success();
            }

            // 3. 根据循环类型执行
            List<Object> collectedOutputs = new ArrayList<>();

            switch (config.getType()) {
                case COUNT:
                    collectedOutputs = executeCountLoop(node, config, bodyNodes, context);
                    break;
                case ARRAY:
                    collectedOutputs = executeArrayLoop(node, config, bodyNodes, context);
                    break;
                case CONDITION:
                    collectedOutputs = executeConditionLoop(node, config, bodyNodes, context);
                    break;
                default:
                    return NodeExecutionResult.failure("未知的循环类型: " + config.getType());
            }

            // 4. 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("loop_results", collectedOutputs);
            outputs.put("iteration_count", collectedOutputs.size());

            log.info("Loop node execution completed: nodeUuid={}, iterations={}",
                    node.getNodeUuid(), collectedOutputs.size());

            return NodeExecutionResult.success(outputs);

        } catch (Exception e) {
            log.error("Loop node execution exception: nodeUuid={}", node.getNodeUuid(), e);
            return NodeExecutionResult.failure(
                    "循环节点Execution failed: " + e.getMessage(),
                    e instanceof Exception ? (Exception) e : new RuntimeException(e)
            );
        }
    }

    /**
     * 解析循环配置
     */
    private LoopConfig parseLoopConfig(String loopConfigJson) {
        LoopConfig config = new LoopConfig();

        if (loopConfigJson != null && !loopConfigJson.isEmpty()) {
            try {
                Map<String, Object> configMap = objectMapper.readValue(
                        loopConfigJson,
                        new TypeReference<Map<String, Object>>() {}
                );

                String typeStr = (String) configMap.get("type");
                config.setType(typeStr != null ? LoopType.valueOf(typeStr.toUpperCase()) : LoopType.COUNT);

                if (configMap.containsKey("times")) {
                    config.setTimes(((Number) configMap.get("times")).intValue());
                }

                if (configMap.containsKey("maxIterations")) {
                    config.setMaxIterations(((Number) configMap.get("maxIterations")).intValue());
                }

                if (configMap.containsKey("arraySource")) {
                    config.setArraySource(configMap.get("arraySource"));
                }

                if (configMap.containsKey("condition")) {
                    config.setCondition(objectMapper.writeValueAsString(configMap.get("condition")));
                }

            } catch (Exception e) {
                log.warn("Failed to parse loop config, using default config", e);
            }
        }

        return config;
    }

    /**
     * 计数循环
     */
    private List<Object> executeCountLoop(WorkflowNodeEntity node,
                                           LoopConfig config,
                                           List<WorkflowNodeEntity> bodyNodes,
                                           ExecutionContext context) {
        int times = config.getTimes() != null ? config.getTimes() : 1;
        List<Object> results = new ArrayList<>();

        log.info("Starting count loop: times={}", times);

        for (int i = 0; i <= times; i++) {
            // 进入循环上下文
            context.enterLoop(node.getNodeUuid(), i, i);

            // 执行循环体
            Object iterationResult = executeLoopBody(bodyNodes, context);
            results.add(iterationResult);

            // 退出循环上下文
            context.exitLoop();

            log.debug("Loop iteration completed: index={}/{}", i, times);
        }

        return results;
    }

    /**
     * 数组遍历循环
     */
    private List<Object> executeArrayLoop(WorkflowNodeEntity node,
                                           LoopConfig config,
                                           List<WorkflowNodeEntity> bodyNodes,
                                           ExecutionContext context) {
        // 解析数组源
        List<?> items = resolveArraySource(config.getArraySource(), context);

        if (items == null || items.isEmpty()) {
            log.warn("Array is empty, skipping loop: nodeUuid={}", node.getNodeUuid());
            return Collections.emptyList();
        }

        List<Object> results = new ArrayList<>();

        log.info("Starting array iteration loop: itemsCount={}", items.size());

        for (int i = 0; i <= items.size(); i++) {
            Object currentItem = items.get(i - 1);

            // 进入循环上下文
            context.enterLoop(node.getNodeUuid(), currentItem, i);

            // 设置当前元素到上下文变量
            context.setGlobalVariable("current_item", currentItem);
            context.setGlobalVariable("current_index", i);

            // 执行循环体
            Object iterationResult = executeLoopBody(bodyNodes, context);
            results.add(iterationResult);

            // 退出循环上下文
            context.exitLoop();

            log.debug("Loop iteration completed: index={}/{}, item={}",
                    i, items.size(), currentItem);
        }

        return results;
    }

    /**
     * 条件循环（类似 while）
     */
    private List<Object> executeConditionLoop(WorkflowNodeEntity node,
                                               LoopConfig config,
                                               List<WorkflowNodeEntity> bodyNodes,
                                               ExecutionContext context) {
        List<Object> results = new ArrayList<>();
        int maxIterations = config.getMaxIterations() != null ? config.getMaxIterations() : 1000; // 防止无限循环

        int iteration = 0;
        boolean conditionMet = true;

        log.info("Starting condition loop: maxIterations={}", maxIterations);

        while (conditionMet && iteration < maxIterations) {
            // 评估循环条件
            if (config.getCondition() != null) {
                conditionMet = conditionEvaluator.evaluate(config.getCondition(), context, node.getNodeUuid());
            }

            if (!conditionMet) {
                log.debug("Loop condition not satisfied, exiting loop: iteration={}", iteration);
                break;
            }

            iteration++;

            // 进入循环上下文
            context.enterLoop(node.getNodeUuid(), iteration, iteration);

            // 执行循环体
            Object iterationResult = executeLoopBody(bodyNodes, context);
            results.add(iterationResult);

            // 退出循环上下文
            context.exitLoop();

            log.debug("Condition loop iteration completed: iteration={}", iteration);
        }

        if (iteration >= maxIterations) {
            log.warn("Condition loop reached max iterations limit: maxIterations={}", maxIterations);
        }

        return results;
    }

    /**
     * 执行循环体
     */
    private Object executeLoopBody(List<WorkflowNodeEntity> bodyNodes, ExecutionContext context) {
        Map<String, Object> bodyOutputs = new HashMap<>();

        for (WorkflowNodeEntity bodyNode : bodyNodes) {
            try {
                if (executorRegistry == null || !executorRegistry.hasExecutor(bodyNode.getType())) {
                    log.warn("Node executor not found: type={}", bodyNode.getType());
                    continue;
                }

                // 解析输入参数
                Map<String, Object> inputs = parameterResolver.resolveInputs(bodyNode, context);

                // 获取执行器
                NodeExecutor executor = executorRegistry.getExecutor(bodyNode.getType());

                // 执行节点
                NodeExecutionResult result = executor.execute(bodyNode, inputs, context);

                if (result.isSuccess()) {
                    bodyOutputs.putAll(result.getOutputs());
                    // 存储节点输出到上下文
                    context.setNodeOutputs(bodyNode.getNodeUuid(), result.getOutputs());
                } else {
                    // 循环体内节点执行失败处理
                    log.error("Loop body node execution failed: nodeUuid={}, error={}",
                            bodyNode.getNodeUuid(), result.getErrorMessage());
                    throw new WorkflowExecutionException(
                            ErrorCode.NODE_EXECUTION_FAILED,
                            bodyNode.getNodeUuid(),
                            bodyNode.getName(),
                            "循环体节点Execution failed: " + result.getErrorMessage()
                    );
                }

            } catch (Exception e) {
                log.error("Loop body node execution exception: nodeUuid={}", bodyNode.getNodeUuid(), e);
                throw new WorkflowExecutionException(
                        ErrorCode.NODE_EXECUTION_FAILED,
                        bodyNode.getNodeUuid(),
                        bodyNode.getName(),
                        "循环体执行失败",
                        e
                );
            }
        }

        return bodyOutputs;
    }

    /**
     * 获取循环体节点
     * 通过父节点ID查找属于循环体的子节点
     */
    private List<WorkflowNodeEntity> getLoopBodyNodes(WorkflowNodeEntity loopNode,
                                                     ExecutionContext context) {
        if (context.getDefinition() == null || context.getDefinition().getNodes() == null) {
            return Collections.emptyList();
        }

        // 使用父节点ID来查找循环体节点
        String loopNodeId = loopNode.getId();

        return context.getDefinition().getNodes().stream()
                .filter(n -> loopNodeId != null && loopNodeId.equals(n.getParentNodeId()))
                .sorted(Comparator.comparing(WorkflowNodeEntity::getPositionY))
                .toList();
    }

    /**
     * 解析数组源
     */
    @SuppressWarnings("unchecked")
    private List<?> resolveArraySource(Object arraySource, ExecutionContext context) {
        if (arraySource == null) {
            return Collections.emptyList();
        }

        // 如果是引用表达式
        if (arraySource instanceof String) {
            String ref = (String) arraySource;
            if (ref.startsWith("${") && ref.endsWith("}")) {
                // 解析引用
                String refContent = ref.substring(2, ref.length() - 1);
                String[] parts = refContent.split("\\.", 2);
                if (parts.length == 2) {
                    String nodeIdentifier = parts[0];
                    String paramName = parts[1];

                    // 先尝试通过名称查找
                    var sourceNode = context.getNodeByName(nodeIdentifier);
                    if (sourceNode == null) {
                        // 再尝试通过UUID查找
                        sourceNode = context.getNode(nodeIdentifier);
                    }

                    if (sourceNode != null) {
                        Object value = context.getNodeOutput(sourceNode.getNodeUuid(), paramName);
                        if (value instanceof List) {
                            return (List<?>) value;
                        }
                        if (value instanceof Object[]) {
                            return Arrays.asList((Object[]) value);
                        }
                        // 单个值包装成列表
                        return Collections.singletonList(value);
                    }
                }
            }
        }

        // 如果是列表
        if (arraySource instanceof List) {
            return (List<?>) arraySource;
        }

        // 单个值包装成列表
        return Collections.singletonList(arraySource);
    }

    @Override
    public ValidationResult validate(WorkflowNodeEntity node) {
        ValidationResult result = ValidationResult.success();

        String loopConfigJson = node.getLoopConfig();
        if (loopConfigJson == null || loopConfigJson.isEmpty()) {
            result.addError("循环节点必须配置循环参数");
        }

        return result;
    }

    @Override
    public boolean supportsParallel() {
            // 循环节点内部需要顺序执行
            return false;
    }

    /**
     * 循环配置
     */
    private enum LoopType {
        COUNT,      // 计数循环
        ARRAY,      // 数组遍历
        CONDITION   // 条件循环
    }

    @lombok.Data
    private static class LoopConfig {
        private LoopType type = LoopType.COUNT;
        private Integer times = 1;
        private Integer maxIterations = 1000;
        private Object arraySource;
        private String condition;
    }
}
