/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.executor.base;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.execution.context.ExecutionContext;
import com.huawei.cloudopenlabs.workflow.execution.error.ErrorCode;
import com.huawei.cloudopenlabs.workflow.execution.error.WorkflowExecutionException;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutionResult;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutor;
import com.huawei.cloudopenlabs.workflow.execution.executor.ValidationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开始节点执行器
 * 负责初始化工作流参数，将输入数据写入执行上下文
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StartNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "start";
    }

    @Override
    public NodeExecutionResult execute(WorkflowNodeEntity node,
                                        Map<String, Object> inputs,
                                        ExecutionContext context) {
        log.info("执行开始节点: nodeUuid={}, nodeName={}", node.getNodeUuid(), node.getName());

        Map<String, Object> outputs = new HashMap<>();

        try {
            // 获取开始节点定义的输出参数
            String outputParamsJson = node.getOutputParams();
            if (outputParamsJson != null && !outputParamsJson.isEmpty()) {
                List<Map<String, Object>> outputParams = objectMapper.readValue(
                        outputParamsJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                for (Map<String, Object> param : outputParams) {
                    String paramName = (String) param.get("name");
                    Boolean required = (Boolean) param.get("required");
                    Object defaultValue = param.get("defaultValue");

                    // 从用户输入数据中获取参数值（开始节点从上下文获取用户传入的inputData）
                    Object value = context.getInputData() != null ? context.getInputData().get(paramName) : null;

                    // 如果输入中没有，使用默认值
                    if (value == null && defaultValue != null) {
                        value = defaultValue;
                    }

                    // 必填参数校验
                    if (Boolean.TRUE.equals(required) && value == null) {
                        throw new WorkflowExecutionException(
                                ErrorCode.PARAM_REQUIRED_MISSING,
                                node.getNodeUuid(),
                                node.getName(),
                                "必填参数缺失: " + paramName
                        );
                    }

                    if (value != null) {
                        outputs.put(paramName, value);
                        log.debug("开始节点参数: {} = {}", paramName, value);
                    }
                }
            }

            // 将开始节点的输出设置到全局变量
            outputs.forEach((key, value) -> context.setGlobalVariable(key, value));

            log.info("开始节点执行完成: nodeUuid={}, outputs={}", node.getNodeUuid(), outputs.keySet());

            return NodeExecutionResult.success(outputs);

        } catch (WorkflowExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("开始节点执行异常: nodeUuid={}", node.getNodeUuid(), e);
            throw new WorkflowExecutionException(
                    ErrorCode.NODE_EXECUTION_FAILED,
                    node.getNodeUuid(),
                    node.getName(),
                    "开始节点执行失败: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public ValidationResult validate(WorkflowNodeEntity node) {
        ValidationResult result = ValidationResult.success();

        // 验证输出参数配置
        String outputParamsJson = node.getOutputParams();
        if (outputParamsJson != null && !outputParamsJson.isEmpty()) {
            try {
                List<Map<String, Object>> outputParams = objectMapper.readValue(
                        outputParamsJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                for (Map<String, Object> param : outputParams) {
                    String paramName = (String) param.get("name");
                    if (paramName == null || paramName.trim().isEmpty()) {
                        result.addError("开始节点参数名不能为空");
                    }
                }
            } catch (Exception e) {
                result.addError("开始节点输出参数配置格式错误: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public boolean supportsParallel() {
        // 开始节点不需要并行执行
        return false;
    }
}
