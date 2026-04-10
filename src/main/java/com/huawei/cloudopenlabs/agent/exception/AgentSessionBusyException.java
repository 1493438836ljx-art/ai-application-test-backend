/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.exception;

/**
 * Agent 会话繁忙异常
 * 当会话正在被其他请求处理时抛出
 */
public class AgentSessionBusyException extends RuntimeException {

    public AgentSessionBusyException(String message) {
        super(message);
    }

    public AgentSessionBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
