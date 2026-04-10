/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.error;

import lombok.Getter;

/**
 * Agent 统一错误码枚举
 * <p>
 * 定义了 Agent 服务的所有错误码，便于统一管理和国际化
 * </p>
 *
 * <h3>错误码规范：</h3>
 * <ul>
 *   <li>A0000: 成功</li>
 *   <li>A1xxx: 客户端错误（请求参数、会话状态等）</li>
 *   <li>A2xxx: 服务端错误（内部错误、执行失败等）</li>
 *   <li>A3xxx: 外部服务错误（CLI、数据库等）</li>
 *   <li>A4xxx: 限流错误（并发、速率限制等）</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 */
@Getter
public enum AgentErrorCode {

    // ==================== 成功 ====================
    SUCCESS("A0000", "操作成功"),

    // ==================== 客户端错误 (A1xxx) ====================
    INVALID_SESSION_ID("A1001", "无效的会话ID格式"),
    SESSION_BUSY("A1002", "会话正在处理中"),
    MAX_ROUNDS_EXCEEDED("A1003", "超过最大轮次限制"),
    INVALID_REQUEST("A1004", "请求参数无效"),
    SESSION_NOT_FOUND("A1005", "会话不存在"),
    SESSION_EXPIRED("A1006", "会话已过期"),
    SKILL_NOT_FOUND("A1007", "Skill文件不存在"),
    CONTEXT_TOO_LARGE("A1008", "上下文超长"),
    INVALID_QUERY("A1009", "查询语句无效"),

    // ==================== 服务端错误 (A2xxx) ====================
    INTERNAL_ERROR("A2001", "服务器内部错误"),
    CLI_EXECUTION_FAILED("A2002", "CLI执行失败"),
    PARSE_ERROR("A2003", "响应解析失败"),
    TIMEOUT("A2004", "操作超时"),
    ORCHESTRATION_FAILED("A2005", "编排执行失败"),
    QUERY_EXECUTION_FAILED("A2006", "查询执行失败"),
    ACTION_EXECUTION_FAILED("A2007", "操作执行失败"),
    RESPONSE_BUILD_FAILED("A2008", "响应构建失败"),
    STREAM_ERROR("A2009", "流式传输错误"),
    HEARTBEAT_FAILED("A2010", "心跳检测失败"),

    // ==================== 外部服务错误 (A3xxx) ====================
    EXTERNAL_SERVICE_ERROR("A3001", "外部服务异常"),
    DATABASE_ERROR("A3002", "数据库操作失败"),
    NETWORK_ERROR("A3003", "网络连接异常"),
    FILE_SYSTEM_ERROR("A3004", "文件系统错误"),
    CACHE_ERROR("A3005", "缓存操作失败"),

    // ==================== 限流错误 (A4xxx) ====================
    RATE_LIMIT_EXCEEDED("A4001", "请求过于频繁"),
    CONCURRENT_LIMIT("A4002", "并发数超限"),
    RESOURCE_EXHAUSTED("A4003", "资源耗尽"),
    QUEUE_FULL("A4004", "任务队列已满");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    AgentErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 创建标准错误响应
     */
    public AgentErrorResponse toResponse() {
        return new AgentErrorResponse(code, message);
    }

    /**
     * 创建带详情的错误响应
     */
    public AgentErrorResponse toResponse(String detail) {
        return new AgentErrorResponse(code, message, detail);
    }

    /**
     * 创建带异常信息的错误响应
     */
    public AgentErrorResponse toResponse(Throwable cause) {
        String detail = cause != null ? cause.getMessage() : null;
        return new AgentErrorResponse(code, message, detail);
    }

    /**
     * 根据错误码查找枚举
     */
    public static AgentErrorCode fromCode(String code) {
        for (AgentErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }

    /**
     * 判断是否为客户端错误
     */
    public boolean isClientError() {
        return code.startsWith("A1");
    }

    /**
     * 判断是否为服务端错误
     */
    public boolean isServerError() {
        return code.startsWith("A2");
    }

    /**
     * 判断是否为外部服务错误
     */
    public boolean isExternalError() {
        return code.startsWith("A3");
    }

    /**
     * 判断是否为限流错误
     */
    public boolean isRateLimitError() {
        return code.startsWith("A4");
    }
}
