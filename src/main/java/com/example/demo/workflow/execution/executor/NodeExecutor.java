package com.example.demo.workflow.execution.executor;

import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;

import java.util.Map;

/**
 * 节点执行器接口
 * 所有节点执行器必须实现此接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public interface NodeExecutor {

    /**
     * 获取支持的节点类型
     *
     * @return 节点类型标识（如 start, end, skill, condition_simple 等）
     */
    String getNodeType();

    /**
     * 执行节点
     *
     * @param node    节点定义
     * @param inputs  解析后的输入参数
     * @param context 执行上下文
     * @return 执行结果
     */
    NodeExecutionResult execute(WorkflowNodeEntity node,
                                 Map<String, Object> inputs,
                                 ExecutionContext context);

    /**
     * 验证节点配置（可选实现）
     *
     * @param node 节点定义
     * @return 验证结果
     */
    default ValidationResult validate(WorkflowNodeEntity node) {
        return ValidationResult.success();
    }

    /**
     * 获取节点描述（用于日志）
     *
     * @param node 节点定义
     * @return 描述字符串
     */
    default String getNodeDescription(WorkflowNodeEntity node) {
        return String.format("[%s] %s", node.getType(), node.getName());
    }

    /**
     * 是否支持并行执行
     * 默认支持，某些节点（如循环节点）可能需要禁用并行
     *
     * @return 是否支持并行
     */
    default boolean supportsParallel() {
        return true;
    }

    /**
     * 获取执行超时时间（毫秒）
     * 返回null表示使用默认超时
     *
     * @param node 节点定义
     * @return 超时时间（毫秒）
     */
    default Long getTimeout(WorkflowNodeEntity node) {
        return null;
    }
}
