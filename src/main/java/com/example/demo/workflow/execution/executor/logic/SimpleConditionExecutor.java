package com.example.demo.workflow.execution.executor.logic;

import com.example.demo.workflow.entity.WorkflowConnectionEntity;
import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;
import com.example.demo.workflow.execution.evaluator.ConditionEvaluator;
import com.example.demo.workflow.execution.executor.NodeExecutionResult;
import com.example.demo.workflow.execution.executor.NodeExecutor;
import com.example.demo.workflow.execution.executor.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单条件节点执行器
 * 实现二分支条件判断（是/否）
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SimpleConditionExecutor implements NodeExecutor {

    private final ConditionEvaluator conditionEvaluator;

    @Override
    public String getNodeType() {
        return "condition_simple";
    }

    @Override
    public NodeExecutionResult execute(WorkflowNodeEntity node,
                                        Map<String, Object> inputs,
                                        ExecutionContext context) {
        log.info("执行简单条件节点: nodeUuid={}, nodeName={}",
                node.getNodeUuid(), node.getName());

        try {
            // 1. 获取条件配置
            String conditionsJson = node.getConditions();
            if (conditionsJson == null || conditionsJson.isEmpty()) {
                log.warn("简单条件节点没有配置条件表达式: nodeUuid={}", node.getNodeUuid());
                return NodeExecutionResult.failure("条件表达式未配置");
            }

            // 2. 评估条件表达式
            boolean result = conditionEvaluator.evaluate(
                    conditionsJson,
                    context,
                    node.getNodeUuid()
            );

            log.info("条件评估结果: nodeUuid={}, result={}", node.getNodeUuid(), result);

            // 3. 确定下一步要执行的分支
            String branchLabel = result ? "true" : "false";
            List<String> nextNodes = getNextNodesByBranch(node, branchLabel, context);

            // 4. 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("condition_result", result);
            outputs.put("branch", branchLabel);

            log.info("简单条件节点执行完成: nodeUuid={}, branch={}, nextNodes={}",
                    node.getNodeUuid(), branchLabel, nextNodes);

            return NodeExecutionResult.builder()
                    .success(true)
                    .outputs(outputs)
                    .nextNodes(nextNodes)
                    .build();

        } catch (Exception e) {
            log.error("简单条件节点执行异常: nodeUuid={}", node.getNodeUuid(), e);
            return NodeExecutionResult.failure(
                    "条件评估异常: " + e.getMessage(),
                    e instanceof Exception ? (Exception) e : new RuntimeException(e)
            );
        }
    }

    /**
     * 根据分支标签获取下一步节点
     */
    private List<String> getNextNodesByBranch(WorkflowNodeEntity node,
                                               String branchLabel,
                                               ExecutionContext context) {
        List<String> nextNodes = new ArrayList<>();

        // 从连线中获取对应分支的目标节点
        if (context.getDefinition() != null &&
            context.getDefinition().getConnections() != null) {

            for (WorkflowConnectionEntity conn : context.getDefinition().getConnections()) {
                // 检查连线源节点是否是当前节点
                if (node.getId().equals(conn.getSourceNodeId())) {
                    // 检查分支标签是否匹配
                    String connBranch = conn.getBranchLabel();
                    if (branchLabel.equals(connBranch) || connBranch == null) {
                        // 获取目标节点UUID
                        var targetNode = context.getDefinition().getNodeById(conn.getTargetNodeId());
                        if (targetNode != null) {
                            nextNodes.add(targetNode.getNodeUuid());
                        }
                    }
                }
            }
        }

        return nextNodes;
    }

    @Override
    public ValidationResult validate(WorkflowNodeEntity node) {
        ValidationResult result = ValidationResult.success();

        // 验证条件表达式配置
        String conditionsJson = node.getConditions();
        if (conditionsJson == null || conditionsJson.isEmpty()) {
            result.addError("简单条件节点必须配置条件表达式");
        }

        return result;
    }

    @Override
    public boolean supportsParallel() {
        // 条件节点不支持并行执行（需要根据条件选择分支）
        return false;
    }
}
