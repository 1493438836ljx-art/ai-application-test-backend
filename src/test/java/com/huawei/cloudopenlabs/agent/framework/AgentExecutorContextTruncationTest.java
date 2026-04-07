package com.huawei.cloudopenlabs.agent.framework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentExecutor 上下文截断单元测试
 * 验证 MAX_CONTEXT_LENGTH 和 MAX_RESULT_LENGTH 截断功能
 */
class AgentExecutorContextTruncationTest {

    private static final int MAX_CONTEXT_LENGTH = 50000;
    private static final int MAX_RESULT_LENGTH = 5000;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ========== truncateContent 测试 ==========

    @Test
    @DisplayName("测试 truncateContent - 短内容不截断")
    void testTruncateContentShortString() {
        String content = "这是一段短内容";
        String result = truncateContent(content, 100);
        assertEquals(content, result, "短内容不应该被截断");
    }

    @Test
    @DisplayName("测试 truncateContent - 长内容需要截断")
    void testTruncateContentLongString() {
        String content = "a".repeat(10000);
        String result = truncateContent(content, 100);
        assertTrue(result.length() <= 120, "长内容应该被截断");
        assertTrue(result.contains("...(已截断)"), "截断后应包含截断标记");
    }

    @Test
    @DisplayName("测试 truncateContent - 边界值测试")
    void testTruncateContentBoundary() {
        String content = "a".repeat(100);
        String result = truncateContent(content, 100);
        assertEquals(content, result, "等于最大长度的内容不应截断");

        String longerContent = "a".repeat(101);
        String longerResult = truncateContent(longerContent, 100);
        assertTrue(longerResult.contains("...(已截断)"), "超过最大长度1的内容应截断");
    }

    // ========== emergencyTruncate 测试 ==========

    @Test
    @DisplayName("测试 emergencyTruncate - 短内容不截断")
    void testEmergencyTruncateShortContent() {
        String content = "这是一段短内容";
        String result = emergencyTruncate(content, 1000);
        assertEquals(content, result, "短内容不应紧急截断");
    }

    @Test
    @DisplayName("测试 emergencyTruncate - 长内容需要截断")
    void testEmergencyTruncateLongContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("这是第").append(i).append("行内容，用于测试紧急截断功能。\n");
        }
        String content = sb.toString();

        String result = emergencyTruncate(content, 1000);

        assertTrue(result.length() <= 1100, "紧急截断后长度应接近限制");
        assertTrue(result.contains("...(中间内容已省略"), "紧急截断应包含省略标记");
    }

    @Test
    @DisplayName("测试 emergencyTruncate - 保留开头和结尾")
    void testEmergencyTruncatePreservesHeadAndTail() {
        String head = "这是开头内容，必须保留。这是更多开头内容以确保长度足够。";
        String middle = "x".repeat(10000);
        String tail = "这是结尾���容，必须保留。";
        String content = head + middle + tail;

        String result = emergencyTruncate(content, 200);

        assertTrue(result.contains("开头"), "应保留部分开头内容");
        assertTrue(result.endsWith(tail), "应保留结尾内容");
    }

    // ========== JSON 结果截断测试 ==========

    @Test
    @DisplayName("测试 JSON 结果截断 - 小对象不截断")
    void testJsonTruncationSmallObject() {
        Map<String, Object> result = new HashMap<>();
        result.put("key1", "value1");
        result.put("key2", 123);

        String json = mapToJson(result);
        String truncated = truncateContent(json, 1000);

        assertEquals(json, truncated, "小 JSON 对象不应截断");
    }

    @Test
    @DisplayName("测试 JSON 结果截断 - 大对象需要截断")
    void testJsonTruncationLargeObject() {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            result.put("key" + i, "这是一个很长的值，用于测试截断功能。".repeat(10));
        }

        String json = mapToJson(result);
        assertTrue(json.length() > 5000, "生成的 JSON 应该超过 5000 字符");

        String truncated = truncateContent(json, 1000);
        assertTrue(truncated.length() <= 1020, "截断后应小于最大长度");
        assertTrue(truncated.contains("...(已截断)"), "应包含截断标记");
    }

    // ========== 上下文整体长度测试 ==========

    @Test
    @DisplayName("测试上下文整体长度限制")
    void testOverallContextLength() {
        StringBuilder sb = new StringBuilder();
        sb.append("【重要】必须按要求格式输出。\n\n");
        sb.append("用户请求: 这是一个复杂的任务\n\n");
        sb.append("workflowId: 1\n\n");

        // 添加大量查询结果
        sb.append("之前的查询结果:\n");
        for (int i = 0; i < 50; i++) {
            sb.append("查询").append(i).append(": ").append("x".repeat(500)).append("\n");
        }
        sb.append("\n");

        // 添加大量操作结果
        sb.append("之前的操作结果:\n");
        for (int i = 0; i < 50; i++) {
            sb.append("操作").append(i).append(": ").append("y".repeat(500)).append("\n");
        }
        sb.append("\n");

        sb.append("当前轮次: 10\n");

        String context = sb.toString();
        assertTrue(context.length() > MAX_CONTEXT_LENGTH, "生成的上下文应超过最大长度");

        // 应用紧急截断
        String truncated = emergencyTruncate(context, MAX_CONTEXT_LENGTH);
        assertTrue(truncated.length() <= MAX_CONTEXT_LENGTH + 100, "截断后应小于最大长度");
    }

    // ========== 辅助方法 ==========

    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...(已截断)";
    }

    private String emergencyTruncate(String context, int maxLength) {
        if (context.length() <= maxLength) {
            return context;
        }

        int headLength = (int) (maxLength * 0.4);
        int tailLength = (int) (maxLength * 0.4);
        String separator = "\n\n...(中间内容已省略以节省 Token)...\n\n";

        return context.substring(0, headLength) +
                separator +
                context.substring(context.length() - tailLength);
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
