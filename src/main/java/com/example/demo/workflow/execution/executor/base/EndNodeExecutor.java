package com.example.demo.workflow.execution.executor.base;

import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;
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
            // 获取结束节点定义的输出参数
            String outputParamsJson = node.getOutputParams();
            if (outputParamsJson != null && !outputParamsJson.isEmpty()) {
                List<Map<String, Object>> outputParams = objectMapper.readValue(
                        outputParamsJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                for (Map<String, Object> param : outputParams) {
                    String paramName = (String) param.get("name");

                    // 从输入中获取参数值（已经由参数解析器解析好）
                    Object value = inputs.get(paramName);
                    if (value != null) {
                        outputs.put(paramName, value);
                        log.debug("结束节点输出: {} = {}", paramName, value);
                    }
                }
            }

            // 如果没有定义输出参数，将所有输入作为输出
            if (outputs.isEmpty() && !inputs.isEmpty()) {
                outputs.putAll(inputs);
            }

            log.info("结束节点执行完成: nodeUuid={}, outputs={}", node.getNodeUuid(), outputs.keySet());

            return NodeExecutionResult.success(outputs);

        } catch (Exception e) {
            log.error("结束节点执行异常: nodeUuid={}", node.getNodeUuid(), e);
            // 结束节点即使出错也返回成功，但记录错误信息
            return NodeExecutionResult.success(outputs);
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
