package com.example.demo.workflow.execution.executor.base;

import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;
import com.example.demo.workflow.execution.context.ExecutionGraph;
import com.example.demo.workflow.execution.executor.NodeExecutionResult;
import com.example.demo.workflow.execution.executor.NodeExecutor;
import com.example.demo.workflow.execution.executor.ValidationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 结束节点执行器
 * 负责收集最终输出结果，标记工作流完成
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EndNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "end";
    }

    @Override
    public NodeExecutionResult execute(WorkflowNodeEntity node,
                                        Map<String, Object> inputs,
                                        ExecutionContext context) {
        log.info("执行结束节点: nodeUuid={}, nodeName={}", node.getNodeUuid(), node.getName());

        Map<String, Object> outputs = new HashMap<>();

        try {
            // 1. 首先尝试从配置的输入参数获取值（由ParameterResolver解析）
            if (inputs != null && !inputs.isEmpty()) {
                outputs.putAll(inputs);
                log.debug("结束节点从配置参数获取输入: {}", inputs.keySet());
            }

            // 2. 如果没有配置输入参数，自动从直接前驱节点收集输出
            if (outputs.isEmpty()) {
                collectOutputsFromPredecessors(node, context, outputs);
            }

            // 3. 尝试从定义的输出参数中筛选需要的字段
            String outputParamsJson = node.getOutputParams();
            if (outputParamsJson != null && !outputParamsJson.isEmpty()) {
                List<Map<String, Object>> outputParams = objectMapper.readValue(
                        outputParamsJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                Map<String, Object> filteredOutputs = new HashMap<>();
                for (Map<String, Object> param : outputParams) {
                    String paramName = (String) param.get("name");
                    Object value = outputs.get(paramName);
                    if (value != null) {
                        filteredOutputs.put(paramName, value);
                        log.debug("结束节点输出: {} = {}", paramName, value);
                    }
                }

                // 如果有筛选出参数，使用筛选后的结果
                if (!filteredOutputs.isEmpty()) {
                    outputs = filteredOutputs;
                }
            }

            log.info("结束节点执行完成: nodeUuid={}, outputs={}", node.getNodeUuid(), outputs);

            return NodeExecutionResult.success(outputs);

        } catch (Exception e) {
            log.error("结束节点执行异常: nodeUuid={}", node.getNodeUuid(), e);
            // 结束节点即使出错也返回成功，但记录错误信息
            return NodeExecutionResult.success(outputs);
        }
    }

    /**
     * 从直接前驱节点收集输出
     */
    private void collectOutputsFromPredecessors(WorkflowNodeEntity node,
                                                 ExecutionContext context,
                                                 Map<String, Object> outputs) {
        ExecutionGraph graph = context.getExecutionGraph();
        if (graph == null) {
            log.warn("执行图为空，无法获取前驱节点");
            return;
        }

        // 获取直接前驱节点（不是所有前置节点，只是直接连接的节点）
        List<String> directPredecessors = graph.getPredecessors(node.getNodeUuid());
        log.debug("结束节点的直接前驱节点: {}", directPredecessors);

        for (String predUuid : directPredecessors) {
            Map<String, Object> predOutputs = context.getNodeOutputs(predUuid);
            if (predOutputs != null && !predOutputs.isEmpty()) {
                outputs.putAll(predOutputs);
                log.debug("结束节点从前驱节点 {} 收集输出: {}", predUuid, predOutputs.keySet());
            }
        }
    }

    @Override
    public ValidationResult validate(WorkflowNodeEntity node) {
        // 结束节点没有特殊验证要求
        return ValidationResult.success();
    }

    @Override
    public boolean supportsParallel() {
        // 结束节点不需要并行执行
        return false;
    }
}
