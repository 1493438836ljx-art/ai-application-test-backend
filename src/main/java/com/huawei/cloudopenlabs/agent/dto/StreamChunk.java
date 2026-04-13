/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式响应块
 * 用于解析 AI 服务的 SSE 流式响应
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamChunk {

    /**
     * 事件类型
     * - start: 会话开始
     * - chunk: 内容块
     * - done: 执行完成
     * - error: 发生错误
     */
    private String type;

    /**
     * 内容
     * 对于 chunk 类型，包含实际输出内容
     * 对于 error 类型，包含错误信息
     * 注意：可能是字符串或嵌套对象（如 thinking、tool_use 等）
     */
    private Object content;

    /**
     * 内容类型
     * - thinking: AI 思考过程
     * - text: 实际输出文本
     * - tool_use: 工具使用
     * - result: 最终结果
     */
    private String contentType;

    /**
     * 工具名称（仅当 contentType 为 tool_use 时有效）
     */
    private String toolName;

    /**
     * 工具输入参数（仅当 contentType 为 tool_use 时有效）
     */
    private Object toolInput;

    /**
     * 会话ID
     * 用于多 round, 的会话持久化
     */
    private String sessionId;

    /**
     * 执行耗时（毫秒）
     * 仅在 done 事件中存在
     */
    private Long duration;

    /**
     * 错误信息
     * 仅在 error 事件中存在
     */
    private String message;

    /**
     * 获取内容（兼容 content 和 message 字段）
     * 将 content 转换为字符串格式
     */
    public String getContentOrMessage() {
        if (content != null) {
            if (content instanceof String) {
                String str = (String) content;
                if (!str.isEmpty()) {
                    return str;
                }
            } else {
                // 对于嵌套对象，提取实际的文本内容
                if (content instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) content;
                    // 尝试从嵌套结构中提取文本
                    Object textContent = map.get("text");
                    if (textContent instanceof String) {
                        return (String) textContent;
                    }
                    Object thinking = map.get("thinking");
                    if (thinking instanceof String) {
                        return (String) thinking;
                    }
                    // 如果没有找到文本，返回整个对象的字符串表示
                    return content.toString();
                }
                return content.toString();
            }
        }
        return message;
    }
}
