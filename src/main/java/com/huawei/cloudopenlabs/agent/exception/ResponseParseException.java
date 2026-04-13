/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.exception;

/**
 * 响应解析异常
 * <p>
 * 当 AI 响应解析失败时抛出
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
public class ResponseParseException extends RuntimeException {

    /**
     * 原始响应内容
     */
    private String rawResponse;

    public ResponseParseException(String message) {
        super(message);
    }

    public ResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResponseParseException(String message, String rawResponse) {
        super(message);
        this.rawResponse = rawResponse;
    }

    public ResponseParseException(String message, String rawResponse, Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
