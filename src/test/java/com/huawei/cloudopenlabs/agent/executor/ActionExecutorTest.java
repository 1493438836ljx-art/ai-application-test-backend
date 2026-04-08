package com.huawei.cloudopenlabs.agent.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.huawei.cloudopenlabs.agent.dto.AgentPlan;
import com.huawei.cloudopenlabs.agent.exception.ActionExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ActionExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActionExecutorTest {

    private ActionExecutor actionExecutor;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        actionExecutor = new ActionExecutor(webClientBuilder, objectMapper);
    }

    @Nested
    @DisplayName("路径变量替换测试")
    class PathResolveTests {

        @Test
        @DisplayName("替换 workflowId 变量")
        void testResolveWorkflowId() {
            // Given
            String path = "/api/workflow/{workflowId}/nodes";
            Long workflowId = 123L;

            // When
            String result = actionExecutor.resolvePath(path, workflowId);

            // Then
            assertEquals("/api/workflow/123/nodes", result);
        }

        @Test
        @DisplayName("替换 ${workflowId} 格式变量")
        void testResolveWorkflowIdWithDollar() {
            // Given
            String path = "/api/workflow/${workflowId}/nodes";
            Long workflowId = 456L;

            // When
            String result = actionExecutor.resolvePath(path, workflowId);

            // Then
            assertEquals("/api/workflow/456/nodes", result);
        }

        @Test
        @DisplayName("替换 id 变量")
        void testResolveId() {
            // Given
            String path = "/api/workflow/{id}/data";
            Long workflowId = 789L;

            // When
            String result = actionExecutor.resolvePath(path, workflowId);

            // Then
            assertEquals("/api/workflow/789/data", result);
        }

        @Test
        @DisplayName("路径无变量时保持不变")
        void testNoVariables() {
            // Given
            String path = "/api/workflow/create";

            // When
            String result = actionExecutor.resolvePath(path, null);

            // Then
            assertEquals("/api/workflow/create", result);
        }

        @Test
        @DisplayName("空路径处理")
        void testEmptyPath() {
            // When
            String result = actionExecutor.resolvePath("", 1L);

            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("null路径处理")
        void testNullPath() {
            // When
            String result = actionExecutor.resolvePath(null, 1L);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("错误结果构建测试")
    class ErrorResultTests {

        @Test
        @DisplayName("构建错误结果包含必要字段")
        void testBuildErrorResult() {
            // Given
            AgentPlan.Action action = AgentPlan.Action.builder()
                    .id("test-action-1")
                    .path("/api/test")
                    .build();
            Exception error = new RuntimeException("Connection refused");

            // When
            Map<String, Object> result = actionExecutor.buildErrorResult(action, error);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("error"));
            assertEquals("test-action-1", result.get("actionId"));
            assertEquals("/api/test", result.get("path"));
            assertTrue(((String) result.get("message")).contains("Connection refused"));
        }

        @Test
        @DisplayName("错误结果包含原因信息")
        void testBuildErrorResultWithCause() {
            // Given
            AgentPlan.Action action = AgentPlan.Action.builder()
                    .id("test-action-2")
                    .path("/api/test2")
                    .build();
            Exception cause = new RuntimeException("Network timeout");
            Exception error = new RuntimeException("Action failed", cause);

            // When
            Map<String, Object> result = actionExecutor.buildErrorResult(action, error);

            // Then
            assertTrue((Boolean) result.get("error"));
            assertEquals("Network timeout", result.get("cause"));
        }
    }

    @Nested
    @DisplayName("错误结果判断测试")
    class IsErrorResultTests {

        @Test
        @DisplayName("包含 error=true 的结果是错误结果")
        void testIsErrorResultTrue() {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("error", true);

            // When & Then
            assertTrue(actionExecutor.isErrorResult(result));
        }

        @Test
        @DisplayName("不包含 error 字段的结果不是错误结果")
        void testIsErrorResultFalse() {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("data", "success");

            // When & Then
            assertFalse(actionExecutor.isErrorResult(result));
        }

        @Test
        @DisplayName("error=false 的结果不是错误结果")
        void testIsErrorResultFalseValue() {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("error", false);

            // When & Then
            assertFalse(actionExecutor.isErrorResult(result));
        }

        @Test
        @DisplayName("null 结果不是错误结果")
        void testNullResult() {
            assertFalse(actionExecutor.isErrorResult(null));
        }

        @Test
        @DisplayName("非 Map 结果不是错误结果")
        void testNonMapResult() {
            assertFalse(actionExecutor.isErrorResult("string result"));
            assertFalse(actionExecutor.isErrorResult(123));
        }
    }

    @Nested
    @DisplayName("操作结果摘要测试")
    class SummaryTests {

        @Test
        @DisplayName("空结果摘要")
        void testEmptySummary() {
            // When
            ActionExecutor.ActionResultSummary summary = actionExecutor.summarizeResults(null);

            // Then
            assertEquals(0, summary.getTotalActions());
            assertEquals(0, summary.getSuccessCount());
            assertEquals(0, summary.getErrorCount());
        }

        @Test
        @DisplayName("全部成功的摘要")
        void testAllSuccessSummary() {
            // Given
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("a1", Map.of("data", "result1"));
            results.put("a2", Map.of("data", "result2"));

            // When
            ActionExecutor.ActionResultSummary summary = actionExecutor.summarizeResults(results);

            // Then
            assertEquals(2, summary.getTotalActions());
            assertEquals(2, summary.getSuccessCount());
            assertEquals(0, summary.getErrorCount());
            assertTrue(summary.isAllSuccess());
        }

        @Test
        @DisplayName("部分失败的摘要")
        void testPartialErrorSummary() {
            // Given
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("a1", Map.of("data", "result1"));
            results.put("a2", Map.of("error", true, "message", "failed"));
            results.put("a3", Map.of("data", "result3"));

            // When
            ActionExecutor.ActionResultSummary summary = actionExecutor.summarizeResults(results);

            // Then
            assertEquals(3, summary.getTotalActions());
            assertEquals(2, summary.getSuccessCount());
            assertEquals(1, summary.getErrorCount());
            assertFalse(summary.isAllSuccess());
        }
    }

    @Nested
    @DisplayName("空操作列表处理测试")
    class EmptyActionsTests {

        @Test
        @DisplayName("null 操作列表返回空结果")
        void testNullActions() {
            // When
            Map<String, Object> results = actionExecutor.executeActions(null, 1L);

            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("空操作列表返回空结果")
        void testEmptyActions() {
            // When
            Map<String, Object> results = actionExecutor.executeActions(new ArrayList<>(), 1L);

            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("无事务模式下空列表返回空结果")
        void testEmptyActionsNoTransaction() {
            // When
            Map<String, Object> results = actionExecutor.executeActionsWithoutTransaction(null, 1L);

            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("ActionResultSummary 类测试")
    class ActionResultSummaryClassTests {

        @Test
        @DisplayName("toString 包含关键信息")
        void testToString() {
            // Given
            ActionExecutor.ActionResultSummary summary =
                    new ActionExecutor.ActionResultSummary(5, 4, 1, 1234L);

            // When
            String str = summary.toString();

            // Then
            assertTrue(str.contains("total=5"));
            assertTrue(str.contains("success=4"));
            assertTrue(str.contains("error=1"));
            assertTrue(str.contains("duration=1234ms"));
        }

        @Test
        @DisplayName("isAllSuccess 正确判断")
        void testIsAllSuccess() {
            // 无错误
            ActionExecutor.ActionResultSummary successSummary =
                    new ActionExecutor.ActionResultSummary(3, 3, 0, 100L);
            assertTrue(successSummary.isAllSuccess());

            // 有错误
            ActionExecutor.ActionResultSummary errorSummary =
                    new ActionExecutor.ActionResultSummary(3, 2, 1, 100L);
            assertFalse(errorSummary.isAllSuccess());
        }
    }
}
