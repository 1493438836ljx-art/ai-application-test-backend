package com.example.demo.workflow.execution.error;

import com.example.demo.workflow.entity.WorkflowNodeEntity;
import com.example.demo.workflow.execution.context.ExecutionContext;
import com.example.demo.workflow.execution.executor.NodeExecutionResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点错误处理器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NodeErrorHandler {

    private final ObjectMapper objectMapper;

    /**
     * 错误策略枚举
     */
    public enum ErrorStrategy {
        STOP,           // 终止工作流
        SKIP,           // 跳过当前节点
        RETRY,          // 重试执行
        ERROR_BRANCH    // 执行错误分支
    }

    /**
     * 处理节点执行错误
     *
     * @param node      节点定义
     * @param context   执行上下文
     * @param exception 异常
     * @return 错误处理结果
     */
    public ErrorHandleResult handleNodeError(WorkflowNodeEntity node,
                                             ExecutionContext context,
                                             Exception exception) {
        String nodeUuid = node.getNodeUuid();
        String nodeName = node.getName();

        log.error("处理节点执行错误: nodeUuid={}, nodeName={}, error={}",
                nodeUuid, nodeName, exception.getMessage());

        // 获取节点的错误策略配置
        ErrorStrategyConfig errorConfig = parseErrorConfig(node.getErrorStrategy(), node.getConfig());

        // 根据错误类型和策略处理
        ErrorType errorType = determineErrorType(exception);

        // 1. 可恢复错误 + 重试策略
        if (errorType == ErrorType.RECOVERABLE &&
            errorConfig.getStrategy() == ErrorStrategy.RETRY) {
            return handleRetry(node, context, exception, errorConfig);
        }

        // 2. 根据策略处理
        switch (errorConfig.getStrategy()) {
            case STOP:
                return handleStop(node, context, exception);

            case SKIP:
                return handleSkip(node, context, exception, errorConfig);

            case RETRY:
                return handleRetry(node, context, exception, errorConfig);

            case ERROR_BRANCH:
                return handleErrorBranch(node, context, exception, errorConfig);

            default:
                return handleStop(node, context, exception);
        }
    }

    /**
     * 确定错误类型
     */
    private ErrorType determineErrorType(Exception exception) {
        if (exception instanceof WorkflowExecutionException) {
            return ((WorkflowExecutionException) exception).getErrorType();
        }
        // 默认为业务错误
        return ErrorType.BUSINESS;
    }

    /**
     * 处理终止策略
     */
    private ErrorHandleResult handleStop(WorkflowNodeEntity node,
                                          ExecutionContext context,
                                          Exception exception) {
        log.error("节点执行失败，终止工作流: nodeUuid={}, error={}",
                node.getNodeUuid(), exception.getMessage());

        return ErrorHandleResult.stop(exception.getMessage());
    }

    /**
     * 处理跳过策略
     */
    private ErrorHandleResult handleSkip(WorkflowNodeEntity node,
                                          ExecutionContext context,
                                          Exception exception,
                                          ErrorStrategyConfig errorConfig) {
        log.warn("节点执行失败，跳过继续: nodeUuid={}, error={}",
                node.getNodeUuid(), exception.getMessage());

        String skipReason = errorConfig.getSkipReason() != null ?
                errorConfig.getSkipReason() : exception.getMessage();

        return ErrorHandleResult.skip(skipReason);
    }

    /**
     * 处理重试策略
     */
    private ErrorHandleResult handleRetry(WorkflowNodeEntity node,
                                           ExecutionContext context,
                                           Exception exception,
                                           ErrorStrategyConfig errorConfig) {

        String nodeUuid = node.getNodeUuid();
        int currentRetryCount = getRetryCount(context, nodeUuid);

        if (currentRetryCount >= errorConfig.getMaxRetries()) {
            log.error("节点重试次数已达上限: nodeUuid={}, retries={}/{}",
                    nodeUuid, currentRetryCount, errorConfig.getMaxRetries());

            // 重试失败，使用默认策略处理
            return handleStop(node, context,
                    new RuntimeException("重试次数已达上限: " + currentRetryCount));
        }

        // 计算重试间隔
        long retryInterval = calculateRetryInterval(errorConfig, currentRetryCount);

        log.info("节点执行失败，准备重试: nodeUuid={}, retry={}/{}, interval={}ms",
                nodeUuid, currentRetryCount + 1, errorConfig.getMaxRetries(), retryInterval);

        // 更新重试计数
        incrementRetryCount(context, nodeUuid);

        return ErrorHandleResult.retry(retryInterval);
    }

    /**
     * 处理错误分支策略
     */
    private ErrorHandleResult handleErrorBranch(WorkflowNodeEntity node,
                                                 ExecutionContext context,
                                                 Exception exception,
                                                 ErrorStrategyConfig errorConfig) {

        String errorBranchNodeUuid = errorConfig.getErrorBranchNodeUuid();

        if (errorBranchNodeUuid == null) {
            log.warn("未配置错误分支节点，使用终止策略: nodeUuid={}", node.getNodeUuid());
            return handleStop(node, context, exception);
        }

        log.info("节点执行失败，跳转到错误分支: nodeUuid={}, errorBranch={}",
                node.getNodeUuid(), errorBranchNodeUuid);

        // 设置错误信息到上下文
        context.setGlobalVariable("_error_node_uuid", node.getNodeUuid());
        context.setGlobalVariable("_error_message", exception.getMessage());
        context.setGlobalVariable("_error_timestamp", LocalDateTime.now().toString());

        return ErrorHandleResult.errorBranch(errorBranchNodeUuid);
    }

    /**
     * 解析错误配置
     */
    private ErrorStrategyConfig parseErrorConfig(String errorStrategy, String configJson) {
        ErrorStrategyConfig config = new ErrorStrategyConfig();

        // 解析错误策略
        if (errorStrategy != null && !errorStrategy.isEmpty()) {
            try {
                config.setStrategy(ErrorStrategy.valueOf(errorStrategy.toUpperCase()));
            } catch (Exception e) {
                config.setStrategy(ErrorStrategy.STOP);
            }
        }

        // 从配置中解析详细参数
        if (configJson != null && !configJson.isEmpty()) {
            try {
                Map<String, Object> configMap = objectMapper.readValue(
                        configJson,
                        new TypeReference<Map<String, Object>>() {}
                );

                // 解析错误配置
                Object errorConfigObj = configMap.get("errorConfig");
                if (errorConfigObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> errorConfigMap = (Map<String, Object>) errorConfigObj;

                    if (errorConfigMap.containsKey("maxRetries")) {
                        config.setMaxRetries(((Number) errorConfigMap.get("maxRetries")).intValue());
                    }
                    if (errorConfigMap.containsKey("retryInterval")) {
                        config.setRetryInterval(((Number) errorConfigMap.get("retryInterval")).longValue());
                    }
                    if (errorConfigMap.containsKey("exponentialBackoff")) {
                        config.setExponentialBackoff((Boolean) errorConfigMap.get("exponentialBackoff"));
                    }
                    if (errorConfigMap.containsKey("backoffMultiplier")) {
                        config.setBackoffMultiplier(((Number) errorConfigMap.get("backoffMultiplier")).doubleValue());
                    }
                    if (errorConfigMap.containsKey("maxRetryInterval")) {
                        config.setMaxRetryInterval(((Number) errorConfigMap.get("maxRetryInterval")).longValue());
                    }
                    if (errorConfigMap.containsKey("errorBranchNodeUuid")) {
                        config.setErrorBranchNodeUuid((String) errorConfigMap.get("errorBranchNodeUuid"));
                    }
                    if (errorConfigMap.containsKey("skipReason")) {
                        config.setSkipReason((String) errorConfigMap.get("skipReason"));
                    }
                }

            } catch (Exception e) {
                log.warn("解析错误配置失败", e);
            }
        }

        return config;
    }

    /**
     * 计算重试间隔（支持指数退避）
     */
    private long calculateRetryInterval(ErrorStrategyConfig config, int retryCount) {
        if (!config.isExponentialBackoff()) {
            return config.getRetryInterval();
        }

        long interval = (long) (config.getRetryInterval() *
                Math.pow(config.getBackoffMultiplier(), retryCount));

        return Math.min(interval, config.getMaxRetryInterval());
    }

    /**
     * 获取重试次数
     */
    private int getRetryCount(ExecutionContext context, String nodeUuid) {
        Object count = context.getGlobalVariable("_retry_count_" + nodeUuid);
        return count != null ? ((Number) count).intValue() : 0;
    }

    /**
     * 增加重试次数
     */
    private void incrementRetryCount(ExecutionContext context, String nodeUuid) {
        int currentCount = getRetryCount(context, nodeUuid);
        context.setGlobalVariable("_retry_count_" + nodeUuid, currentCount + 1);
    }

    /**
     * 错误策略配置
     */
    @lombok.Data
    private static class ErrorStrategyConfig {
        private ErrorStrategy strategy = ErrorStrategy.STOP;
        private int maxRetries = 3;
        private long retryInterval = 1000;
        private boolean exponentialBackoff = true;
        private double backoffMultiplier = 2.0;
        private long maxRetryInterval = 60000;
        private String errorBranchNodeUuid;
        private String skipReason;
    }

    /**
     * 错误处理结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ErrorHandleResult {
        private ErrorHandleAction action;
        private String errorMessage;
        private Long retryIntervalMs;
        private String errorBranchNodeUuid;
        private String skipReason;

        public static ErrorHandleResult stop(String errorMessage) {
            return ErrorHandleResult.builder()
                    .action(ErrorHandleAction.STOP)
                    .errorMessage(errorMessage)
                    .build();
        }

        public static ErrorHandleResult skip(String reason) {
            return ErrorHandleResult.builder()
                    .action(ErrorHandleAction.SKIP)
                    .skipReason(reason)
                    .build();
        }

        public static ErrorHandleResult retry(long intervalMs) {
            return ErrorHandleResult.builder()
                    .action(ErrorHandleAction.RETRY)
                    .retryIntervalMs(intervalMs)
                    .build();
        }

        public static ErrorHandleResult errorBranch(String nodeUuid) {
            return ErrorHandleResult.builder()
                    .action(ErrorHandleAction.ERROR_BRANCH)
                    .errorBranchNodeUuid(nodeUuid)
                    .build();
        }
    }

    /**
     * 错误处理动作
     */
    public enum ErrorHandleAction {
        STOP,           // 终止工作流
        SKIP,           // 跳过继续
        RETRY,          // 重试执行
        ERROR_BRANCH    // 执行错误分支
    }
}
