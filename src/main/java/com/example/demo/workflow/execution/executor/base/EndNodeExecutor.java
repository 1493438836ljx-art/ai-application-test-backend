package com.example.demo.workflow.execution.executor.base;

import com.example.demo.file.service.FileUploadService;
import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;
import com.example.demo.workflow.execution.context.ExecutionGraph;
import com.example.demo.workflow.execution.executor.NodeExecutionResult;
import com.example.demo.workflow.execution.executor.NodeExecutor;
import com.example.demo.workflow.execution.executor.ValidationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结束节点执行器
 * 负责收集最终输出结果，标记工作流完成
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EndNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private FileUploadService fileUploadService;

    // 文件类型匹配模式
    private static final Pattern FILE_TYPE_PATTERN = Pattern.compile("^File<([A-Za-z]+)>$");
    private static final Pattern ARRAY_FILE_TYPE_PATTERN = Pattern.compile("^Array<File<([A-Za-z]+)>>$");

    @Override
    public String getNodeType() {
        return "end";
    }

    @Override
    public NodeExecutionResult execute(WorkflowNodeEntity node,
                                        Map<String, Object> inputs,
                                        ExecutionContext context) {
        log.info("执行结束节点: nodeUuid={}, nodeName={}", node.getNodeUuid(), node.getName());

        Map<String, Object> rawOutputs = new HashMap<>();

        try {
            // 1. 首先尝试从配置的输入参数获取值（由ParameterResolver解析）
            if (inputs != null && !inputs.isEmpty()) {
                rawOutputs.putAll(inputs);
                log.debug("结束节点从配置参数获取输入: {}", inputs.keySet());
            }

            // 2. 如果没有配置输入参数，自动从直接前驱节点收集输出
            if (rawOutputs.isEmpty()) {
                collectOutputsFromPredecessors(node, context, rawOutputs);
            }

            // 3. 构建增强的输出结构（带类型信息）
            Map<String, Object> enrichedOutputs = buildEnrichedOutputs(node, rawOutputs);

            // 4. 如果没有配置输出参数定义，使用简单格式
            if (enrichedOutputs.isEmpty()) {
                enrichedOutputs = rawOutputs;
            }

            log.info("结束节点执行完成: nodeUuid={}, outputs={}", node.getNodeUuid(), enrichedOutputs);

            return NodeExecutionResult.success(enrichedOutputs);

        } catch (Exception e) {
            log.error("结束节点执行异常: nodeUuid={}", node.getNodeUuid(), e);
            // 结束节点即使出错也返回成功，但记录错误信息
            return NodeExecutionResult.success(rawOutputs);
        }
    }

    /**
     * 从直接前驱节点收集输出
     */
    private void collectOutputsFromPredecessors(WorkflowNodeEntity node,
                                                 ExecutionContext context,
                                                 Map<String, Object> outputs) {
        ExecutionGraph graph = context.getExecutionGraph();
        if (graph == null) {
            log.warn("执行图为空，无法获取前驱节点");
            return;
        }

        // 获取直接前驱节点（不是所有前置节点，只是直接连接的节点）
        List<String> directPredecessors = graph.getPredecessors(node.getNodeUuid());
        log.debug("结束节点的直接前驱节点: {}", directPredecessors);

        for (String predUuid : directPredecessors) {
            Map<String, Object> predOutputs = context.getNodeOutputs(predUuid);
            if (predOutputs != null && !predOutputs.isEmpty()) {
                outputs.putAll(predOutputs);
                log.debug("结束节点从前驱节点 {} 收集输出: {}", predUuid, predOutputs.keySet());
            }
        }
    }

    /**
     * 构建增强的输出结构（带类型信息）
     */
    private Map<String, Object> buildEnrichedOutputs(WorkflowNodeEntity node,
                                                      Map<String, Object> rawOutputs) {
        Map<String, Object> enriched = new LinkedHashMap<>();

        String outputParamsJson = node.getOutputParams();
        if (outputParamsJson == null || outputParamsJson.isEmpty()) {
            return enriched;
        }

        try {
            List<Map<String, Object>> paramDefs = objectMapper.readValue(
                    outputParamsJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> paramDef : paramDefs) {
                String name = (String) paramDef.get("name");
                Object value = rawOutputs.get(name);

                if (value != null) {
                    Map<String, Object> param = new LinkedHashMap<>();
                    param.put("name", name);
                    param.put("label", paramDef.getOrDefault("label", name));
                    param.put("type", paramDef.get("type"));
                    param.put("category", paramDef.getOrDefault("category", "BASIC"));
                    param.put("value", value);

                    String type = (String) paramDef.get("type");

                    // 处理单文件类型
                    Matcher fileMatcher = FILE_TYPE_PATTERN.matcher(type != null ? type : "");
                    if (fileMatcher.matches()) {
                        String fileType = fileMatcher.group(1);
                        param.put("fileType", fileType);
                        enrichFileParam(param, value);
                    }

                    // 处理文件数组类型
                    Matcher arrayFileMatcher = ARRAY_FILE_TYPE_PATTERN.matcher(type != null ? type : "");
                    if (arrayFileMatcher.matches()) {
                        String fileType = arrayFileMatcher.group(1);
                        param.put("fileType", fileType);
                        enrichFileArrayParam(param, value);
                    }

                    enriched.put(name, param);
                    log.debug("构建输出参数: {} = {}", name, param);
                }
            }
        } catch (Exception e) {
            log.warn("解析输出参数定义失败: {}", e.getMessage());
        }

        return enriched;
    }

    /**
     * 增强文件类型参数
     */
    private void enrichFileParam(Map<String, Object> param, Object value) {
        if (fileUploadService == null) {
            log.warn("FileUploadService 未注入，无法获取文件信息");
            param.put("downloadUrl", "/api/file/download/" + value);
            return;
        }

        String fileId = value.toString();
        param.put("fileName", fileUploadService.getOriginalFilename(fileId));
        param.put("fileSize", fileUploadService.getFileSize(fileId));
        param.put("downloadUrl", fileUploadService.getFilePath(fileId));
    }

    /**
     * 增强文件数组类型参数
     */
    @SuppressWarnings("unchecked")
    private void enrichFileArrayParam(Map<String, Object> param, Object value) {
        if (fileUploadService == null) {
            log.warn("FileUploadService 未注入，无法获取文件信息");
            return;
        }

        if (value instanceof List) {
            List<Map<String, Object>> files = new ArrayList<>();
            for (Object fileId : (List<?>) value) {
                String id = fileId.toString();
                Map<String, Object> fileInfo = new LinkedHashMap<>();
                fileInfo.put("fileId", id);
                fileInfo.put("fileName", fileUploadService.getOriginalFilename(id));
                fileInfo.put("fileSize", fileUploadService.getFileSize(id));
                fileInfo.put("downloadUrl", fileUploadService.getFilePath(id));
                files.add(fileInfo);
            }
            param.put("files", files);
        }
    }

    @Override
    public ValidationResult validate(WorkflowNodeEntity node) {
        // 结束节点没有特殊验证要求
        return ValidationResult.success();
    }

    @Override
    public boolean supportsParallel() {
        // 结束节点不需要并行执行
        return false;
    }
}
