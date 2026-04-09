package com.huawei.cloudopenlabs.workflow.service.validation;

import com.huawei.cloudopenlabs.workflow.dto.ValidationResult;
import com.huawei.cloudopenlabs.workflow.entity.CompatibilityStatus;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowNodeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Skill 兼容性检查器
 * 检查 Skill 节点引用的 Skill 是否存在、是否被禁用、参数配置是否正确
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillCompatibilityChecker {

    private final WorkflowNodeMapper nodeMapper;
    private final ObjectMapper objectMapper;

    // 错误码常量
    public static final String SKILL_NOT_FOUND = "WF_SKILL_001";
    public static final String SKILL_DISABLED = "WF_SKILL_002";
    public static final String MISSING_REQUIRED_PARAM = "WF_SKILL_003";
    public static final String PARAM_TYPE_MISMATCH = "WF_SKILL_004";
    public static final String SKILL_VERSION_CHANGED = "WF_SKILL_005";
    public static final String SKILL_NOT_CONFIGURED = "WF_SKILL_006";

    /**
     * 检查工作流中所有 Skill 节点的兼容性
     *
     * @param workflowId 工作流ID
     * @return 验证结果
     */
    public ValidationResult check(String workflowId) {
        log.debug("开始 Skill 兼容性检查: workflowId={}", workflowId);
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);

        for (WorkflowNodeEntity node : nodes) {
            // 只检查 Skill 节点
            if (!"skill".equals(node.getType())) {
                continue;
            }

            CompatibilityStatus status = checkNode(node, result);
            updateCompatibilityStatus(node, status);
        }

        log.debug("Skill 兼容性检查完成: workflowId={}, valid={}, errors={}",
                workflowId, result.isValid(), result.getErrors().size());

        return result;
    }

    /**
     * 检查单个节点的兼容性
     *
     * @param node   节点实体
     * @param result 验证结果（用于收集错误）
     * @return 兼容性状态
     */
    public CompatibilityStatus checkNode(WorkflowNodeEntity node, ValidationResult result) {
        if (!"skill".equals(node.getType())) {
            return CompatibilityStatus.COMPATIBLE;
        }

        // 检查是否配置了 SkillId
        if (node.getSkillId() == null || node.getSkillId().isEmpty()) {
            if (result != null) {
                result.addError(SKILL_NOT_CONFIGURED,
                        "Skill 节点 '" + node.getName() + "' 未配置 Skill",
                        node.getNodeUuid(), "skillId");
            }
            return CompatibilityStatus.INVALID;
        }

        // TODO: 调用 SkillService 检查 Skill 是否存在和可用
        // 这里暂时简化实现，实际应该注入 SkillService 进行检查
        // SkillEntity skill = skillService.getById(node.getSkillId());
        // if (skill == null) { ... }
        // if (!skill.isEnabled()) { ... }

        // 检查参数配置
        ValidationResult paramResult = checkParameters(node);
        if (result != null) {
            result.merge(paramResult);
        }

        // 检查版本变更
        if (isVersionChanged(node)) {
            if (result != null) {
                result.addWarning(SKILL_VERSION_CHANGED,
                        "Skill 节点 '" + node.getName() + "' 引用的 Skill 版本已变更，建议重新配置",
                        node.getNodeUuid());
            }
        }

        // 根据参数检查结果确定最终状态
        if (paramResult.isValid()) {
            return CompatibilityStatus.COMPATIBLE;
        } else {
            return CompatibilityStatus.NEEDS_UPDATE;
        }
    }

    /**
     * 检查节点的参数配置
     *
     * @param node 节点实体
     * @return 验证结果
     */
    private ValidationResult checkParameters(WorkflowNodeEntity node) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        String inputParams = node.getInputParams();
        if (inputParams == null || inputParams.isEmpty()) {
            return result;
        }

        try {
            JsonNode paramsNode = objectMapper.readTree(inputParams);
            // TODO: 与 Skill 定义的参数进行比对
            // 这里暂时只做基本的 JSON 格式验证

            if (paramsNode.isArray()) {
                for (JsonNode param : paramsNode) {
                    if (param.has("value")) {
                        String value = param.get("value").asText();
                        // 检查参数引用语法
                        if (value.startsWith("${") && !value.endsWith("}")) {
                            result.addError(PARAM_TYPE_MISMATCH,
                                    "节点 '" + node.getName() + "' 的参数引用语法错误: " + value,
                                    node.getNodeUuid(), "inputParams");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析输入参数失败: nodeUuid={}", node.getNodeUuid(), e);
            result.addError(PARAM_TYPE_MISMATCH,
                    "节点 '" + node.getName() + "' 的输入参数格式错误",
                    node.getNodeUuid(), "inputParams");
        }

        return result;
    }

    /**
     * 检查 Skill 版本是否变更
     *
     * @param node 节点实体
     * @return 是否变更
     */
    private boolean isVersionChanged(WorkflowNodeEntity node) {
        String snapshot = node.getSkillSnapshot();
        if (snapshot == null || snapshot.isEmpty()) {
            return true;
        }

        try {
            JsonNode snapshotNode = objectMapper.readTree(snapshot);
            String snapshotVersion = snapshotNode.has("version") ?
                    snapshotNode.get("version").asText() : null;

            // TODO: 与当前 Skill 版本比较
            // 这里暂时返回 false，实际应该获取当前 Skill 版本进行比较
            return false;
        } catch (Exception e) {
            log.warn("解析 Skill 快照失败: nodeUuid={}", node.getNodeUuid(), e);
            return true;
        }
    }

    /**
     * 更新节点的兼容性状态
     *
     * @param node   节点实体
     * @param status 兼容性状态
     */
    private void updateCompatibilityStatus(WorkflowNodeEntity node, CompatibilityStatus status) {
        if (!status.name().equals(node.getCompatibilityStatus())) {
            node.setCompatibilityStatus(status.name());
            nodeMapper.updateById(node);
            log.info("更新节点兼容性状态: nodeUuid={}, status={}", node.getNodeUuid(), status);
        }
    }

    /**
     * 批量更新节点兼容性状态
     *
     * @param nodeIds 节点ID列表
     * @param status  兼容性状态
     */
    public void batchUpdateCompatibilityStatus(List<String> nodeIds, String status) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }
        nodeMapper.batchUpdateCompatibilityStatus(nodeIds, status);
        log.info("批量更新节点兼容性状态: count={}, status={}", nodeIds.size(), status);
    }

    /**
     * 获取指定兼容性状态的节点列表
     *
     * @param workflowId 工作流ID
     * @param status     兼容性状态
     * @return 节点列表
     */
    public List<WorkflowNodeEntity> getNodesByCompatibilityStatus(String workflowId, String status) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        return nodes.stream()
                .filter(n -> status.equals(n.getCompatibilityStatus()))
                .collect(java.util.stream.Collectors.toList());
    }
}
