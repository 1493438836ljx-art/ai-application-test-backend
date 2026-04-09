package com.huawei.cloudopenlabs.workflow.execution.state;

import com.huawei.cloudopenlabs.workflow.entity.ExecutionStatus;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowExecutionEntity;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutionResult;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowExecutionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 状态管理器
 * 负责执行状态的持久化和管理
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StateManager {

    private final WorkflowExecutionMapper executionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 节点执行状态缓存
     * key: executionId_nodeUuid
     * value: NodeExecutionStatus
     */
    private final Map<String, NodeExecutionStatus> nodeStatusCache = new ConcurrentHashMap<>();

    /**
     * 节点输出缓存
     * key: executionId_nodeUuid
     * value: 节点输出Map
     */
    private final Map<String, Map<String, Object>> nodeOutputCache = new ConcurrentHashMap<>();

    /**
     * 更新工作流执行状态
     */
    @Transactional
    public void updateWorkflowStatus(String executionId,
                                      ExecutionStatus status,
                                      Integer progress,
                                      Map<String, Object> outputData) {
        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("执行记录不存在: executionId={}", executionId);
            return;
        }

        execution.setStatus(status.name());

        if (progress != null) {
            execution.setProgress(Math.min(100, Math.max(0, progress)));
        }

        if (outputData != null) {
            try {
                execution.setOutputData(objectMapper.writeValueAsString(outputData));
            } catch (JsonProcessingException e) {
                log.error("序列化输出数据失败", e);
            }
        }

        if (status == ExecutionStatus.RUNNING && execution.getStartTime() == null) {
            execution.setStartTime(LocalDateTime.now());
        }

        if (status.isTerminal()) {
            execution.setEndTime(LocalDateTime.now());
            if (execution.getStartTime() != null) {
                long duration = Duration.between(execution.getStartTime(),
                        execution.getEndTime()).toMillis();
                execution.setDurationMs(duration);
            }
        }

        executionMapper.updateById(execution);
        log.info("工作流状态已更新: executionId={}, status={}, progress={}%",
                executionId, status, execution.getProgress());
    }

    /**
     * 更新节点执行状态
     */
    @Transactional
    public void updateNodeStatus(String executionId,
                                  String nodeUuid,
                                  NodeExecutionStatus status) {
        updateNodeStatus(executionId, nodeUuid, status, null);
    }

    /**
     * 更新节点执行状态（带结果）
     */
    @Transactional
    public void updateNodeStatus(String executionId,
                                  String nodeUuid,
                                  NodeExecutionStatus status,
                                  NodeExecutionResult result) {
        String cacheKey = buildCacheKey(executionId, nodeUuid);

        // 更新缓存
        nodeStatusCache.put(cacheKey, status);

        if (result != null && result.getOutputs() != null) {
            nodeOutputCache.put(cacheKey, new HashMap<>(result.getOutputs()));
        }

        // 更新数据库中的节点执行详情
        updateNodeExecutionsInDb(executionId, nodeUuid, status, result);

        // 更新工作流进度
        updateWorkflowProgress(executionId);

        log.info("节点状态已更新: executionId={}, nodeUuid={}, status={}",
                executionId, nodeUuid, status);
    }

    /**
     * 获取节点执行状态
     */
    public NodeExecutionStatus getNodeStatus(String executionId, String nodeUuid) {
        String cacheKey = buildCacheKey(executionId, nodeUuid);

        // 优先从缓存获取
        NodeExecutionStatus status = nodeStatusCache.get(cacheKey);
        if (status != null) {
            return status;
        }

        // 从数据库获取
        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution != null && execution.getNodeExecutions() != null) {
            try {
                Map<String, Map<String, Object>> nodeExecutions = objectMapper.readValue(
                        execution.getNodeExecutions(),
                        new TypeReference<Map<String, Map<String, Object>>>() {}
                );

                Map<String, Object> nodeData = nodeExecutions.get(nodeUuid);
                if (nodeData != null) {
                    String statusStr = (String) nodeData.get("status");
                    if (statusStr != null) {
                        return NodeExecutionStatus.valueOf(statusStr);
                    }
                }
            } catch (Exception e) {
                log.error("解析节点执行状态失败", e);
            }
        }

        return null;
    }

    /**
     * 获取所有节点执行状态
     */
    public Map<String, NodeExecutionStatus> getAllNodeStatuses(String executionId) {
        Map<String, NodeExecutionStatus> statuses = new HashMap<>();

        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution != null && execution.getNodeExecutions() != null) {
            try {
                Map<String, Map<String, Object>> nodeExecutions = objectMapper.readValue(
                        execution.getNodeExecutions(),
                        new TypeReference<Map<String, Map<String, Object>>>() {}
                );

                for (Map.Entry<String, Map<String, Object>> entry : nodeExecutions.entrySet()) {
                    String statusStr = (String) entry.getValue().get("status");
                    if (statusStr != null) {
                        statuses.put(entry.getKey(), NodeExecutionStatus.valueOf(statusStr));
                    }
                }
            } catch (Exception e) {
                log.error("解析节点执行状态失败", e);
            }
        }

        return statuses;
    }

    /**
     * 统计特定状态的节点数量
     */
    public int countNodesByStatus(String executionId, NodeExecutionStatus targetStatus) {
        Map<String, NodeExecutionStatus> statuses = getAllNodeStatuses(executionId);
        return (int) statuses.values().stream()
                .filter(s -> s == targetStatus)
                .count();
    }

    /**
     * 获取节点输出
     */
    public Map<String, Object> getNodeOutputs(String executionId, String nodeUuid) {
        String cacheKey = buildCacheKey(executionId, nodeUuid);

        // 优先从缓存获取
        Map<String, Object> outputs = nodeOutputCache.get(cacheKey);
        if (outputs != null) {
            return new HashMap<>(outputs);
        }

        // 从数据库获取
        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution != null && execution.getNodeExecutions() != null) {
            try {
                Map<String, Map<String, Object>> nodeExecutions = objectMapper.readValue(
                        execution.getNodeExecutions(),
                        new TypeReference<Map<String, Map<String, Object>>>() {}
                );

                Map<String, Object> nodeData = nodeExecutions.get(nodeUuid);
                if (nodeData != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> outputData = (Map<String, Object>) nodeData.get("outputs");
                    if (outputData != null) {
                        return outputData;
                    }
                }
            } catch (Exception e) {
                log.error("解析节点输出失败", e);
            }
        }

        return new HashMap<>();
    }

    /**
     * 清除执行相关的缓存
     */
    public void clearCache(String executionId) {
        String prefix = executionId + "_";

        nodeStatusCache.keySet().removeIf(key -> key.startsWith(prefix));
        nodeOutputCache.keySet().removeIf(key -> key.startsWith(prefix));

        log.debug("清除执行缓存: executionId={}", executionId);
    }

    /**
     * 更新工作流最终输出
     */
    @Transactional
    public void updateWorkflowOutput(String executionId, Map<String, Object> outputs) {
        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return;
        }

        try {
            execution.setOutputData(objectMapper.writeValueAsString(outputs));
            executionMapper.updateById(execution);
        } catch (JsonProcessingException e) {
            log.error("序列化输出数据失败", e);
        }
    }

    // ==================== 私有方法 ====================

    private String buildCacheKey(String executionId, String nodeUuid) {
        return executionId + "_" + nodeUuid;
    }

    /**
     * 更新数据库中的节点执行详情
     */
    @SuppressWarnings("unchecked")
    private void updateNodeExecutionsInDb(String executionId,
                                           String nodeUuid,
                                           NodeExecutionStatus status,
                                           NodeExecutionResult result) {
        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return;
        }

        try {
            // 解析现有的节点执行详情
            Map<String, Map<String, Object>> nodeExecutions;
            if (execution.getNodeExecutions() != null && !execution.getNodeExecutions().isEmpty()) {
                nodeExecutions = objectMapper.readValue(
                        execution.getNodeExecutions(),
                        new TypeReference<Map<String, Map<String, Object>>>() {}
                );
            } else {
                nodeExecutions = new HashMap<>();
            }

            // 更新或创建节点执行信息
            Map<String, Object> nodeData = nodeExecutions.computeIfAbsent(
                    nodeUuid, k -> new HashMap<>()
            );

            nodeData.put("status", status.name());
            nodeData.put("updatedAt", LocalDateTime.now().toString());

            if (status == NodeExecutionStatus.RUNNING) {
                nodeData.put("startTime", LocalDateTime.now().toString());
            }

            if (status.isTerminal()) {
                nodeData.put("endTime", LocalDateTime.now().toString());
                if (nodeData.get("startTime") != null) {
                    LocalDateTime startTime = LocalDateTime.parse((String) nodeData.get("startTime"));
                    long duration = Duration.between(startTime, LocalDateTime.now()).toMillis();
                    nodeData.put("durationMs", duration);
                }
            }

            if (result != null) {
                if (result.getOutputs() != null) {
                    nodeData.put("outputs", result.getOutputs());
                }
                if (result.getErrorMessage() != null) {
                    nodeData.put("errorMessage", result.getErrorMessage());
                }
                if (result.getDurationMs() != null) {
                    nodeData.put("durationMs", result.getDurationMs());
                }
            }

            // 保存回数据库
            execution.setNodeExecutions(objectMapper.writeValueAsString(nodeExecutions));
            executionMapper.updateById(execution);

        } catch (Exception e) {
            log.error("更新节点执行详情失败: executionId={}, nodeUuid={}", executionId, nodeUuid, e);
        }
    }

    /**
     * 更新工作流执行进度
     */
    private void updateWorkflowProgress(String executionId) {
        WorkflowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return;
        }

        try {
            // 统计所有节点执行状态
            Map<String, NodeExecutionStatus> statuses = getAllNodeStatuses(executionId);

            int total = statuses.size();
            if (total == 0) {
                return;
            }

            long completed = statuses.values().stream()
                    .filter(NodeExecutionStatus::isTerminal)
                    .count();

            int progress = (int) ((completed * 100) / total);
            execution.setProgress(progress);

            executionMapper.updateById(execution);

        } catch (Exception e) {
            log.error("更新工作流进度失败: executionId={}", executionId, e);
        }
    }
}
