/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.config.AgentContextProperties;
import com.huawei.cloudopenlabs.agent.dto.AgentPlan;
import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import com.huawei.cloudopenlabs.agent.exception.*;
import com.huawei.cloudopenlabs.agent.executor.*;
import com.huawei.cloudopenlabs.agent.framework.ContextBuilder;
import com.huawei.cloudopenlabs.agent.parser.ResponseParser;
import com.huawei.cloudopenlabs.agent.service.AgentSessionService;
import com.huawei.cloudopenlabs.agent.service.LockService;
import com.huawei.cloudopenlabs.agent.skill.SkillManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 编排器
 * <p>
 * 负责多 round, 的编排和协调，整合所有组件
 * </p>
 *
 * <h3>整合的组件：</h3>
 * <ul>
 *   <li>ClaudeCliExecutor - CLI 执行器</li>
 *   <li>ContextBuilder - 上下文构建器</li>
 *   <li>ResponseParser - 响应解析器</li>
 *   <li>QueryExecutor - 查询执行器</li>
 *   <li>ActionExecutor - 操作执行器</li>
 *   <li>SkillManager - Skill 管理器</li>
 *   <li>LockService - 锁服务</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class AgentOrchestrator {

    private final ClaudeCliExecutor cliExecutor;
    private final ContextBuilder contextBuilder;
    private final ResponseParser responseParser;
    private final QueryExecutor queryExecutor;
    private final ActionExecutor actionExecutor;
    private final AgentSessionService sessionService;
    private final LockService lockService;
    private final SkillManager skillManager;
    private final AgentContextProperties contextProperties;
    private final ObjectMapper objectMapper;

    // ==================== 配置常量 ====================

    /**
     * 最大对话轮次
     */
    private static final int MAX_ROUNDS = 15;

    /**
     * 最大解析错误次数
     */
    private static final int MAX_PARSE_ERRORS = 3;

    @Autowired
    public AgentOrchestrator(
            ClaudeCliExecutor cliExecutor,
            @Lazy ContextBuilder contextBuilder,
            ResponseParser responseParser,
            QueryExecutor queryExecutor,
            ActionExecutor actionExecutor,
            AgentSessionService sessionService,
            LockService lockService,
            SkillManager skillManager,
            AgentContextProperties contextProperties,
            ObjectMapper objectMapper) {

        this.cliExecutor = cliExecutor;
        this.contextBuilder = contextBuilder;
        this.responseParser = responseParser;
        this.queryExecutor = queryExecutor;
        this.actionExecutor = actionExecutor;
        this.sessionService = sessionService;
        this.lockService = lockService;
        this.skillManager = skillManager;
        this.contextProperties = contextProperties;
        this.objectMapper = objectMapper;

        log.info("AgentOrchestrator initialized: maxRounds={}, maxParseErrors={}",
                MAX_ROUNDS, MAX_PARSE_ERRORS);
    }

    /**
     * 处理消息流（主入口）
     * <p>
     * 支持多 round, 的流式处理
     * </p>
     *
     * @param userMessage    用户消息
     * @param workflowId     工作流ID
     * @param conversationId 会话ID（可选，新会话传 null）
     * @param callback       流式回调
     */
    public void processMessageStream(
            String userMessage,
            String workflowId,
            String conversationId,
            StreamCallback callback) {

        // 标准化会话ID
        String sessionId = normalizeSessionId(conversationId);
        long startTime = System.currentTimeMillis();

        log.info("Starting message stream processing: sessionId={}, workflowId={}", sessionId, workflowId);

        // 获取锁
        if (!lockService.tryLock(sessionId)) {
            log.warn("Session is processing, rejecting request: sessionId={}", sessionId);
            callback.onError("会话正在处理中，请稍后重试");
            return;
        }

        try {
            // 创建或获取会话
            AgentSessionEntity session = sessionService.getOrCreateSession(workflowId, sessionId);

            // 首次会话，准备 Skill
            if (session.getRoundCount() == null || session.getRoundCount() == 0) {
                String skillDir = skillManager.prepareSkill(sessionId);
                log.info("First session, skill directory prepared: {}", skillDir);
            }

            // 发送开始事件
            callback.onStart(sessionId);

            // 构建初始上下文
            String context = contextBuilder.buildInitialContext(userMessage, session);

            // 开始多轮处理
            processRound(session, context, callback, true, startTime);

        } catch (Exception e) {
            log.error("Message stream processing exception: sessionId={}, error={}", sessionId, e.getMessage(), e);
            callback.onError("处理消息失败: " + e.getMessage());
        } finally {
            lockService.unlock(sessionId);
        }
    }

    /**
     * 处理单 round, 
     */
    private void processRound(
            AgentSessionEntity session,
            String context,
            StreamCallback callback,
            boolean isFirstRound,
            long startTime) {

        int currentRound = session.getRoundCount() != null ? session.getRoundCount() : 0;

        // 检查轮次限制
        if (currentRound >= MAX_ROUNDS) {
            log.warn("Exceeded max round limit: sessionId={}, round={}", session.getConversationId(), currentRound);
            callback.onError(buildMaxRoundsError(session));
            return;
        }

        // 递增轮次并设置开始时间
        int newRound = currentRound + 1;
        sessionService.updateRoundCount(session.getConversationId(), newRound);
        sessionService.setStartTime(session.getConversationId(), System.currentTimeMillis());

        log.info("Starting round {}  round, : sessionId={}", newRound, session.getConversationId());

        // 构建执行请求
        ClaudeExecutionRequest request = ClaudeExecutionRequest.builder()
                .sessionId(session.getConversationId())
                .resume(!isFirstRound)
                .input(context)
                .build();

        // 用于收集响应
        StringBuilder responseBuffer = new StringBuilder();
        AtomicReference<String> actualSessionId = new AtomicReference<>(session.getConversationId());

        // 执行 CLI
        try {
            cliExecutor.executeStream(
                    request,
                    // chunk 回调
                    chunk -> {
                        if (chunk.getContent() != null) {
                            responseBuffer.append(chunk.getContent());
                        }
                        // 转发给调用方
                        callback.onChunk(chunk);

                        // 更新会话ID（如果是新会话）
                        if (chunk.getSessionId() != null && !chunk.getSessionId().isEmpty()) {
                            actualSessionId.set(chunk.getSessionId());
                        }
                    },
                    // 完成回调
                    () -> {
                        try {
                            // 解析响应
                            String response = responseBuffer.toString();
                            AgentPlan plan = responseParser.parse(response);

                            // 更新会话ID
                            if (!actualSessionId.get().equals(session.getConversationId())) {
                                sessionService.updateConversationId(
                                        session.getConversationId(),
                                        actualSessionId.get()
                                );
                            }

                            // 根据计划类型处理
                            handleAgentPlan(session, plan, callback, startTime);

                        } catch (ResponseParseException e) {
                            handleParseError(session, e, callback, startTime);
                        }
                    },
                    // 错误回调
                    error -> {
                        log.error("CLI execution error: {}", error.getMessage());
                        callback.onError("Execution failed: " + error.getMessage());
                    }
            );

        } catch (ClaudeCliException e) {
            log.error("CLI execution exception: sessionId={}, error={}", session.getConversationId(), e.getMessage());
            callback.onError("CLI Execution failed: " + e.getMessage());
        }
    }

    /**
     * 处理 Agent 计划
     */
    private void handleAgentPlan(
            AgentSessionEntity session,
            AgentPlan plan,
            StreamCallback callback,
            long startTime) {

        log.info("Agent plan: status={}, queries={}, actions={}",
                plan.getStatus(),
                plan.getQueries() != null ? plan.getQueries().size() : 0,
                plan.getActions() != null ? plan.getActions().size() : 0);

        switch (plan.getStatus()) {
            case AgentPlan.STATUS_QUERY:
                handleQuery(session, plan, callback, startTime);
                break;

            case AgentPlan.STATUS_ACTION:
                handleAction(session, plan, callback, startTime);
                break;

            case AgentPlan.STATUS_COMPLETE:
                handleComplete(session, plan, callback, startTime);
                break;

            default:
                log.warn("Unknown status: {}, treating as parse error", plan.getStatus());
                handleParseError(session,
                        new ResponseParseException("未知状态: " + plan.getStatus()),
                        callback, startTime);
        }
    }

    /**
     * 处理查询请求
     */
    private void handleQuery(
            AgentSessionEntity session,
            AgentPlan plan,
            StreamCallback callback,
            long startTime) {

        log.info("Executing query: sessionId={}, queryCount={}", session.getConversationId(), plan.getQueries().size());

        try {
            // 并行执行查询
            Map<String, Object> queryResults = queryExecutor.executeQueries(plan.getQueries(), session.getWorkflowId());

            // 更新会话
            sessionService.updateQueryResults(session.getConversationId(), queryResults);

            // 通知工作流更新
            callback.onWorkflowUpdate(queryResults);

            // 刷新会话数据并构建新上下文
            AgentSessionEntity updatedSession = sessionService.getByConversationId(session.getConversationId())
                    .orElse(session);

            String newContext = contextBuilder.buildInitialContext("继续处理", updatedSession);

            // 继续下一轮
            processRound(updatedSession, newContext, callback, false, startTime);

        } catch (Exception e) {
            log.error("Query execution exception: {}", e.getMessage(), e);
            callback.onError("查询Execution failed: " + e.getMessage());
        }
    }

    /**
     * 处理操作请求
     */
    private void handleAction(
            AgentSessionEntity session,
            AgentPlan plan,
            StreamCallback callback,
            long startTime) {

        log.info("Executing action: sessionId={}, actionCount={}", session.getConversationId(), plan.getActions().size());

        try {
            // 顺序执行操作（带事务）
            Map<String, Object> actionResults = actionExecutor.executeActions(plan.getActions(), session.getWorkflowId());

            // 更新会话
            sessionService.updateActionResults(session.getConversationId(), actionResults);

            // 通知工作流更新
            callback.onWorkflowUpdate(actionResults);

            // 刷新会话数据并构建新上下文
            AgentSessionEntity updatedSession = sessionService.getByConversationId(session.getConversationId())
                    .orElse(session);

            String newContext = contextBuilder.buildInitialContext("继续处理", updatedSession);

            // 继续下一轮
            processRound(updatedSession, newContext, callback, false, startTime);

        } catch (ActionExecutionException e) {
            log.error("Action execution exception: {}", e.getMessage(), e);
            callback.onError("操作Execution failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Action processing exception: {}", e.getMessage(), e);
            callback.onError("操作处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理完成
     */
    private void handleComplete(
            AgentSessionEntity session,
            AgentPlan plan,
            StreamCallback callback,
            long startTime) {

        long duration = System.currentTimeMillis() - startTime;

        log.info("Agent task completed: sessionId={}, duration={}ms",
                session.getConversationId(), duration);

        // 更新会话状态
        sessionService.markAsCompleted(session.getConversationId());

        // 如果有推理内容，更新
        if (plan.getReasoning() != null) {
            sessionService.updateLastReasoning(session.getConversationId(), plan.getReasoning());
        }

        // 发送完成回调
        callback.onDone(session.getConversationId(), duration);
    }

    /**
     * 处理解析错误
     */
    private void handleParseError(
            AgentSessionEntity session,
            ResponseParseException error,
            StreamCallback callback,
            long startTime) {

        int parseErrorCount = session.getParseErrorCount() != null ? session.getParseErrorCount() : 0;
        parseErrorCount++;
        sessionService.updateParseErrorCount(session.getConversationId(), parseErrorCount);

        if (parseErrorCount > MAX_PARSE_ERRORS) {
            log.error("Parse error count exceeded limit: sessionId={}, count={}", session.getConversationId(), parseErrorCount);
            sessionService.markAsError(session.getConversationId(), "AI 响应格式持续异常");
            callback.onError("AI 响应格式持续异常，请重试或联系管理员");
            return;
        }

        log.warn("Parse error, preparing to retry: sessionId={}, count={}/{}",
                session.getConversationId(), parseErrorCount, MAX_PARSE_ERRORS);

        // 构建错误提示上下文重试
        String errorContext = "【系统提示】\n上一次响应格式解析error: " + error.getMessage() + "\n\n" +
                "请检查你的响应格式，确保：\n" +
                "1. 返回有效的 JSON 格式\n" +
                "2. status 字段必须是 query、action 或 complete 之一\n" +
                "3. 如果是 query 或 action，请提供对应的列表\n\n" +
                "请重新组织你的响应。";

        // 刷新会话数据
        AgentSessionEntity updatedSession = sessionService.getByConversationId(session.getConversationId())
                .orElse(session);

        processRound(updatedSession, errorContext, callback, false, startTime);
    }

    /**
     * 标准化会话ID
     */
    private String normalizeSessionId(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return "new-" + UUID.randomUUID().toString();
        }
        return conversationId;
    }

    /**
     * 构建最大轮次错误
     */
    private String buildMaxRoundsError(AgentSessionEntity session) {
        return String.format(
                "任务执行超过最大轮次限制(%d轮)。\n\n" +
                        "可能原因：\n" +
                        "1. 任务过于复杂，请拆分为多个简单任务\n" +
                        "2. AI 无法理解您的请求，请尝试更明确的描述\n" +
                        "3. 系统异常，请联系管理员\n\n" +
                        "已执行轮次: %d",
                MAX_ROUNDS,
                session.getRoundCount()
        );
    }

    // ==================== 流式回调接口 ====================

    /**
     * 流式回调接口
     */
    public interface StreamCallback {

        /**
         * 会话开始回调
         *
         * @param sessionId 会话ID
         */
        default void onStart(String sessionId) {
            log.debug("Streaming session started: sessionId={}", sessionId);
        }

        /**
         * 内容块回调（实时接收 AI 输出）
         *
         * @param chunk 流式数据块
         */
        void onChunk(StreamChunk chunk);

        /**
         * 工作流更新回调（当 AI 修改工作流时触发）
         *
         * @param result 更新结果
         */
        default void onWorkflowUpdate(Object result) {
            log.debug("Workflow update: {}", result);
        }

        /**
         * 任务完成回调
         *
         * @param sessionId 会话ID
         * @param duration  执行耗时（毫秒）
         */
        default void onDone(String sessionId, Long duration) {
            log.debug("Streaming session completed: sessionId={}, duration={}ms", sessionId, duration);
        }

        /**
         * 错误回调
         *
         * @param error 错误信息
         */
        void onError(String error);
    }

    /**
     * 会话状态摘要
     */
    public record SessionSummary(
            String sessionId,
            int roundCount,
            String status,
            long durationMs,
            int queryCount,
            int actionCount
    ) {
        @Override
        public String toString() {
            return String.format(
                    "SessionSummary{sessionId='%s', rounds=%d, status='%s', duration=%dms, queries=%d, actions=%d}",
                    sessionId, roundCount, status, durationMs, queryCount, actionCount
            );
        }
    }

    /**
     * 获取会话摘要
     */
    public SessionSummary getSessionSummary(String sessionId) {
        Optional<AgentSessionEntity> sessionOpt = sessionService.getByConversationId(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        AgentSessionEntity session = sessionOpt.get();

        long duration = session.getStartTime() != null ?
                System.currentTimeMillis() - session.getStartTime() : 0;

        int queryCount = 0;
        int actionCount = 0;

        try {
            if (session.getQueryResults() != null && !session.getQueryResults().isEmpty()) {
                Map<?, ?> queries = objectMapper.readValue(session.getQueryResults(), Map.class);
                queryCount = queries.size();
            }
            if (session.getActionResults() != null && !session.getActionResults().isEmpty()) {
                Map<?, ?> actions = objectMapper.readValue(session.getActionResults(), Map.class);
                actionCount = actions.size();
            }
        } catch (Exception e) {
            log.debug("Failed to parse session result count: {}", e.getMessage());
        }

        return new SessionSummary(
                session.getConversationId(),
                session.getRoundCount() != null ? session.getRoundCount() : 0,
                session.getStatus(),
                duration,
                queryCount,
                actionCount
        );
    }
}
