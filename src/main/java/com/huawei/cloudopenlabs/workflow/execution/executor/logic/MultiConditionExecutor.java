/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.executor.logic;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowConnectionEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.execution.context.ExecutionContext;
import com.huawei.cloudopenlabs.workflow.execution.evaluator.ConditionEvaluator;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutionResult;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutor;
import com.huawei.cloudopenlabs.workflow.execution.executor.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多路条件节点执行器
 * 实现多分支条件判断（类似 switch-case）
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MultiConditionExecutor implements NodeExecutor {

    private final ConditionEvaluator conditionEvaluator;

    @Override
    public String getNodeType() {
        return "condition_multi";
    }

    @Override
    public NodeExecutionResult execute(WorkflowNodeEntity node,
                                        Map<String, Object> inputs,
                                        ExecutionContext context) {
        log.info("Executing multi-condition node: nodeUuid={}, nodeName={}",
                node.getNodeUuid(), node.getName());

        try {
            // 1. 获取多条件配置
            String conditionsJson = node.getConditions();
            if (conditionsJson == null || conditionsJson.isEmpty()) {
                log.warn("Multi-condition node has no conditions configured: nodeUuid={}", node.getNodeUuid());
                return NodeExecutionResult.failure("多条件配置未配置");
            }

            // 2. 评估多条件，获取匹配的分支ID
            String matchedBranchId = conditionEvaluator.evaluateMultiConditions(
                    conditionsJson,
                    context,
                    node.getNodeUuid()
            );

            if (matchedBranchId == null) {
                log.warn("Multi-condition node has no matching branch: nodeUuid={}", node.getNodeUuid());
                matchedBranchId = "default";
            }

            log.info("Multi-condition matched: nodeUuid={}, matchedBranchId={}",
                    node.getNodeUuid(), matchedBranchId);

            // 3. 获取对应分支的目标节点
            List<String> nextNodes = getNextNodesByBranch(node, matchedBranchId, context);

            // 4. 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("matched_branch", matchedBranchId);

            log.info("Multi-condition node execution completed: nodeUuid={}, matchedBranch={}, nextNodes={}",
                    node.getNodeUuid(), matchedBranchId, nextNodes);

            return NodeExecutionResult.builder()
                    .success(true)
                    .outputs(outputs)
                    .nextNodes(nextNodes)
                    .build();

        } catch (Exception e) {
            log.error("Multi-condition node execution exception: nodeUuid={}", node.getNodeUuid(), e);
            return NodeExecutionResult.failure(
                    "多条件评估异常: " + e.getMessage(),
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

        if (context.getDefinition() != null &&
            context.getDefinition().getConnections() != null) {

            for (WorkflowConnectionEntity conn : context.getDefinition().getConnections()) {
                if (node.getId().equals(conn.getSourceNodeId())) {
                    String connBranch = conn.getBranchLabel();
                    if (branchLabel.equals(connBranch)) {
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

        String conditionsJson = node.getConditions();
        if (conditionsJson == null || conditionsJson.isEmpty()) {
            result.addError("多路条件节点必须配置条件");
        }

        return result;
    }

    @Override
    public boolean supportsParallel() {
        return false;
    }
}
