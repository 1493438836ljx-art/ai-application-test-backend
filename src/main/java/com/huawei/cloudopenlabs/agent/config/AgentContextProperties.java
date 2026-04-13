/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 上下文配置属性
 * 用于控制上下文构建的���断参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.context")
public class AgentContextProperties {

    /**
     * 最大上下文长度（字符数）
     * 超过此长度将被截断
     * 默认: 50000 字符
     */
    private int maxLength = 50000;

    /**
     * 最大历史结果数量
     * 只保留最近的 N 条结果
     * 默认: 10 条
     */
    private int maxHistoryResults = 10;

    /**
     * 单个结果最大长度（字符数）
     * 超过此长度的结果将被截断
     * 默认: 5000 字符
     */
    private int maxResultLength = 5000;

    /**
     * 是否启用上下文截断
     * 默认: true
     */
    private boolean truncationEnabled = true;
}
