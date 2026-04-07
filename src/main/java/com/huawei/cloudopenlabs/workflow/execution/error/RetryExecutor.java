package com.huawei.cloudopenlabs.workflow.execution.error;

import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import com.huawei.cloudopenlabs.workflow.execution.context.ExecutionContext;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutionResult;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutor;
import com.huawei.cloudopenlabs.workflow.execution.executor.NodeExecutorRegistry;
import com.huawei.cloudopenlabs.workflow.execution.engine.ParameterResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * 重试执行器
 * 封装节点执行的重试逻辑
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RetryExecutor {

    private final NodeExecutorRegistry executorRegistry;
    private final ParameterResolver parameterResolver;

    @Autowired(required = false)
    private NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * 带重试的执行节点
     *
     * @param node    节点定义
     * @param context 执行上下文
     * @param config  重试配置
     * @return 执行结果
     */
    public NodeExecutionResult executeWithRetry(WorkflowNodeEntity node,
                                                ExecutionContext context,
                                                RetryConfig config) {
        int maxRetries = config.getMaxRetries();
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            attempt++;

            try {
                // 解析输入参数
                Map<String, Object> inputs = parameterResolver.resolveInputs(node, context);

                // 获取执行器
                String nodeType = node.getType();
                if (nodeExecutorRegistry == null || !nodeExecutorRegistry.hasExecutor(nodeType)) {
                    return NodeExecutionResult.failure(
                            "未找到节点执行器: " + nodeType
                    );
                }

                NodeExecutor executor = nodeExecutorRegistry.getExecutor(nodeType);

                // 执行节点
                NodeExecutionResult result = executor.execute(node, inputs, context);

                if (result.isSuccess()) {
                    if (attempt > 1) {
                        log.info("节点重试成功: nodeUuid={}, attempts={}",
                                node.getNodeUuid(), attempt);
                    }
                    return result;
                }

                // 执行失败，记录异常
                lastException = new RuntimeException(result.getErrorMessage());

                log.warn("节点执行失败: nodeUuid={}, attempt={}/{}, error={}",
                        node.getNodeUuid(), attempt, maxRetries + 1,
                        result.getErrorMessage());

            } catch (Exception e) {
                lastException = e;
                log.warn("节点执行异常: nodeUuid={}, attempt={}/{}, error={}",
                        node.getNodeUuid(), attempt, maxRetries + 1, e.getMessage());
            }

            // 检查是否需要重试
            if (attempt <= maxRetries) {
                // 计算等待时间
                long waitTime = calculateWaitTime(config, attempt - 1);

                log.info("等待重试: nodeUuid={}, waitMs={}, nextAttempt={}",
                        node.getNodeUuid(), waitTime, attempt + 1);

                // 等待
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new WorkflowExecutionException(
                            ErrorCode.INTERNAL_ERROR,
                            node.getNodeUuid(),
                            node.getName(),
                            "重试被中断"
                    );
                }
            }
        }

        // 所有重试都失败
        log.error("节点执行最终失败: nodeUuid={}, attempts={}, lastError={}",
                node.getNodeUuid(), attempt,
                lastException != null ? lastException.getMessage() : "unknown");

        return NodeExecutionResult.failure(
                lastException != null ? lastException.getMessage() : "执行失败",
                lastException
        );
    }

    /**
     * 计算等待时间（支持指数退避和随机抖动）
     */
    private long calculateWaitTime(RetryConfig config, int retryCount) {
        long baseInterval = config.getRetryInterval();

        if (config.isExponentialBackoff()) {
            // 指数退避
            baseInterval = (long) (baseInterval *
                    Math.pow(config.getBackoffMultiplier(), retryCount));
        }

        // 添加随机抖动（避免重试风暴）
        if (config.isJitter()) {
            double jitterFactor = config.getJitterFactor();
            Random random = new Random();
            double jitter = 1.0 - jitterFactor + random.nextDouble() * 2 * jitterFactor;
            baseInterval = (long) (baseInterval * jitter);
        }

        // 不超过最大间隔
        return Math.min(baseInterval, config.getMaxRetryInterval());
    }

    /**
     * 重试配置
     */
    @lombok.Data
    @lombok.Builder
    public static class RetryConfig {
        /**
         * 最大重试次数
         */
        @lombok.Builder.Default
        private int maxRetries = 3;

        /**
         * 重试间隔（毫秒）
         */
        @lombok.Builder.Default
        private long retryInterval = 1000;

        /**
         * 是否使用指数退避
         */
        @lombok.Builder.Default
        private boolean exponentialBackoff = true;

        /**
         * 指数退避倍数
         */
        @lombok.Builder.Default
        private double backoffMultiplier = 2.0;

        /**
         * 最大重试间隔（毫秒）
         */
        @lombok.Builder.Default
        private long maxRetryInterval = 60000;

        /**
         * 是否添加随机抖动
         */
        @lombok.Builder.Default
        private boolean jitter = true;

        /**
         * 随机抖动因子（0.0 - 0.5）
         */
        @lombok.Builder.Default
        private double jitterFactor = 0.3;

        /**
         * 默认配置
         */
        public static RetryConfig defaultConfig() {
            return RetryConfig.builder().build();
        }

        /**
         * 快速重试配置
         */
        public static RetryConfig fastRetry() {
            return RetryConfig.builder()
                    .maxRetries(3)
                    .retryInterval(100)
                    .exponentialBackoff(false)
                    .build();
        }

        /**
         * 慢速重试配置
         */
        public static RetryConfig slowRetry() {
            return RetryConfig.builder()
                    .maxRetries(5)
                    .retryInterval(5000)
                    .exponentialBackoff(true)
                    .backoffMultiplier(2.0)
                    .maxRetryInterval(120000)
                    .build();
        }
    }
}
