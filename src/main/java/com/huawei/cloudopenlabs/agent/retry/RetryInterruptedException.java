/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.retry;

/**
 * 重试中断异常
 * <p>
 * 当重试过程被中断时抛出
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 * @since 2026-04-13
 */
public class RetryInterruptedException extends RuntimeException {

    public RetryInterruptedException(String message) {
        super(message);
    }

    public RetryInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
