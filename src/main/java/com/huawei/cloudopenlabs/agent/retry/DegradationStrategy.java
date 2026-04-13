/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.retry;

import com.huawei.cloudopenlabs.agent.error.AgentErrorCode;
import com.huawei.cloudopenlabs.agent.error.AgentErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 降级策略
 * <p>
 * 当主流程失败时提供备选方案，保证系统可用性
 * </p>
 *
 * <h3>降级策略：</h3>
 * <ul>
 *   <li>query: 返回缓存数据或提示稍后重试</li>
 *   <li>action: 记录操作请求，稍后重试</li>
 *   <li>stream: 切换到非流式响应</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class DegradationStrategy {

    /**
     * 执行降级处理
     *
     * @param operation 操作类型（query/action/stream）
     * @param error     原始错误
     * @return 降级响应
     */
    public Object handleDegradation(String operation, Exception error) {
        log.warn("执行Degradation strategy: operation={}, error={}", operation, error.getMessage());

        return switch (operation.toLowerCase()) {
            case "query" -> degradeQuery(error);
            case "action" -> degradeAction(error);
            case "stream" -> degradeStream(error);
            case "execute" -> degradeExecute(error);
            default -> buildErrorResponse(error);
        };
    }

    /**
     * 查询操作降级
     */
    private Object degradeQuery(Exception error) {
        log.info("执行查询降级：返回提示信息");

        return Map.of(
                "degraded", true,
                "code", AgentErrorCode.INTERNAL_ERROR.getCode(),
                "message", "查询服务暂时不可用，请稍后重试",
                "suggestion", "您可以尝试刷新页面或稍后再试"
        );
    }

    /**
     * 操作降级
     */
    private Object degradeAction(Exception error) {
        log.info("执行操作降级：记录操作请求");

        // 在实际实现中，这里可以将操作记录到队列或数据库，稍后重试
        return Map.of(
                "degraded", true,
                "code", AgentErrorCode.INTERNAL_ERROR.getCode(),
                "message", "操作已记录，将在系统恢复后执行",
                "suggestion", "您的请求已保存，系统恢复后将自动处理"
        );
    }

    /**
     * 流式操作降级
     */
    private Object degradeStream(Exception error) {
        log.info("执行流式降级：切换到非流式模式");

        return Map.of(
                "degraded", true,
                "code", AgentErrorCode.STREAM_ERROR.getCode(),
                "message", "流式服务暂时不可用，已切换到普通模式",
                "suggestion", "请尝试刷新页面重新连接"
        );
    }

    /**
     * 执行操作降级
     */
    private Object degradeExecute(Exception error) {
        log.info("执行通用降级");

        return Map.of(
                "degraded", true,
                "code", AgentErrorCode.INTERNAL_ERROR.getCode(),
                "message", "服务暂时不可用，请稍后重试",
                "retryable", true
        );
    }

    /**
     * 构建错误响应
     */
    private AgentErrorResponse buildErrorResponse(Exception error) {
        return AgentErrorResponse.from(error);
    }

    /**
     * 将技术错误转换为用户友好的消息
     */
    public String extractUserFriendlyMessage(Exception error) {
        if (error == null) {
            return "系统异常";
        }

        String className = error.getClass().getSimpleName();

        // 超时类错误
        if (error instanceof TimeoutException || className.contains("Timeout")) {
            return "操作超时，请稍后重试";
        }

        // 网络类错误
        if (className.contains("Network") ||
            className.contains("Connection") ||
            className.contains("Socket") ||
            className.contains("Connect")) {
            return "网络连接异常，请检查网络后重试";
        }

        // 重试耗尽
        if (error instanceof RetryExhaustedException) {
            return "服务暂时不可用，请稍后重试";
        }

        // 中断
        if (error instanceof InterruptedException || error instanceof RetryInterruptedException) {
            return "操作被中断，请重试";
        }

        // 默认消息
        return "系统繁忙，请稍后重试";
    }

    /**
     * 判断是否应该降级
     */
    public boolean shouldDegrade(Exception error) {
        if (error == null) {
            return false;
        }

        String className = error.getClass().getSimpleName();

        // 超时应该降级
        if (error instanceof TimeoutException || className.contains("Timeout")) {
            return true;
        }

        // 网络错误应该降级
        if (className.contains("Network") ||
            className.contains("Connection") ||
            className.contains("Socket") ||
            className.contains("Connect")) {
            return true;
        }

        // 重试耗尽应该降级
        if (error instanceof RetryExhaustedException) {
            return true;
        }

        // 其他错误不降级，直接返回错误
        return false;
    }

    /**
     * 获取降级级别
     */
    public DegradationLevel getDegradationLevel(Exception error) {
        if (error == null) {
            return DegradationLevel.NONE;
        }

        String className = error.getClass().getSimpleName();

        // 严重错误 - 完全降级
        if (className.contains("OutOfMemory") ||
            className.contains("StackOverflow")) {
            return DegradationLevel.FULL;
        }

        // 部分降级
        if (error instanceof TimeoutException ||
            error instanceof RetryExhaustedException) {
            return DegradationLevel.PARTIAL;
        }

        // 轻微降级
        if (className.contains("Network") ||
            className.contains("Connection") ||
            className.contains("Socket")) {
            return DegradationLevel.MINOR;
        }

        return DegradationLevel.NONE;
    }

    /**
     * 降级级别枚举
     */
    public enum DegradationLevel {
        /**
         * 无降级
         */
        NONE,
        /**
         * 轻微降级（如网络抖动）
         */
        MINOR,
        /**
         * 部分降级（如超时）
         */
        PARTIAL,
        /**
         * 完全降级（如服务不可用）
         */
        FULL
    }
}
