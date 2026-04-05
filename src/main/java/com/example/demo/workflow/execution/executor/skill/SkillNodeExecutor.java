package com.example.demo.workflow.execution.executor.skill;

import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;
import com.example.demo.workflow.execution.engine.ParameterResolver;
import com.example.demo.workflow.execution.error.ErrorCode;
import com.example.demo.workflow.execution.error.WorkflowExecutionException;
import com.example.demo.workflow.execution.executor.NodeExecutionResult;
import com.example.demo.workflow.execution.executor.NodeExecutor;
import com.example.demo.workflow.execution.executor.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能节点执行器
 * 根据执行方式和执行位置分发到对应的执行器
 *
 * 当前为框架实现，提供基本的执行流程
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SkillNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;
    private final ParameterResolver parameterResolver;

    // 可以通过注入的方式获取Skill服务
    // private final SkillService skillService;

    @Override
    public String getNodeType() {
        return "skill";
    }

    @Override
    public NodeExecutionResult execute(WorkflowNodeEntity node,
                                        Map<String, Object> inputs,
                                        ExecutionContext context) {
        log.info("执行技能节点: nodeUuid={}, nodeName={}, skillId={}",
                node.getNodeUuid(), node.getName(), node.getSkillId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取 Skill 定义
            String skillId = node.getSkillId();
            if (skillId == null || skillId.isEmpty()) {
                throw new WorkflowExecutionException(
                        ErrorCode.SKILL_NOT_FOUND,
                        node.getNodeUuid(),
                        node.getName(),
                        "节点未关联Skill"
                );
            }

            // 2. 解析并验证输入参数
            Map<String, Object> resolvedInputs = resolveAndValidateInputs(node, inputs, context);

            // 3. 获取执行配置
            String executionType = getExecutionType(node);
            String executionLocation = getExecutionLocation(node);

            log.info("技能执行配置: skillId={}, execType={}, execLocation={}",
                    skillId, executionType, executionLocation);

            // 4. 执行技能（当前为框架实现）
            SkillExecutionResult result = executeSkill(
                    skillId,
                    node.getSkillSnapshot(),
                    executionType,
                    executionLocation,
                    resolvedInputs,
                    context
            );

            long durationMs = System.currentTimeMillis() - startTime;

            // 5. 处理结果
            if (!result.isSuccess()) {
                return handleExecutionFailure(node, result, context);
            }

            // 6. 映射输出参数
            Map<String, Object> outputs = mapOutputs(node, result.getOutputs());

            log.info("技能节点执行完成: nodeUuid={}, skillId={}, durationMs={}",
                    node.getNodeUuid(), skillId, durationMs);

            return NodeExecutionResult.builder()
                    .success(true)
                    .outputs(outputs)
                    .logs(result.getLogs())
                    .durationMs(durationMs)
                    .build();

        } catch (WorkflowExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("技能节点执行异常: nodeUuid={}, skillId={}",
                    node.getNodeUuid(), node.getSkillId(), e);
            throw new WorkflowExecutionException(
                    ErrorCode.SKILL_EXECUTION_FAILED,
                    node.getNodeUuid(),
                    node.getName(),
                    "技能执行失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 解析并验证输入参数
     */
    private Map<String, Object> resolveAndValidateInputs(WorkflowNodeEntity node,
                                                         Map<String, Object> inputs,
                                                         ExecutionContext context) {
        Map<String, Object> resolved = new HashMap<>();

        // 解析节点配置的输入参数
        String inputParamsJson = node.getInputParams();
        if (inputParamsJson != null && !inputParamsJson.isEmpty()) {
            try {
                List<Map<String, Object>> inputParams = objectMapper.readValue(
                        inputParamsJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                for (Map<String, Object> param : inputParams) {
                    String paramName = (String) param.get("name");
                    Boolean required = (Boolean) param.get("required");

                    Object value = inputs.get(paramName);

                    // 必填校验
                    if (Boolean.TRUE.equals(required) && value == null) {
                        throw new WorkflowExecutionException(
                                ErrorCode.PARAM_REQUIRED_MISSING,
                                node.getNodeUuid(),
                                node.getName(),
                                "必填参数缺失: " + paramName
                        );
                    }

                    if (value != null) {
                        resolved.put(paramName, value);
                    }
                }

            } catch (JsonProcessingException e) {
                log.warn("解析输入参数配置失败: {}", e.getMessage());
            }
        }

        // 如果没有配置，直接使用传入的输入
        if (resolved.isEmpty() && !inputs.isEmpty()) {
            resolved.putAll(inputs);
        }

        return resolved;
    }

    /**
     * 获取执行方式
     */
    private String getExecutionType(WorkflowNodeEntity node) {
        // 从节点配置中获取
        String config = node.getConfig();
        if (config != null && !config.isEmpty()) {
            try {
                Map<String, Object> configMap = objectMapper.readValue(
                        config,
                        new TypeReference<Map<String, Object>>() {}
                );
                Object execType = configMap.get("executionType");
                if (execType != null) {
                    return execType.toString();
                }
            } catch (Exception e) {
                log.warn("解析节点配置失败", e);
            }
        }

        // 默认使用自动化脚本执行
        return "AUTOMATED";
    }

    /**
     * 获取执行位置
     */
    private String getExecutionLocation(WorkflowNodeEntity node) {
        String config = node.getConfig();
        if (config != null && !config.isEmpty()) {
            try {
                Map<String, Object> configMap = objectMapper.readValue(
                        config,
                        new TypeReference<Map<String, Object>>() {}
                );
                Object execLocation = configMap.get("executionLocation");
                if (execLocation != null) {
                    return execLocation.toString();
                }
            } catch (Exception e) {
                log.warn("解析节点配置失败", e);
            }
        }

        // 默认在服务端执行
        return "SERVICE";
    }

    /**
     * 执行技能（框架实现）
     */
    private SkillExecutionResult executeSkill(String skillId,
                                               String skillSnapshot,
                                               String executionType,
                                               String executionLocation,
                                               Map<String, Object> inputs,
                                               ExecutionContext context) {
        log.info("执行技能: skillId={}, type={}, location={}",
                skillId, executionType, executionLocation);

        // TODO: 实现实际的技能执行逻辑
        // 1. 根据 executionType 选择执行器 (AI_AGENT / AUTOMATED)
        // 2. 根据 executionLocation 选择执行位置 (CLIENT / SERVICE)
        // 3. 调用对应的执行器

        // 当前为模拟实现
        return SkillExecutionResult.builder()
                .success(true)
                .outputs(new HashMap<>(inputs))
                .logs("技能执行完成 (模拟)")
                .build();
    }

    /**
     * 处理执行失败
     */
    private NodeExecutionResult handleExecutionFailure(WorkflowNodeEntity node,
                                                       SkillExecutionResult result,
                                                       ExecutionContext context) {
        log.error("技能节点执行失败: nodeUuid={}, error={}",
                node.getNodeUuid(), result.getErrorMessage());

        return NodeExecutionResult.builder()
                .success(false)
                .errorMessage(result.getErrorMessage())
                .errorStack(result.getErrorStack())
                .build();
    }

    /**
     * 映射输出参数
     */
    private Map<String, Object> mapOutputs(WorkflowNodeEntity node,
                                            Map<String, Object> rawOutputs) {
        Map<String, Object> outputs = new HashMap<>();

        String outputParamsJson = node.getOutputParams();
        if (outputParamsJson != null && !outputParamsJson.isEmpty()) {
            try {
                List<Map<String, Object>> outputParams = objectMapper.readValue(
                        outputParamsJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                for (Map<String, Object> param : outputParams) {
                    String paramName = (String) param.get("name");
                    Object value = rawOutputs.get(paramName);
                    outputs.put(paramName, value);
                }

            } catch (Exception e) {
                log.warn("映射输出参数失败", e);
            }
        }

        // 如果没有配置输出参数映射，返回原始输出
        if (outputs.isEmpty() && rawOutputs != null) {
            outputs.putAll(rawOutputs);
        }

        return outputs;
    }

    @Override
    public ValidationResult validate(WorkflowNodeEntity node) {
        ValidationResult result = ValidationResult.success();

        if (node.getSkillId() == null || node.getSkillId().isEmpty()) {
            result.addError("技能节点必须关联一个Skill");
        }

        return result;
    }

    @Override
    public boolean supportsParallel() {
        return true;
    }

    @Override
    public Long getTimeout(WorkflowNodeEntity node) {
        // 从节点配置中获取超时设置
        String config = node.getConfig();
        if (config != null && !config.isEmpty()) {
            try {
                Map<String, Object> configMap = objectMapper.readValue(
                        config,
                        new TypeReference<Map<String, Object>>() {}
                );
                Object timeout = configMap.get("timeout");
                if (timeout instanceof Number) {
                    return ((Number) timeout).longValue();
                }
            } catch (Exception e) {
                log.warn("解析超时配置失败", e);
            }
        }

        // 默认5分钟超时
        return 5 * 60 * 1000L;
    }

    /**
     * 技能执行结果
     */
    @lombok.Data
    @lombok.Builder
    private static class SkillExecutionResult {
        private boolean success;
        private Map<String, Object> outputs;
        private String logs;
        private Long durationMs;
        private String errorMessage;
        private String errorStack;

        public static SkillExecutionResult success(Map<String, Object> outputs) {
            return SkillExecutionResult.builder()
                    .success(true)
                    .outputs(outputs)
                    .build();
        }

        public static SkillExecutionResult failure(String errorMessage) {
            return SkillExecutionResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
