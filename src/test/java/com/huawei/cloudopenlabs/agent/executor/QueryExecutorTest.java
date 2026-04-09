package com.huawei.cloudopenlabs.agent.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.dto.AgentPlan;
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
 * QueryExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class QueryExecutorTest {

    private QueryExecutor queryExecutor;

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
        queryExecutor = new QueryExecutor(webClientBuilder, objectMapper);
    }

    @Nested
    @DisplayName("路径变量替换测试")
    class PathResolveTests {

        @Test
        @DisplayName("替换 workflowId 变量")
        void testResolveWorkflowId() {
            // Given
            String path = "/api/workflow/{workflowId}/nodes";
            String workflowId = "test-workflow-123";

            // When
            String result = queryExecutor.resolvePath(path, workflowId);

            // Then
            assertEquals("/api/workflow/test-workflow-123/nodes", result);
        }

        @Test
        @DisplayName("替换 ${workflowId} 格式变量")
        void testResolveWorkflowIdWithDollar() {
            // Given
            String path = "/api/workflow/${workflowId}/nodes";
            String workflowId = "test-workflow-456";

            // When
            String result = queryExecutor.resolvePath(path, workflowId);

            // Then
            assertEquals("/api/workflow/test-workflow-456/nodes", result);
        }

        @Test
        @DisplayName("替换 id 变量")
        void testResolveId() {
            // Given
            String path = "/api/workflow/{id}";
            String workflowId = "test-workflow-789";

            // When
            String result = queryExecutor.resolvePath(path, workflowId);

            // Then
            assertEquals("/api/workflow/test-workflow-789", result);
        }

        @Test
        @DisplayName("路径无变量时保持不变")
        void testNoVariables() {
            // Given
            String path = "/api/workflow/list";

            // When
            String result = queryExecutor.resolvePath(path, null);

            // Then
            assertEquals("/api/workflow/list", result);
        }

        @Test
        @DisplayName("空路径处理")
        void testEmptyPath() {
            // When
            String result = queryExecutor.resolvePath("", "test-workflow-1");

            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("null路径处理")
        void testNullPath() {
            // When
            String result = queryExecutor.resolvePath(null, "test-workflow-1");

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
            AgentPlan.Query query = AgentPlan.Query.builder()
                    .id("test-query-1")
                    .path("/api/test")
                    .build();
            Exception error = new RuntimeException("Connection refused");

            // When
            Map<String, Object> result = queryExecutor.buildErrorResult(query, error);

            // Then
            assertNotNull(result);
            assertTrue((Boolean) result.get("error"));
            assertEquals("test-query-1", result.get("queryId"));
            assertEquals("/api/test", result.get("path"));
            assertTrue(((String) result.get("message")).contains("Connection refused"));
        }

        @Test
        @DisplayName("错误结果包含原因信息")
        void testBuildErrorResultWithCause() {
            // Given
            AgentPlan.Query query = AgentPlan.Query.builder()
                    .id("test-query-2")
                    .path("/api/test2")
                    .build();
            Exception cause = new RuntimeException("Network timeout");
            Exception error = new RuntimeException("Query failed", cause);

            // When
            Map<String, Object> result = queryExecutor.buildErrorResult(query, error);

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
            assertTrue(queryExecutor.isErrorResult(result));
        }

        @Test
        @DisplayName("不包含 error 字段的结果不是错误结果")
        void testIsErrorResultFalse() {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("data", "success");

            // When & Then
            assertFalse(queryExecutor.isErrorResult(result));
        }

        @Test
        @DisplayName("error=false 的结果不是错误结果")
        void testIsErrorResultFalseValue() {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("error", false);

            // When & Then
            assertFalse(queryExecutor.isErrorResult(result));
        }

        @Test
        @DisplayName("null 结果不是错误结果")
        void testNullResult() {
            assertFalse(queryExecutor.isErrorResult(null));
        }

        @Test
        @DisplayName("非 Map 结果不是错误结果")
        void testNonMapResult() {
            assertFalse(queryExecutor.isErrorResult("string result"));
            assertFalse(queryExecutor.isErrorResult(123));
        }
    }

    @Nested
    @DisplayName("查询结果摘要测试")
    class SummaryTests {

        @Test
        @DisplayName("空结果摘要")
        void testEmptySummary() {
            // When
            QueryExecutor.QueryResultSummary summary = queryExecutor.summarizeResults(null);

            // Then
            assertEquals(0, summary.getTotalQueries());
            assertEquals(0, summary.getSuccessCount());
            assertEquals(0, summary.getErrorCount());
        }

        @Test
        @DisplayName("全部成功的摘要")
        void testAllSuccessSummary() {
            // Given
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("q1", Map.of("data", "result1"));
            results.put("q2", Map.of("data", "result2"));

            // When
            QueryExecutor.QueryResultSummary summary = queryExecutor.summarizeResults(results);

            // Then
            assertEquals(2, summary.getTotalQueries());
            assertEquals(2, summary.getSuccessCount());
            assertEquals(0, summary.getErrorCount());
            assertTrue(summary.isAllSuccess());
        }

        @Test
        @DisplayName("部分失败的摘要")
        void testPartialErrorSummary() {
            // Given
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("q1", Map.of("data", "result1"));
            results.put("q2", Map.of("error", true, "message", "failed"));
            results.put("q3", Map.of("data", "result3"));

            // When
            QueryExecutor.QueryResultSummary summary = queryExecutor.summarizeResults(results);

            // Then
            assertEquals(3, summary.getTotalQueries());
            assertEquals(2, summary.getSuccessCount());
            assertEquals(1, summary.getErrorCount());
            assertFalse(summary.isAllSuccess());
        }

        @Test
        @DisplayName("全部失败的摘要")
        void testAllErrorSummary() {
            // Given
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("q1", Map.of("error", true, "message", "failed1"));
            results.put("q2", Map.of("error", true, "message", "failed2"));

            // When
            QueryExecutor.QueryResultSummary summary = queryExecutor.summarizeResults(results);

            // Then
            assertEquals(2, summary.getTotalQueries());
            assertEquals(0, summary.getSuccessCount());
            assertEquals(2, summary.getErrorCount());
            assertFalse(summary.isAllSuccess());
        }
    }

    @Nested
    @DisplayName("空查询列表处理测试")
    class EmptyQueriesTests {

        @Test
        @DisplayName("null 查询列表返回空结果")
        void testNullQueries() {
            // When
            Map<String, Object> results = queryExecutor.executeQueries(null, "test-workflow-1");

            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("空查询列表返回空结果")
        void testEmptyQueries() {
            // When
            Map<String, Object> results = queryExecutor.executeQueries(new ArrayList<>(), "test-workflow-1");

            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("QueryResultSummary 类测试")
    class QueryResultSummaryClassTests {

        @Test
        @DisplayName("toString 包含关键信息")
        void testToString() {
            // Given
            QueryExecutor.QueryResultSummary summary =
                    new QueryExecutor.QueryResultSummary(5, 4, 1, 1234L);

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
            QueryExecutor.QueryResultSummary successSummary =
                    new QueryExecutor.QueryResultSummary(3, 3, 0, 100L);
            assertTrue(successSummary.isAllSuccess());

            // 有错误
            QueryExecutor.QueryResultSummary errorSummary =
                    new QueryExecutor.QueryResultSummary(3, 2, 1, 100L);
            assertFalse(errorSummary.isAllSuccess());
        }
    }
}
