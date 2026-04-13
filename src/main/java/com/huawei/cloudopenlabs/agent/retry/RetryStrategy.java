/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.retry;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 重试策略接口
 * <p>
 * 定义重试操作的标准接口
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 * @since 2026-04-13
 */
public interface RetryStrategy {

    /**
     * 执行带重试的操作
     *
     * @param action      要执行的操作
     * @param shouldRetry 判断是否应该重试的谓词
     * @param maxRetries  最大重试次数
     * @param <T>         返回类型
     * @return 操作结果
     * @throws RuntimeException 如果所有重试都失败
     */
    <T> T executeWithRetry(Supplier<T> action,
                           Predicate<Exception> shouldRetry,
                           int maxRetries);

    /**
     * 执行带重试的操作（使用默认重试谓词）
     *
     * @param action     要执行的操作
     * @param maxRetries 最大重试次数
     * @param <T>        返回类型
     * @return 操作结果
     */
    default <T> T executeWithRetry(Supplier<T> action, int maxRetries) {
        return executeWithRetry(action, e -> true, maxRetries);
    }
}
