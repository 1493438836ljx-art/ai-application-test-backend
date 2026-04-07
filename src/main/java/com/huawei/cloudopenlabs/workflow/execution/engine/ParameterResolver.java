package com.huawei.cloudopenlabs.workflow.execution.engine;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.execution.context.ExecutionContext;
import com.huawei.cloudopenlabs.workflow.execution.error.ErrorCode;
import com.huawei.cloudopenlabs.workflow.execution.error.WorkflowExecutionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数解析器
 * 解析节点输入参数配置，从上下文中获取值
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ParameterResolver {

    private final ObjectMapper objectMapper;

    /**
     * 参数引用模式: ${节点名.参数名} 或 ${节点UUID.参数名}
     */
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^\\$\\{(.+?)\\}$");

    /**
     * 解析节点输入参数
     *
     * @param node    节点定义
     * @param context 执行上下文
     * @return 解析后的参数Map
     */
    public Map<String, Object> resolveInputs(WorkflowNodeEntity node, ExecutionContext context) {
        Map<String, Object> inputs = new HashMap<>();

        String inputParamsJson = node.getInputParams();
        if (inputParamsJson == null || inputParamsJson.isEmpty()) {
            return inputs;
        }

        try {
            List<Map<String, Object>> inputParams = objectMapper.readValue(
                    inputParamsJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> param : inputParams) {
                String paramName = (String) param.get("name");
                Object value = resolveParameterValue(param, node.getNodeUuid(), context);

                if (value != null) {
                    inputs.put(paramName, value);
                }
            }

        } catch (Exception e) {
            log.error("解析节点输入参数失败: nodeUuid={}", node.getNodeUuid(), e);
            throw new WorkflowExecutionException(
                    ErrorCode.PARAM_RESOLVE_FAILED,
                    node.getNodeUuid(),
                    node.getName(),
                    "参数解析失败: " + e.getMessage(),
                    e
            );
        }

        return inputs;
    }

    /**
     * 解析单个参数值
     */
    @SuppressWarnings("unchecked")
    private Object resolveParameterValue(Map<String, Object> param,
                                          String currentNodeUuid,
                                          ExecutionContext context) {
        String paramName = (String) param.get("name");
        String paramType = (String) param.get("type");
        Boolean required = (Boolean) param.get("required");
        Object defaultValue = param.get("defaultValue");

        Object value = null;

        // 优先使用 valueSource 格式（新格式）
        Map<String, Object> valueSource = (Map<String, Object>) param.get("valueSource");
        if (valueSource != null) {
            String sourceType = (String) valueSource.get("type");
            Object sourceValue = valueSource.get("value");

            if ("literal".equals(sourceType)) {
                // 字面值
                value = convertValue(sourceValue, paramType);
            } else if ("reference".equals(sourceType)) {
                // 引用其他节点输出
                value = resolveReference(sourceValue.toString(), currentNodeUuid, context);
            } else if ("expression".equals(sourceType)) {
                // 表达式（简单实现，直接返回字符串）
                value = sourceValue;
            }
        } else {
            // 兼容旧格式：直接使用 value 字段
            Object directValue = param.get("value");
            if (directValue != null) {
                String strValue = directValue.toString();
                // 检查是否是引用格式 ${xxx.yyy}
                Matcher matcher = REFERENCE_PATTERN.matcher(strValue);
                if (matcher.matches()) {
                    // 引用其他节点输出
                    value = resolveReference(strValue, currentNodeUuid, context);
                } else {
                    // 字面值
                    value = convertValue(directValue, paramType);
                }
            }
        }

        // 如果值为空，使用默认值
        if (value == null && defaultValue != null && !"".equals(defaultValue)) {
            value = convertValue(defaultValue, paramType);
        }

        // 必填参数校验
        if (Boolean.TRUE.equals(required) && value == null) {
            throw new WorkflowExecutionException(
                    ErrorCode.PARAM_REQUIRED_MISSING,
                    currentNodeUuid,
                    null,
                    "必填参数缺失: " + paramName
            );
        }

        return value;
    }

    /**
     * 解析引用表达式
     * 格式: ${节点名称.输出参数名} 或 ${节点UUID.输出参数名}
     */
    private Object resolveReference(String reference,
                                     String currentNodeUuid,
                                     ExecutionContext context) {
        // 解析引用格式: ${xxx.yyy}
        String refContent = reference;
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        if (matcher.matches()) {
            refContent = matcher.group(1);
        }

        // 分割节点和参数
        String[] parts = refContent.split("\\.", 2);
        if (parts.length != 2) {
            throw new WorkflowExecutionException(
                    ErrorCode.PARAM_REFERENCE_INVALID,
                    currentNodeUuid,
                    null,
                    "无效的引用格式: " + reference
            );
        }

        String nodeIdentifier = parts[0];
        String paramName = parts[1];

        // 查找目标节点UUID（可能是节点名称或UUID）
        String targetNodeUuid = resolveNodeIdentifier(nodeIdentifier, context);

        if (targetNodeUuid == null) {
            throw new WorkflowExecutionException(
                    ErrorCode.NODE_NOT_FOUND,
                    currentNodeUuid,
                    null,
                    "引用的节点不存在: " + nodeIdentifier
            );
        }

        // 验证是否可引用（只能引用前置节点）
        if (!context.canReferenceNode(currentNodeUuid, targetNodeUuid)) {
            throw new WorkflowExecutionException(
                    ErrorCode.PARAM_REFERENCE_INVALID,
                    currentNodeUuid,
                    null,
                    String.format("无法引用非前置节点的输出: %s（当前节点: %s）",
                            nodeIdentifier, currentNodeUuid)
            );
        }

        // 获取节点输出
        Object value = context.getNodeOutput(targetNodeUuid, paramName);

        if (value == null) {
            log.warn("引用的参数值为空: node={}, param={}", nodeIdentifier, paramName);
        }

        return value;
    }

    /**
     * 解析节点标识符（名称或UUID）
     */
    private String resolveNodeIdentifier(String identifier, ExecutionContext context) {
        // 首先尝试直接匹配UUID
        if (context.getExecutionGraph() != null &&
            context.getExecutionGraph().hasNode(identifier)) {
            return identifier;
        }

        // 尝试通过节点名称查找
        var node = context.getNodeByName(identifier);
        if (node != null) {
            return node.getNodeUuid();
        }

        return null;
    }

    /**
     * 类型转换
     */
    @SuppressWarnings("unchecked")
    private Object convertValue(Object value, String targetType) {
        if (value == null) {
            return null;
        }

        if (targetType == null || targetType.isEmpty()) {
            return value;
        }

        String strValue = value.toString();

        try {
            switch (targetType.toLowerCase()) {
                case "string":
                    return strValue;

                case "integer":
                case "int":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(strValue);

                case "long":
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    return Long.parseLong(strValue);

                case "double":
                case "number":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(strValue);

                case "boolean":
                case "bool":
                    if (value instanceof Boolean) {
                        return value;
                    }
                    return Boolean.parseBoolean(strValue);

                case "array":
                case "list":
                    if (value instanceof List) {
                        return value;
                    }
                    // 尝试解析JSON数组
                    return objectMapper.readValue(strValue, List.class);

                case "object":
                case "map":
                    if (value instanceof Map) {
                        return value;
                    }
                    // 尝试解析JSON对象
                    return objectMapper.readValue(strValue, Map.class);

                default:
                    return value;
            }
        } catch (Exception e) {
            log.warn("类型转换失败: value={}, targetType={}, error={}",
                    value, targetType, e.getMessage());
            return value;
        }
    }

    /**
     * 解析条件表达式中的值
     * 支持引用和字面值
     */
    public Object resolveValue(Object value, String currentNodeUuid, ExecutionContext context) {
        if (value == null) {
            return null;
        }

        String strValue = value.toString();

        // 如果是引用表达式
        Matcher matcher = REFERENCE_PATTERN.matcher(strValue);
        if (matcher.matches()) {
            return resolveReference(strValue, currentNodeUuid, context);
        }

        return value;
    }
}
