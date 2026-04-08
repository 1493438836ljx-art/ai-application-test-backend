package com.huawei.cloudopenlabs.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.config.AgentContextProperties;
import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import com.huawei.cloudopenlabs.agent.executor.*;
import com.huawei.cloudopenlabs.agent.framework.ContextBuilder;
import com.huawei.cloudopenlabs.agent.parser.ResponseParser;
import com.huawei.cloudopenlabs.agent.service.AgentSessionService;
import com.huawei.cloudopenlabs.agent.service.LockService;
import com.huawei.cloudopenlabs.agent.skill.SkillManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentOrchestrator 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock
    private ClaudeCliExecutor cliExecutor;

    @Mock
    private ContextBuilder contextBuilder;

    @Mock
    private ResponseParser responseParser;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private ActionExecutor actionExecutor;

    @Mock
    private AgentSessionService sessionService;

    @Mock
    private LockService lockService;

    @Mock
    private SkillManager skillManager;

    @Mock
    private AgentContextProperties contextProperties;

    private ObjectMapper objectMapper;

    private AgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orchestrator = new AgentOrchestrator(
                cliExecutor,
                contextBuilder,
                responseParser,
                queryExecutor,
                actionExecutor,
                sessionService,
                lockService,
                skillManager,
                contextProperties,
                objectMapper
        );
    }

    @Nested
    @DisplayName("锁竞争测试")
    class LockContentionTests {

        @Test
        @DisplayName("获取锁成功时正常处理")
        void testLockAcquiredSuccessfully() {
            // Given
            String sessionId = "test-session-1";
            Long workflowId = 1L;
            String userMessage = "测试消息";

            AgentSessionEntity session = createMockSession(sessionId, workflowId);

            when(lockService.tryLock(sessionId)).thenReturn(true);
            when(sessionService.getOrCreateSession(workflowId, sessionId)).thenReturn(session);
            when(contextBuilder.buildInitialContext(anyString(), any())).thenReturn("测试上下文");
            when(skillManager.prepareSkill(sessionId)).thenReturn("/path/to/skill");

            AtomicBoolean onErrorCalled = new AtomicBoolean(false);
            AtomicBoolean onStartCalled = new AtomicBoolean(false);

            AgentOrchestrator.StreamCallback callback = new AgentOrchestrator.StreamCallback() {
                @Override
                public void onChunk(StreamChunk chunk) {}

                @Override
                public void onError(String error) {
                    onErrorCalled.set(true);
                }

                @Override
                public void onStart(String sid) {
                    onStartCalled.set(true);
                }
            };

            // When
            doAnswer(invocation -> null).when(cliExecutor).executeStream(any(), any(), any(), any());

            orchestrator.processMessageStream(userMessage, workflowId, sessionId, callback);

            // Then
            assertTrue(onStartCalled.get());
            assertFalse(onErrorCalled.get());
            verify(lockService).unlock(sessionId);
        }

        @Test
        @DisplayName("获取锁失败时拒绝请求")
        void testLockAcquisitionFailed() {
            // Given
            String sessionId = "busy-session";

            when(lockService.tryLock(sessionId)).thenReturn(false);

            AtomicReference<String> errorMessage = new AtomicReference<>();
            AgentOrchestrator.StreamCallback callback = new AgentOrchestrator.StreamCallback() {
                @Override
                public void onChunk(StreamChunk chunk) {}

                @Override
                public void onError(String error) {
                    errorMessage.set(error);
                }
            };

            // When
            orchestrator.processMessageStream("test", 1L, sessionId, callback);

            // Then
            assertNotNull(errorMessage.get());
            assertTrue(errorMessage.get().contains("会话正在处理中"));
            verify(lockService, never()).unlock(anyString());
        }

        @Test
        @DisplayName("空会话ID时生成新ID并获取锁")
        void testNullSessionIdGeneratesNewId() {
            // Given
            Long workflowId = 1L;

            when(lockService.tryLock(anyString())).thenReturn(false);

            AtomicReference<String> errorMessage = new AtomicReference<>();
            AgentOrchestrator.StreamCallback callback = new AgentOrchestrator.StreamCallback() {
                @Override
                public void onChunk(StreamChunk chunk) {}

                @Override
                public void onError(String error) {
                    errorMessage.set(error);
                }
            };

            // When
            orchestrator.processMessageStream("test", workflowId, null, callback);

            // Then - 验证生成了新ID并尝试获取锁
            verify(lockService).tryLock(argThat(id -> id != null && id.startsWith("new-")));
        }
    }

    @Nested
    @DisplayName("会话摘要测试")
    class SessionSummaryTests {

        @Test
        @DisplayName("获取存在的会话摘要")
        void testGetExistingSessionSummary() throws Exception {
            // Given
            String sessionId = "test-session-summary";
            AgentSessionEntity session = createMockSession(sessionId, 1L);
            session.setRoundCount(5);
            session.setQueryResults("{\"q1\":{},\"q2\":{}}");
            session.setActionResults("{\"a1\":{}}");
            session.setStartTime(System.currentTimeMillis() - 1000);

            when(sessionService.getByConversationId(sessionId)).thenReturn(Optional.of(session));

            // When
            AgentOrchestrator.SessionSummary summary = orchestrator.getSessionSummary(sessionId);

            // Then
            assertNotNull(summary);
            assertEquals(sessionId, summary.sessionId());
            assertEquals(5, summary.roundCount());
            assertEquals(2, summary.queryCount());
            assertEquals(1, summary.actionCount());
        }

        @Test
        @DisplayName("获取不存在的会话摘要返回null")
        void testGetNonExistentSessionSummary() {
            // Given
            when(sessionService.getByConversationId("non-existent")).thenReturn(Optional.empty());

            // When
            AgentOrchestrator.SessionSummary summary = orchestrator.getSessionSummary("non-existent");

            // Then
            assertNull(summary);
        }

        @Test
        @DisplayName("会话摘要toString格式正确")
        void testSessionSummaryToString() {
            // Given
            AgentOrchestrator.SessionSummary summary = new AgentOrchestrator.SessionSummary(
                    "test-session",
                    3,
                    "ACTIVE",
                    1500L,
                    2,
                    1
            );

            // When
            String str = summary.toString();

            // Then
            assertTrue(str.contains("test-session"));
            assertTrue(str.contains("rounds=3"));
            assertTrue(str.contains("ACTIVE"));
            assertTrue(str.contains("duration=1500ms"));
            assertTrue(str.contains("queries=2"));
            assertTrue(str.contains("actions=1"));
        }

        @Test
        @DisplayName("空查询和操作结果计数为0")
        void testEmptyResultsCount() throws Exception {
            // Given
            String sessionId = "empty-session";
            AgentSessionEntity session = createMockSession(sessionId, 1L);
            session.setQueryResults(null);
            session.setActionResults(null);

            when(sessionService.getByConversationId(sessionId)).thenReturn(Optional.of(session));

            // When
            AgentOrchestrator.SessionSummary summary = orchestrator.getSessionSummary(sessionId);

            // Then
            assertEquals(0, summary.queryCount());
            assertEquals(0, summary.actionCount());
        }
    }

    @Nested
    @DisplayName("流程控制测试")
    class FlowControlTests {

        @Test
        @DisplayName("首次会话调用Skill准备")
        void testFirstSessionSkillPreparation() {
            // Given
            String sessionId = "new-session";
            Long workflowId = 1L;
            AgentSessionEntity session = createMockSession(sessionId, workflowId);
            session.setRoundCount(0);

            when(lockService.tryLock(sessionId)).thenReturn(true);
            when(sessionService.getOrCreateSession(workflowId, sessionId)).thenReturn(session);
            when(skillManager.prepareSkill(sessionId)).thenReturn("/skill/dir");
            when(contextBuilder.buildInitialContext(anyString(), any())).thenReturn("context");

            // When
            orchestrator.processMessageStream("test", workflowId, sessionId, createNoOpCallback());

            // Then
            verify(skillManager).prepareSkill(sessionId);
        }

        @Test
        @DisplayName("非首次会话不调用Skill准备")
        void testNonFirstSessionNoSkillPreparation() {
            // Given
            String sessionId = "existing-session";
            Long workflowId = 1L;
            AgentSessionEntity session = createMockSession(sessionId, workflowId);
            session.setRoundCount(2);

            when(lockService.tryLock(sessionId)).thenReturn(true);
            when(sessionService.getOrCreateSession(workflowId, sessionId)).thenReturn(session);
            when(contextBuilder.buildInitialContext(anyString(), any())).thenReturn("context");

            // When
            orchestrator.processMessageStream("test", workflowId, sessionId, createNoOpCallback());

            // Then
            verify(skillManager, never()).prepareSkill(anyString());
        }

        @Test
        @DisplayName("异常时正确释放锁")
        void testLockReleasedOnException() {
            // Given
            String sessionId = "exception-session";
            Long workflowId = 1L;

            when(lockService.tryLock(sessionId)).thenReturn(true);
            when(sessionService.getOrCreateSession(workflowId, sessionId))
                    .thenThrow(new RuntimeException("测试异常"));

            AtomicReference<String> errorMessage = new AtomicReference<>();
            AgentOrchestrator.StreamCallback callback = new AgentOrchestrator.StreamCallback() {
                @Override
                public void onChunk(StreamChunk chunk) {}

                @Override
                public void onError(String error) {
                    errorMessage.set(error);
                }
            };

            // When
            orchestrator.processMessageStream("test", workflowId, sessionId, callback);

            // Then
            assertNotNull(errorMessage.get());
            verify(lockService).unlock(sessionId);
        }
    }

    @Nested
    @DisplayName("StreamCallback 接口测试")
    class StreamCallbackInterfaceTests {

        @Test
        @DisplayName("默认方法可以被调用")
        void testDefaultMethods() {
            // Given
            AgentOrchestrator.StreamCallback callback = new AgentOrchestrator.StreamCallback() {
                @Override
                public void onChunk(StreamChunk chunk) {}

                @Override
                public void onError(String error) {}
            };

            // When & Then - 默认方法不应抛出异常
            assertDoesNotThrow(() -> callback.onStart("test-session"));
            assertDoesNotThrow(() -> callback.onWorkflowUpdate(new Object()));
            assertDoesNotThrow(() -> callback.onDone("test-session", 1000L));
        }
    }

    @Nested
    @DisplayName("上下文构建测试")
    class ContextBuilderTests {

        @Test
        @DisplayName("调用上下文构建器")
        void testContextBuilderCalled() {
            // Given
            String sessionId = "ctx-session";
            Long workflowId = 1L;
            String userMessage = "用户消息";
            AgentSessionEntity session = createMockSession(sessionId, workflowId);

            when(lockService.tryLock(sessionId)).thenReturn(true);
            when(sessionService.getOrCreateSession(workflowId, sessionId)).thenReturn(session);
            when(contextBuilder.buildInitialContext(userMessage, session)).thenReturn("构建的上下文");

            // When
            orchestrator.processMessageStream(userMessage, workflowId, sessionId, createNoOpCallback());

            // Then
            verify(contextBuilder).buildInitialContext(userMessage, session);
        }
    }

    // ==================== 辅助方法 ====================

    private AgentSessionEntity createMockSession(String sessionId, Long workflowId) {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setConversationId(sessionId);
        session.setWorkflowId(workflowId);
        session.setStatus("ACTIVE");
        session.setRoundCount(0);
        session.setParseErrorCount(0);
        session.setStartTime(System.currentTimeMillis());
        return session;
    }

    private AgentOrchestrator.StreamCallback createNoOpCallback() {
        return new AgentOrchestrator.StreamCallback() {
            @Override
            public void onChunk(StreamChunk chunk) {}

            @Override
            public void onError(String error) {}
        };
    }
}
