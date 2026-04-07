package com.example.demo.workflow.execution.evaluator;

import com.example.demo.workflow.execution.context.ExecutionContext;
import com.example.demo.workflow.execution.error.ErrorCode;
import com.example.demo.workflow.execution.error.WorkflowExecutionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 条件表达式评估器
 * 支持多种比较运算符
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConditionEvaluator {

    private final ObjectMapper objectMapper;

    /**
     * 参数引用模式: ${节点名.参数名}
     */
    private static final String REFERENCE_PATTERN = "^\\$\\{.+\\}$";

    /**
     * 评估条件表达式
     *
     * @param expressionJson 条件表达式JSON
     * @param context         执行上下文
     * @param currentNodeUuid 当前节点UUID
     * @return 评估结果
     */
    public boolean evaluate(String expressionJson, ExecutionContext context, String currentNodeUuid) {
        try {
            Map<String, Object> expression = objectMapper.readValue(
                    expressionJson,
                    new TypeReference<Map<String, Object>>() {}
            );
            return evaluateExpression(expression, context, currentNodeUuid);
        } catch (Exception e) {
            log.error("评估条件表达式失败: {}", expressionJson, e);
            throw new WorkflowExecutionException(
                    ErrorCode.PARAM_RESOLVE_FAILED,
                    currentNodeUuid,
                    null,
                    "条件表达式评估失败: " + e.getMessage()
            );
        }
    }

    /**
     * 评估多条件配置
     *
     * @param conditionsJson  条件配置JSON
     * @param context         执行上下文
     * @param currentNodeUuid 当前节点UUID
     * @return 匹配的分支ID
     */
    public String evaluateMultiConditions(String conditionsJson,
                                           ExecutionContext context,
                                           String currentNodeUuid) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    conditionsJson,
                    new TypeReference<Map<String, Object>>() {}
            );

            List<Map<String, Object>> cases = (List<Map<String, Object>>) config.get("cases");

            if (cases == null || cases.isEmpty()) {
                return (String) config.get("defaultBranch");
            }

            // 按优先级排序
            cases.sort((a, b) -> {
                Integer priorityA = (Integer) a.getOrDefault("priority", 100);
                Integer priorityB = (Integer) b.getOrDefault("priority", 100);
                return priorityA.compareTo(priorityB);
            });

            // 依次评估条件
            for (Map<String, Object> caseConfig : cases) {
                String caseId = (String) caseConfig.get("id");
                String conditionJson = (String) caseConfig.get("condition");

                if (conditionJson != null && !conditionJson.isEmpty()) {
                    boolean matched = evaluate(conditionJson, context, currentNodeUuid);
                    if (matched) {
                        log.info("条件分支匹配: caseId={}", caseId);
                        return caseId;
                    }
                }
            }

            // 返回默认分支
            String defaultBranch = (String) config.get("defaultBranch");
            log.info("使用默认分支: defaultBranch={}", defaultBranch);
            return defaultBranch;

        } catch (Exception e) {
            log.error("评估多条件配置失败: {}", conditionsJson, e);
            throw new WorkflowExecutionException(
                    ErrorCode.PARAM_RESOLVE_FAILED,
                    currentNodeUuid,
                    null,
                    "多条件评估失败: " + e.getMessage()
            );
        }
    }

    /**
     * 评估单个条件表达式
     */
    private boolean evaluateExpression(Map<String, Object> expression,
                                        ExecutionContext context,
                                        String currentNodeUuid) {
        String operator = (String) expression.get("operator");
        Object leftOperand = expression.get("leftOperand");
        Object rightOperand = expression.get("rightOperand");

        // 解析操作数
        Object leftValue = resolveOperand(leftOperand, context, currentNodeUuid);
        Object rightValue = resolveOperand(rightOperand, context, currentNodeUuid);

        return compare(leftValue, rightValue, operator);
    }

    /**
     * 解析操作数
     */
    private Object resolveOperand(Object operand, ExecutionContext context, String currentNodeUuid) {
        if (operand == null) {
            return null;
        }

        String strOperand = operand.toString();

        // 如果是引用表达式 ${xxx.yyy}
        if (strOperand.startsWith("${") && strOperand.endsWith("}")) {
            String ref = strOperand.substring(2, strOperand.length() - 1);
            String[] parts = ref.split("\\.", 2);

            if (parts.length == 2) {
                String nodeIdentifier = parts[0];
                String paramName = parts[1];

                // 查找节点UUID
                String targetNodeUuid = resolveNodeUuid(nodeIdentifier, context);

                if (targetNodeUuid == null) {
                    throw new WorkflowExecutionException(
                            ErrorCode.NODE_NOT_FOUND,
                            currentNodeUuid,
                            null,
                            "引用的节点不存在: " + nodeIdentifier
                    );
                }

                return context.getNodeOutput(targetNodeUuid, paramName);
            }
        }

        return operand;
    }

    /**
     * 解析节点UUID
     */
    private String resolveNodeUuid(String identifier, ExecutionContext context) {
        // 先尝试直接匹配UUID
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
     * 执行比较
     */
    private boolean compare(Object left, Object right, String operator) {
        if (operator == null) {
            return false;
        }

        switch (operator.toLowerCase()) {
            // 相等性比较
            case "equals":
            case "==":
                return Objects.equals(left, right);

            case "notequals":
            case "!=":
                return !Objects.equals(left, right);

            // 数值比较
            case "greaterthan":
            case ">":
                return toNumber(left) > toNumber(right);

            case "greaterthanorequals":
            case ">=":
                return toNumber(left) >= toNumber(right);

            case "lessthan":
            case "<":
                return toNumber(left) < toNumber(right);

            case "lessthanorequals":
            case "<=":
                return toNumber(left) <= toNumber(right);

            // 字符串操作
            case "contains":
                return left != null && right != null &&
                        left.toString().contains(right.toString());

            case "notcontains":
                return left == null || right == null ||
                        !left.toString().contains(right.toString());

            case "startswith":
                return left != null && right != null &&
                        left.toString().startsWith(right.toString());

            case "endswith":
                return left != null && right != null &&
                        left.toString().endsWith(right.toString());

            // 空值检查
            case "isnull":
                return left == null;

            case "isnotnull":
                return left != null;

            case "isempty":
                return left == null || left.toString().isEmpty();

            case "isnotempty":
                return left != null && !left.toString().isEmpty();

            // 集合操作
            case "in":
                if (right instanceof Collection) {
                    return ((Collection<?>) right).contains(left);
                }
                if (right instanceof Object[]) {
                    return Arrays.asList((Object[]) right).contains(left);
                }
                return false;

            case "notin":
                if (right instanceof Collection) {
                    return !((Collection<?>) right).contains(left);
                }
                if (right instanceof Object[]) {
                    return !Arrays.asList((Object[]) right).contains(left);
                }
                return true;

            // 范围检查
            case "between":
                if (right instanceof List && ((List<?>) right).size() >= 2) {
                    List<?> range = (List<?>) right;
                    double value = toNumber(left);
                    double min = toNumber(range.get(0));
                    double max = toNumber(range.get(1));
                    return value >= min && value <= max;
                }
                return false;

            // 布尔操作
            case "and":
                return toBoolean(left) && toBoolean(right);

            case "or":
                return toBoolean(left) || toBoolean(right);

            case "not":
                return !toBoolean(left);

            // 正则匹配
            case "matches":
                if (left != null && right != null) {
                    return left.toString().matches(right.toString());
                }
                return false;

            default:
                log.warn("未知的比较运算符: {}", operator);
                return false;
        }
    }

    /**
     * 转换为数值
     */
    private double toNumber(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        try {
            return new BigDecimal(value.toString()).doubleValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 转换为布尔值
     */
    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        String str = value.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
}
