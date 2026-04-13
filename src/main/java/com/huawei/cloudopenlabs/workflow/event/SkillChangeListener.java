/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.event;

import com.huawei.cloudopenlabs.workflow.dto.ValidationResult;
import com.huawei.cloudopenlabs.workflow.entity.CompatibilityStatus;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowNodeMapper;
import com.huawei.cloudopenlabs.workflow.service.validation.SkillCompatibilityChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * Skill 变更事件监听器
 * 当 Skill 发生变更时，自动检查相关节点的兼容性并更新状态
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillChangeListener {

    private final WorkflowNodeMapper nodeMapper;
    private final SkillCompatibilityChecker skillChecker;

    /**
     * 监听 Skill 变更事件
     * 使用异步处理避免阻塞主流程
     *
     * @param event Skill 变更事件
     */
    @EventListener
    @Async("workflowTaskExecutor")
    public void onSkillChange(SkillChangeEvent event) {
        String skillId = event.getSkillId();
        SkillChangeEvent.ChangeType changeType = event.getChangeType();

        log.info("Received skill change event: skillId={}, changeType={}", skillId, changeType);

        try {
            // 根据变更类型处理
            switch (changeType) {
                case CREATED:
                    // 新增 Skill，无需处理
                    log.debug("Skill created, no compatibility check needed: skillId={}", skillId);
                    break;

                case UPDATED:
                case VERSION_UPGRADED:
                    // 更新或版本升级，检查兼容性
                    handleSkillUpdate(skillId, event);
                    break;

                case ENABLED:
                    // 启用，重新检查兼容性
                    handleSkillEnable(skillId);
                    break;

                case DISABLED:
                    // 禁用，标记所有相关节点为不兼容
                    handleSkillDisable(skillId);
                    break;

                case DELETED:
                    // 删除，标记所有相关节点为无效
                    handleSkillDelete(skillId);
                    break;

                default:
                    log.warn("Unknown change type: {}", changeType);
            }
        } catch (Exception e) {
            log.error("Failed to process skill change event: skillId={}, changeType={}", skillId, changeType, e);
        }
    }

    /**
     * 处理 Skill 更新
     *
     * @param skillId Skill ID
     * @param event   Skill 变更事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSkillUpdate(String skillId, SkillChangeEvent event) {
        // 1. 查找所有使用该 Skill 的节点
        List<WorkflowNodeEntity> nodes = nodeMapper.selectBySkillId(skillId);

        if (nodes.isEmpty()) {
            log.debug("No nodes using this skill: skillId={}", skillId);
            return;
        }

        log.info("Starting compatibility check for {} nodes: skillId={}", nodes.size(), skillId);

        // 2. 逐个检查节点兼容性
        int compatibleCount = 0;
        int needsUpdateCount = 0;
        int incompatibleCount = 0;

        for (WorkflowNodeEntity node : nodes) {
            try {
                // 执行兼容性检查
                ValidationResult nodeResult = new ValidationResult();
                CompatibilityStatus status = skillChecker.checkNode(node, nodeResult);

                // 更新节点状态
                node.setCompatibilityStatus(status.name());
                nodeMapper.updateById(node);

                // 统计
                switch (status) {
                    case COMPATIBLE:
                        compatibleCount++;
                        break;
                    case NEEDS_UPDATE:
                        needsUpdateCount++;
                        break;
                    case INCOMPATIBLE:
                    case INVALID:
                        incompatibleCount++;
                        break;
                    default:
                        break;
                }

                log.debug("Node compatibility check result: nodeId={}, nodeName={}, status={}",
                        node.getId(), node.getName(), status);

            } catch (Exception e) {
                log.error("Failed to check node compatibility: nodeId={}, nodeName={}", node.getId(), node.getName(), e);
                node.setCompatibilityStatus(CompatibilityStatus.INVALID.name());
                nodeMapper.updateById(node);
                incompatibleCount++;
            }
        }

        log.info("Compatibility check completed: skillId={}, compatible={}, needsUpdate={}, incompatible={}",
                skillId, compatibleCount, needsUpdateCount, incompatibleCount);

        // 3. 如果有不兼容的节点，发送通知
        if (incompatibleCount > 0 || needsUpdateCount > 0) {
            sendCompatibilityNotification(skillId, nodes, needsUpdateCount, incompatibleCount);
        }
    }

    /**
     * 处理 Skill 禁用
     *
     * @param skillId Skill ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSkillDisable(String skillId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectBySkillId(skillId);

        if (nodes.isEmpty()) {
            log.debug("No nodes using this skill: skillId={}", skillId);
            return;
        }

        for (WorkflowNodeEntity node : nodes) {
            node.setCompatibilityStatus(CompatibilityStatus.INCOMPATIBLE.name());
            nodeMapper.updateById(node);
        }

        log.info("Skill disabled, marked {} nodes as incompatible: skillId={}", nodes.size(), skillId);

        // 发送通知
        sendDisableNotification(skillId, nodes);
    }

    /**
     * 处理 Skill 启用
     *
     * @param skillId Skill ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSkillEnable(String skillId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectBySkillId(skillId);

        if (nodes.isEmpty()) {
            log.debug("No nodes using this skill: skillId={}", skillId);
            return;
        }

        int compatibleCount = 0;
        int needsUpdateCount = 0;

        for (WorkflowNodeEntity node : nodes) {
            ValidationResult nodeResult = new ValidationResult();
            CompatibilityStatus status = skillChecker.checkNode(node, nodeResult);
            node.setCompatibilityStatus(status.name());
            nodeMapper.updateById(node);

            if (status == CompatibilityStatus.COMPATIBLE) {
                compatibleCount++;
            } else if (status == CompatibilityStatus.NEEDS_UPDATE) {
                needsUpdateCount++;
            }
        }

        log.info("Skill enabled, compatibility check completed: skillId={}, compatible={}, needsUpdate={}",
                skillId, compatibleCount, needsUpdateCount);
    }

    /**
     * 处理 Skill 删除
     *
     * @param skillId Skill ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSkillDelete(String skillId) {
        List<WorkflowNodeEntity> nodes = nodeMapper.selectBySkillId(skillId);

        if (nodes.isEmpty()) {
            log.debug("No nodes using this skill: skillId={}", skillId);
            return;
        }

        for (WorkflowNodeEntity node : nodes) {
            node.setCompatibilityStatus(CompatibilityStatus.INVALID.name());
            nodeMapper.updateById(node);
        }

        log.info("Skill deleted, marked {} nodes as invalid: skillId={}", nodes.size(), skillId);

        // 发送通知
        sendDeleteNotification(skillId, nodes);
    }

    /**
     * 发送兼容性变更通知
     */
    private void sendCompatibilityNotification(String skillId,
                                                List<WorkflowNodeEntity> nodes,
                                                int needsUpdateCount,
                                                int incompatibleCount) {
        // TODO: 实现通知逻辑
        // 可以通过 WebSocket、邮件、站内信等方式通知相关用户
        log.info("Sending compatibility change notification: skillId={}, needsUpdate={}, incompatible={}",
                skillId, needsUpdateCount, incompatibleCount);
    }

    /**
     * 发送禁用通知
     */
    private void sendDisableNotification(String skillId, List<WorkflowNodeEntity> nodes) {
        // TODO: 实现通知逻辑
        log.info("Sending skill disabled notification: skillId={}, affectedNodeCount={}", skillId, nodes.size());
    }

    /**
     * 发送删除通知
     */
    private void sendDeleteNotification(String skillId, List<WorkflowNodeEntity> nodes) {
        // TODO: 实现通知逻辑
        log.info("Sending skill deleted notification: skillId={}, affectedNodeCount={}", skillId, nodes.size());
    }
}
