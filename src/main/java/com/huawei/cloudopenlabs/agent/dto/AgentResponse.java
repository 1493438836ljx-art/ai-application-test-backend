/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 统一响应类
 * <p>
 * 供外部调用的统一响应格式，包装了 Claude Code 执行结果
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    /**
     * 执行是否成功
     */
    private Boolean success;

    /**
     * Claude 的响应内容
     */
    private String response;

    /**
     * 错误信息（执行失败时）
     */
    private String error;

    /**
     * 错误码（执行失败时）
     */
    private Integer errorCode;

    /**
     * 原始请求内容（用于追溯）
     */
    private String originalTaskContent;

    /**
     * 执行耗时（毫秒）
     */
    private Long executionTimeMs;
}
