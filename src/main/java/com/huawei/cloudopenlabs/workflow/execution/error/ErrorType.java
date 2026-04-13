/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.execution.error;

/**
 * 错误类型枚举
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
public enum ErrorType {

    /**
     * 可恢复错误 - 可通过重试解决
     */
    RECOVERABLE("可恢复错误"),

    /**
     * 业务错误 - 业务逻辑导致的错误
     */
    BUSINESS("业务错误"),

    /**
     * 系统错误 - 系统内部错误
     */
    SYSTEM("系统错误");

    private final String description;

    ErrorType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
