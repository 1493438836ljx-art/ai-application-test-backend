/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.exception;

/**
 * 操作执行异常
 * <p>
 * 当操作执行失败时抛出
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
public class ActionExecutionException extends RuntimeException {

    /**
     * 操作ID
     */
    private String actionId;

    /**
     * 操作路径
     */
    private String path;

    public ActionExecutionException(String message) {
        super(message);
    }

    public ActionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActionExecutionException(String actionId, String path, String message) {
        super(message);
        this.actionId = actionId;
        this.path = path;
    }

    public ActionExecutionException(String actionId, String path, String message, Throwable cause) {
        super(message, cause);
        this.actionId = actionId;
        this.path = path;
    }

    public String getActionId() {
        return actionId;
    }

    public String getPath() {
        return path;
    }
}
