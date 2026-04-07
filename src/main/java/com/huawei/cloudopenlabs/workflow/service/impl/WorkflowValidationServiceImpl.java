package com.huawei.cloudopenlabs.workflow.service.impl;

import com.huawei.cloudopenlabs.workflow.controller.WorkflowValidationController.ReferenceCheckResult;
import com.huawei.cloudopenlabs.workflow.dto.AvailableVariable;
import com.huawei.cloudopenlabs.workflow.dto.NodeResponse;
import com.huawei.cloudopenlabs.workflow.dto.ValidationResult;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowConnectionEntity;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowConnectionMapper;
import com.huawei.cloudopenlabs.workflow.mapper.WorkflowNodeMapper;
import com.huawei.cloudopenlabs.workflow.service.WorkflowValidationService;
import com.huawei.cloudopenlabs.workflow.service.validation.CyclicDependencyValidator;
import com.huawei.cloudopenlabs.workflow.service.validation.SkillCompatibilityChecker;
import com.huawei.cloudopenlabs.workflow.service.validation.StructureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 工作流验证服务实现
 * 协调各个验证器组件进行完整验证
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowValidationServiceImpl implements WorkflowValidationService {

    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowConnectionMapper connectionMapper;
    private final ObjectMapper objectMapper;

    // 验证器组件
    private final StructureValidator structureValidator;
    private final CyclicDependencyValidator cyclicDependencyValidator;
    private final SkillCompatibilityChecker skillCompatibilityChecker;

    // 参数引用正则表达式：${节点名称.参数名}
    private static final Pattern PARAM_REFERENCE_PATTERN = Pattern.compile("\\$\\{([^}.]+)\\.([^}]+)}");

    @Override
    public ValidationResult validate(Long workflowId) {
        log.info("开始验证工作流: workflowId={}", workflowId);

        ValidationResult result = new ValidationResult();
        result.setValid(true);

        // 1. 结构验证
        log.debug("执行结构验证...");
        ValidationResult structureResult = structureValidator.validate(workflowId);
        result.merge(structureResult);

        // 2. 循环依赖检测
        log.debug("执行循环依赖检测...");
        ValidationResult cycleResult = cyclicDependencyValidator.validate(workflowId);
        result.merge(cycleResult);

        // 3. 参数引用验证
        log.debug("执行参数引用验证...");
        ValidationResult paramRefResult = validateParameterReferences(workflowId);
        result.merge(paramRefResult);

        // 4. Skill 兼容性检查
        log.debug("执行 Skill 兼容性检查...");
        ValidationResult skillResult = skillCompatibilityChecker.check(workflowId);
        result.merge(skillResult);

        log.info("工作流验证完成: workflowId={}, valid={}, errors={}, warnings={}",
                workflowId, result.isValid(), result.getErrors().size(), result.getWarnings().size());

        return result;
    }

    /**
     * 快速验证（仅结构和循环）
     */
    public ValidationResult quickValidate(Long workflowId) {
        log.info("快速验证工作流: workflowId={}", workflowId);

        ValidationResult result = new ValidationResult();
        result.setValid(true);

        result.merge(structureValidator.validate(workflowId));
        result.merge(cyclicDependencyValidator.validate(workflowId));

        return result;
    }

    /**
     * 参数引用验证
     */
    private ValidationResult validateParameterReferences(Long workflowId) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> connections = connectionMapper.selectByWorkflowId(workflowId);

        // 构建节点名称映射
        Map<String, WorkflowNodeEntity> nameToNode = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeEntity::getName, n -> n, (a, b) -> a));

        // 构建前置节点映射
        Map<Long, Set<Long>> predecessorsMap = buildPredecessorsMap(nodes, connections);

        // 遍历每个节点的输入参数
        for (WorkflowNodeEntity node : nodes) {
            String inputParams = node.getInputParams();
            if (inputParams == null || inputParams.isEmpty()) {
                continue;
            }

            Set<Long> predecessors = predecessorsMap.getOrDefault(node.getId(), Collections.emptySet());
            Set<String> predecessorNames = predecessors.stream()
                    .map(id -> nodes.stream()
                            .filter(n -> n.getId().equals(id))
                            .map(WorkflowNodeEntity::getName)
                            .findFirst()
                            .orElse(""))
                    .collect(Collectors.toSet());

            // 提取参数引用
            Set<String> references = extractParamReferences(inputParams);

            for (String ref : references) {
                String[] parts = ref.split("\\.");
                if (parts.length < 2) {
                    result.addError("WF_PARAM_001", "参数引用语法错误: " + ref,
                            node.getNodeUuid(), "inputParams");
                    continue;
                }

                String refNodeName = parts[0];
                String refParamName = parts.length == 2 ? parts[1] : parts[2];

                // 检查节点是否存在
                WorkflowNodeEntity refNode = nameToNode.get(refNodeName);
                if (refNode == null) {
                    result.addError("WF_PARAM_002", "引用的节点不存在: " + refNodeName,
                            node.getNodeUuid(), "inputParams");
                    continue;
                }

                // 检查是否为前置节点
                if (!predecessorNames.contains(refNodeName)) {
                    result.addError("WF_PARAM_003", "引用的节点 '" + refNodeName + "' 不是前置节点",
                            node.getNodeUuid(), "inputParams");
                    continue;
                }

                // 检查参数是否存在
                if (!hasOutputParam(refNode, refParamName)) {
                    result.addWarning("WF_PARAM_004", "引用的参数可能不存在: " + ref,
                            node.getNodeUuid());
                }
            }
        }

        return result;
    }

    @Override
    public List<NodeResponse> getPredecessors(Long workflowId, String nodeUuid) {
        log.info("获取前置节点: workflowId={}, nodeUuid={}", workflowId, nodeUuid);

        List<WorkflowNodeEntity> allNodes = nodeMapper.selectByWorkflowId(workflowId);
        List<WorkflowConnectionEntity> allConnections = connectionMapper.selectByWorkflowId(workflowId);

        // 找到目标节点
        WorkflowNodeEntity targetNode = allNodes.stream()
                .filter(n -> nodeUuid.equals(n.getNodeUuid()))
                .findFirst()
                .orElse(null);

        if (targetNode == null) {
            return List.of();
        }

        // 构建邻接表（反向）
        Map<Long, List<Long>> reverseAdjList = new HashMap<>();
        Map<Long, WorkflowNodeEntity> idToNodeMap = new HashMap<>();

        for (WorkflowNodeEntity node : allNodes) {
            idToNodeMap.put(node.getId(), node);
            reverseAdjList.put(node.getId(), new ArrayList<>());
        }

        for (WorkflowConnectionEntity conn : allConnections) {
            reverseAdjList.computeIfAbsent(conn.getTargetNodeId(), k -> new ArrayList<>())
                    .add(conn.getSourceNodeId());
        }

        // BFS查找所有前置节点
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(targetNode.getId());
        visited.add(targetNode.getId());

        List<NodeResponse> predecessors = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            for (Long sourceId : reverseAdjList.getOrDefault(currentId, List.of())) {
                if (!visited.contains(sourceId)) {
                    visited.add(sourceId);
                    queue.add(sourceId);
                    WorkflowNodeEntity node = idToNodeMap.get(sourceId);
                    if (node != null) {
                        predecessors.add(convertToResponse(node));
                    }
                }
            }
        }

        return predecessors;
    }

    @Override
    public List<AvailableVariable> getAvailableVariables(Long workflowId, String nodeUuid) {
        log.info("获取可用变量: workflowId={}, nodeUuid={}", workflowId, nodeUuid);

        List<NodeResponse> predecessors = getPredecessors(workflowId, nodeUuid);
        List<AvailableVariable> variables = new ArrayList<>();

        for (NodeResponse predecessor : predecessors) {
            if (predecessor.getOutputParams() != null && !predecessor.getOutputParams().isEmpty()) {
                try {
                    JsonNode outputParams = objectMapper.readTree(predecessor.getOutputParams());
                    if (outputParams.isArray()) {
                        for (JsonNode param : outputParams) {
                            AvailableVariable variable = new AvailableVariable();
                            variable.setNodeUuid(predecessor.getNodeUuid());
                            variable.setNodeName(predecessor.getNodeName());
                            variable.setParamName(param.has("name") ? param.get("name").asText() : "unknown");
                            variable.setParamType(param.has("type") ? param.get("type").asText() : "any");
                            variable.setDescription(param.has("description") ? param.get("description").asText() : "");
                            variables.add(variable);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析输出参数失败: nodeUuid={}", predecessor.getNodeUuid(), e);
                }
            }
        }

        return variables;
    }

    @Override
    public ReferenceCheckResult checkReference(Long workflowId, String nodeUuid, String reference) {
        log.info("检查参数引用: workflowId={}, nodeUuid={}, reference={}", workflowId, nodeUuid, reference);

        ReferenceCheckResult result = new ReferenceCheckResult();

        Matcher matcher = PARAM_REFERENCE_PATTERN.matcher(reference);
        if (!matcher.matches()) {
            result.setValid(false);
            result.setMessage("参数引用格式无效，正确格式为：${节点名称.参数名}");
            return result;
        }

        String nodeName = matcher.group(1);
        String paramName = matcher.group(2);

        // 获取可用变量
        List<AvailableVariable> availableVariables = getAvailableVariables(workflowId, nodeUuid);

        // 查找匹配的变量
        for (AvailableVariable variable : availableVariables) {
            if (variable.getNodeName().equals(nodeName) && variable.getParamName().equals(paramName)) {
                result.setValid(true);
                result.setMessage("参数引用有效");
                result.setSourceNodeUuid(variable.getNodeUuid());
                result.setSourceNodeName(variable.getNodeName());
                result.setParamName(variable.getParamName());
                result.setParamType(variable.getParamType());
                return result;
            }
        }

        result.setValid(false);
        result.setMessage("未找到有效的参数引用：节点 '" + nodeName + "' 的参数 '" + paramName + "' 不存在或不可访问");
        return result;
    }

    @Override
    public List<NodeResponse> getExecutionOrder(Long workflowId) {
        log.info("获取执行顺序: workflowId={}", workflowId);

        try {
            List<Long> orderIds = cyclicDependencyValidator.getTopologicalOrderByIds(workflowId);
            List<WorkflowNodeEntity> nodes = nodeMapper.selectByWorkflowId(workflowId);

            Map<Long, WorkflowNodeEntity> idToNodeMap = nodes.stream()
                    .collect(Collectors.toMap(WorkflowNodeEntity::getId, n -> n));

            return orderIds.stream()
                    .map(idToNodeMap::get)
                    .filter(Objects::nonNull)
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalStateException e) {
            log.warn("获取执行顺序失败: {}", e.getMessage());
            throw e;
        }
    }

    // ========== 私有方法 ==========

    /**
     * 构建前置节点映射（BFS）
     */
    private Map<Long, Set<Long>> buildPredecessorsMap(
            List<WorkflowNodeEntity> nodes,
            List<WorkflowConnectionEntity> connections) {

        Map<Long, Set<Long>> predecessorsMap = new HashMap<>();

        // 构建邻接表（反向）
        Map<Long, List<Long>> reverseAdj = new HashMap<>();
        for (WorkflowConnectionEntity conn : connections) {
            reverseAdj.computeIfAbsent(conn.getTargetNodeId(), k -> new ArrayList<>())
                    .add(conn.getSourceNodeId());
        }

        // BFS 计算每个节点的所有前置节点
        for (WorkflowNodeEntity node : nodes) {
            Set<Long> predecessors = new HashSet<>();
            Queue<Long> queue = new LinkedList<>();
            queue.add(node.getId());

            while (!queue.isEmpty()) {
                Long current = queue.poll();
                List<Long> sources = reverseAdj.get(current);
                if (sources != null) {
                    for (Long source : sources) {
                        if (predecessors.add(source)) {
                            queue.add(source);
                        }
                    }
                }
            }
            predecessorsMap.put(node.getId(), predecessors);
        }

        return predecessorsMap;
    }

    /**
     * 提取参数引用
     */
    private Set<String> extractParamReferences(String inputParams) {
        Set<String> references = new HashSet<>();
        Matcher matcher = PARAM_REFERENCE_PATTERN.matcher(inputParams);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return references;
    }

    /**
     * 检查节点是否有指定的输出参数
     */
    private boolean hasOutputParam(WorkflowNodeEntity node, String paramName) {
        String outputParams = node.getOutputParams();
        if (outputParams == null || outputParams.isEmpty()) {
            return false;
        }

        try {
            JsonNode paramsNode = objectMapper.readTree(outputParams);
            if (paramsNode.isArray()) {
                for (JsonNode param : paramsNode) {
                    if (param.has("name") && paramName.equals(param.get("name").asText())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析输出参数失败: nodeUuid={}", node.getNodeUuid(), e);
        }

        return false;
    }

    /**
     * 转换为响应DTO
     */
    private NodeResponse convertToResponse(WorkflowNodeEntity entity) {
        if (entity == null) {
            return null;
        }
        NodeResponse response = new NodeResponse();
        response.setId(entity.getId());
        response.setNodeUuid(entity.getNodeUuid());
        response.setNodeName(entity.getName());
        response.setNodeType(entity.getType());
        response.setNodeCategory(entity.getNodeCategory());
        response.setPositionX(entity.getPositionX());
        response.setPositionY(entity.getPositionY());
        response.setSkillId(entity.getSkillId());
        response.setSkillSnapshot(entity.getSkillSnapshot());
        response.setInputPorts(entity.getInputPorts());
        response.setOutputPorts(entity.getOutputPorts());
        response.setInputParams(entity.getInputParams());
        response.setOutputParams(entity.getOutputParams());
        response.setExecutionLocation(entity.getExecutionLocation());
        response.setErrorStrategy(entity.getErrorStrategy());
        response.setRetryCount(entity.getRetryCount());
        response.setRetryInterval(entity.getRetryInterval());
        response.setErrorBranchId(entity.getErrorBranchId());
        response.setConditionType(entity.getConditionType());
        response.setConditions(entity.getConditions());
        response.setLoopType(entity.getLoopType());
        response.setLoopConfig(entity.getLoopConfig());
        response.setBatchConfig(entity.getBatchConfig());
        response.setAsyncConfig(entity.getAsyncConfig());
        response.setCollectConfig(entity.getCollectConfig());
        response.setCompatibilityStatus(entity.getCompatibilityStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
