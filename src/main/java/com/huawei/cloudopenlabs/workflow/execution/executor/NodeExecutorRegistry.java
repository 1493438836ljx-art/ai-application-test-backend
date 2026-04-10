/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.executor;

import com.huawei.cloudopenlabs.workflow.execution.error.ErrorCode;
import com.huawei.cloudopenlabs.workflow.execution.error.WorkflowExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点执行器注册表
 * 管理所有节点执行器的注册和分发
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
public class NodeExecutorRegistry {

    /**
     * 执行器映射表
     * key: 节点类型
     * value: 执行器实例
     */
    private final Map<String, NodeExecutor> executorMap = new ConcurrentHashMap<>();

    /**
     * 自动注入所有 NodeExecutor 实现
     */
    @Autowired(required = false)
    public void setExecutors(List<NodeExecutor> executors) {
        if (executors != null) {
            for (NodeExecutor executor : executors) {
                register(executor);
            }
        }
    }

    /**
     * 注册执行器
     *
     * @param executor 执行器实例
     */
    public void register(NodeExecutor executor) {
        String nodeType = executor.getNodeType();

        if (executorMap.containsKey(nodeType)) {
            log.warn("覆盖已存在的执行器: nodeType={}, oldExecutor={}, newExecutor={}",
                    nodeType,
                    executorMap.get(nodeType).getClass().getSimpleName(),
                    executor.getClass().getSimpleName());
        }

        executorMap.put(nodeType, executor);
        log.info("注册节点执行器: nodeType={}, executor={}",
                nodeType, executor.getClass().getSimpleName());
    }

    /**
     * 获取执行器
     *
     * @param nodeType 节点类型
     * @return 执行器实例
     * @throws WorkflowExecutionException 执行器未找到异常
     */
    public NodeExecutor getExecutor(String nodeType) {
        NodeExecutor executor = executorMap.get(nodeType);

        if (executor == null) {
            throw new WorkflowExecutionException(
                    ErrorCode.NODE_EXECUTOR_NOT_FOUND,
                    "未找到节点类型对应的执行器: " + nodeType
            );
        }

        return executor;
    }

    /**
     * 检查执行器是否存在
     *
     * @param nodeType 节点类型
     * @return 是否存在
     */
    public boolean hasExecutor(String nodeType) {
        return executorMap.containsKey(nodeType);
    }

    /**
     * 获取所有已注册的节点类型
     *
     * @return 节点类型集合
     */
    public Set<String> getRegisteredNodeTypes() {
        return Set.copyOf(executorMap.keySet());
    }

    /**
     * 获取已注册执行器的数量
     *
     * @return 数量
     */
    public int size() {
        return executorMap.size();
    }

    /**
     * 打印所有已注册的执行器（用于调试）
     */
    @PostConstruct
    public void logRegisteredExecutors() {
        log.info("已注册 {} 个节点执行器: {}", executorMap.size(), executorMap.keySet());
    }
}
