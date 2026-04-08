package com.huawei.cloudopenlabs.agent.retry;

/**
 * 重试耗尽异常
 * <p>
 * 当所有重试尝试都失败时抛出
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 */
public class RetryExhaustedException extends RuntimeException {

    private final int attemptCount;

    public RetryExhaustedException(String message) {
        super(message);
        this.attemptCount = 0;
    }

    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
        this.attemptCount = 0;
    }

    public RetryExhaustedException(String message, Throwable cause, int attemptCount) {
        super(message, cause);
        this.attemptCount = attemptCount;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
}
