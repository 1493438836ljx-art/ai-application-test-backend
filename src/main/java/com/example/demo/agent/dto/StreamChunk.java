package com.example.demo.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式响应块
 * 用于解析 AI 服务的 SSE 流式响应
 *
 * @author AI Test Platform Team
 * @version 1.0.0
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
     */
    private String content;

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
     * 用于多轮对话的会话持久化
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
     */
    public String getContentOrMessage() {
        if (content != null && !content.isEmpty()) {
            return content;
        }
        return message;
    }
}
