package com.huawei.cloudopenlabs.workflow.execution.executor.skill;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.execution.context.ExecutionContext;
import com.huawei.cloudopenlabs.workflow.execution.engine.ParameterResolver;
import com.huawei.cloudopenlabs.workflow.execution.error.ErrorCode;
import com.huawei.cloudopenlabs.workflow.execution.error.WorkflowExecutionException;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutionResult;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutor;
import com.huawei.cloudopenlabs.workflow.execution.executor.ValidationResult;
import com.huawei.cloudopenlabs.workflow.execution.executor.skill.dto.SkillExecutionRequest;
import com.huawei.cloudopenlabs.workflow.kafka.dto.SkillExecutionKafkaRequest;
import com.huawei.cloudopenlabs.workflow.kafka.dto.SkillExecutionKafkaResponse;
import com.huawei.cloudopenlabs.workflow.kafka.service.SkillKafkaService;
import com.huawei.cloudopenlabs.skill.service.SkillService;
import com.huawei.cloudopenlabs.skill.dto.SkillResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 技能节点执行器
 * 通过Kafka向Executor微服务发送执行请求，阻塞等待执行结果
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SkillNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;
    private final ParameterResolver parameterResolver;
    private final SkillKafkaService skillKafkaService;

    @Autowired(required = false)
    private SkillService skillService;

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
            // 1. 获取 Skill ID
            String skillId = node.getSkillId();
            if (skillId == null || skillId.isEmpty()) {
                throw new WorkflowExecutionException(
                        ErrorCode.SKILL_NOT_FOUND,
                        node.getNodeUuid(),
                        node.getName(),
                        "节点未关联Skill"
                );
            }

            // 2. 构建执行请求（包含所有执行所需信息）
            SkillExecutionRequest request = buildExecutionRequest(node, inputs, context);

            // 3. 构建Kafka请求消息
            SkillExecutionKafkaRequest kafkaRequest = SkillExecutionKafkaRequest.builder()
                    .requestId(UUID.randomUUID().toString())
                    .executionId(context.getExecutionId())
                    .workflowId(context.getWorkflowId())
                    .nodeUuid(node.getNodeUuid())
                    .callbackTopic("${skill-executor.kafka.response-topic}")
                    .timestamp(System.currentTimeMillis())
                    .request(request)
                    .build();

            log.info("通过Kafka发送Skill执行请求: requestId={}, executionType={}",
                    kafkaRequest.getRequestId(), request.getExecutionType());

            // 4. 发送请求并等待响应
            SkillExecutionKafkaResponse response = skillKafkaService.sendAndReceive(kafkaRequest);

            long durationMs = System.currentTimeMillis() - startTime;

            // 5. 处理响应
            if (!response.isSuccess()) {
                log.error("Skill执行失败: nodeUuid={}, error={}", node.getNodeUuid(), response.getErrorMessage());
                return NodeExecutionResult.builder()
                        .success(false)
                        .errorMessage(response.getErrorMessage())
                        .errorStack(response.getErrorStack())
                        .durationMs(durationMs)
                        .build();
            }

            // 6. 映射输出参数
            Map<String, Object> outputs = mapOutputs(node, response.getOutputs());

            log.info("技能节点执行完成: nodeUuid={}, skillId={}, durationMs={}",
                    node.getNodeUuid(), skillId, durationMs);

            return NodeExecutionResult.builder()
                    .success(true)
                    .outputs(outputs)
                    .logs(response.getLogs())
                    .durationMs(durationMs)
                    .build();

        } catch (WorkflowExecutionException e) {
            throw e;
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("技能节点执行超时: nodeUuid={}, skillId={}",
                    node.getNodeUuid(), node.getSkillId());
            throw new WorkflowExecutionException(
                    ErrorCode.SKILL_EXECUTION_FAILED,
                    node.getNodeUuid(),
                    node.getName(),
                    "技能执行超时"
            );
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
     * 构建执行请求
     * 从节点配置和Skill信息中收集所有执行所需的数据
     */
    private SkillExecutionRequest buildExecutionRequest(WorkflowNodeEntity node,
                                                       Map<String, Object> inputs,
                                                       ExecutionContext context) {
        // 解析skillSnapshot获取Skill基本信息
        String skillId = node.getSkillId();
        String skillName = node.getName();
        String description = "";
        String executionType = "AUTOMATED";
        String suitePath = null;
        byte[] suiteContent = null;
        List<SkillExecutionRequest.SkillParameterDef> inputParams = new ArrayList<>();
        List<SkillExecutionRequest.SkillParameterDef> outputParams = new ArrayList<>();

        // 从skillSnapshot解析信息
        String skillSnapshot = node.getSkillSnapshot();
        if (skillSnapshot != null && !skillSnapshot.isEmpty()) {
            try {
                Map<String, Object> snapshot = objectMapper.readValue(
                        skillSnapshot,
                        new TypeReference<Map<String, Object>>() {}
                );

                skillName = (String) snapshot.getOrDefault("name", skillName);
                description = (String) snapshot.getOrDefault("description", "");
                executionType = (String) snapshot.getOrDefault("executionType", "AUTOMATED");
                suitePath = (String) snapshot.get("suitePath");

                // 解析输入参数定义
                Object inputParamsObj = snapshot.get("inputParameters");
                if (inputParamsObj instanceof List) {
                    inputParams = parseParameterDefs((List<?>) inputParamsObj);
                }

                // 解析输出参数定义
                Object outputParamsObj = snapshot.get("outputParameters");
                if (outputParamsObj instanceof List) {
                    outputParams = parseParameterDefs((List<?>) outputParamsObj);
                }

            } catch (Exception e) {
                log.warn("解析skillSnapshot失败: {}", e.getMessage());
            }
        }

        // 获取执行配置
        String config = node.getConfig();
        if (config != null && !config.isEmpty()) {
            try {
                Map<String, Object> configMap = objectMapper.readValue(
                        config,
                        new TypeReference<Map<String, Object>>() {}
                );
                if (configMap.containsKey("executionType")) {
                    executionType = configMap.get("executionType").toString();
                }
            } catch (Exception e) {
                log.warn("解析节点配置失败", e);
            }
        }

        // 从inputs获取已解析的实际输入值（由ParameterResolver解析）
        log.debug("构建Skill执行请求: inputs={}", inputs);

        // 如果inputParams为空但inputs有值，直接从inputs构建参数定义
        if ((inputParams == null || inputParams.isEmpty()) && inputs != null && !inputs.isEmpty()) {
            log.debug("从inputs构建参数定义");
            inputParams = new ArrayList<>();
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                SkillExecutionRequest.SkillParameterDef param = SkillExecutionRequest.SkillParameterDef.builder()
                        .name(entry.getKey())
                        .type(entry.getValue() instanceof Integer ? "Integer" :
                              entry.getValue() instanceof Double ? "Double" : "String")
                        .value(entry.getValue())
                        .build();
                inputParams.add(param);
            }
        } else if (inputParams != null) {
            // 将inputs的值设置到已有的参数定义中
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                for (SkillExecutionRequest.SkillParameterDef param : inputParams) {
                    if (param.getName().equals(entry.getKey())) {
                        param.setValue(entry.getValue());
                        break;
                    }
                }
            }
        }

        // 读取执行套件内容
        if (suitePath != null && !suitePath.isEmpty()) {
            try {
                Path path = Paths.get(suitePath);
                if (Files.exists(path)) {
                    suiteContent = Files.readAllBytes(path);
                    log.debug("读取执行套件: path={}, size={} bytes", suitePath, suiteContent.length);
                }
            } catch (Exception e) {
                log.warn("读取执行套件失败: {}", e.getMessage());
            }
        }

        // 如果没有套件内容，尝试从SkillService获取
        if (suiteContent == null && skillService != null) {
            try {
                SkillResponse skillResponse = skillService.getSkillById(skillId);
                if (skillResponse != null && skillResponse.getSuitePath() != null) {
                    Path path = Paths.get(skillResponse.getSuitePath());
                    if (Files.exists(path)) {
                        suiteContent = Files.readAllBytes(path);
                        log.debug("从SkillService读取执行套件: size={} bytes", suiteContent.length);
                    }
                    // 使用Skill定义的执行类型
                    if (skillResponse.getExecutionType() != null) {
                        executionType = skillResponse.getExecutionType().name();
                    }
                }
            } catch (Exception e) {
                log.warn("从SkillService获取执行套件失败: {}", e.getMessage());
            }
        }

        return SkillExecutionRequest.builder()
                .skillId(skillId)
                .skillName(skillName)
                .description(description)
                .executionType(executionType)
                .executionLocation("SERVICE")
                .suitePath(suitePath)
                .suiteContent(suiteContent)
                .inputParameters(inputParams)
                .outputParameters(outputParams)
                .build();
    }

    /**
     * 解析参数定义列表
     */
    private List<SkillExecutionRequest.SkillParameterDef> parseParameterDefs(List<?> paramsList) {
        List<SkillExecutionRequest.SkillParameterDef> result = new ArrayList<>();
        for (Object obj : paramsList) {
                if (obj instanceof Map) {
                    Map<String, Object> param = (Map<String, Object>) obj;
                    SkillExecutionRequest.SkillParameterDef def = SkillExecutionRequest.SkillParameterDef.builder()
                            .name((String) param.get("paramName"))
                            .type((String) param.get("paramType"))
                            .description((String) param.get("description"))
                            .required((Boolean) param.get("required"))
                            .build();
                    result.add(def);
                }
            }
        return result;
    }

    /**
     * 映射输出参数
     */
    private Map<String, Object> mapOutputs(WorkflowNodeEntity node,
                                            Map<String, Object> rawOutputs) {
        Map<String, Object> outputs = new HashMap<>();

        if (rawOutputs == null) {
            return outputs;
        }

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
                    if (value != null) {
                        outputs.put(paramName, value);
                    }
                }

            } catch (Exception e) {
                log.warn("映射输出参数失败", e);
            }
        }

        // 如果没有配置输出参数映射，返回原始输出
        if (outputs.isEmpty()) {
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
}
