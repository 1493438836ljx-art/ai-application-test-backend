package com.example.demo.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 配置类
 * <p>
 * 用于配置 Agent 执行时的参数
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {

    /**
     * 超时时间（秒），默认 120 秒
     */
    @Builder.Default
    private Integer timeout = 120;

    /**
     * 是否开启调试模式
     */
    @Builder.Default
    private Boolean debug = false;

    /**
     * 其他自定义配置参数
     */
    @Builder.Default
    private Map<String, Object> extraParams = null;

    /**
     * 将配置转换为 JSON 字符串
     *
     * @return JSON 字符串
     */
    public String toJsonString() {
        StringBuilder json = new StringBuilder("{");
        json.append("\"timeout\":").append(timeout);
        json.append(",\"debug\":").append(debug);
        if (extraParams != null && !extraParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : extraParams.entrySet()) {
                json.append(",\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value).append("\"");
                } else {
                    json.append(value);
                }
            }
        }
        json.append("}");
        return json.toString();
    }
}
