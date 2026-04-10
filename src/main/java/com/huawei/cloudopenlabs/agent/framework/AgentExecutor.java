/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.framework;

import com.huawei.cloudopenlabs.agent.client.ClaudeCodeApiClient;
import com.huawei.cloudopenlabs.agent.client.ClaudeCodeStreamClient;
import com.huawei.cloudopenlabs.agent.config.AgentContextProperties;
import com.huawei.cloudopenlabs.agent.dto.AgentConfig;
import com.huawei.cloudopenlabs.agent.dto.AgentRequest;
import com.huawei.cloudopenlabs.agent.dto.AgentResponse;
import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.dto.TaskExecuteResponse;
import com.huawei.cloudopenlabs.agent.entity.AgentSessionEntity;
import com.huawei.cloudopenlabs.agent.service.AgentSessionService;
import com.huawei.cloudopenlabs.agent.service.LockService;
import com.huawei.cloudopenlabs.workflow.dto.NodeTypeResponse;
import com.huawei.cloudopenlabs.workflow.dto.WorkflowResponse;
import com.huawei.cloudopenlabs.workflow.service.NodeTypeService;
import com.huawei.cloudopenlabs.workflow.service.WorkflowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 框架统一执行器
 * <p>
 * 提供统一的 Agent 调用接口，内部封装 Claude Code API 调用逻辑
 * 支持多轮会话管理
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "claude.code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentExecutor {

    /**
     * 最大对话轮次限制
     * 防止无限递归导致栈溢出或资源耗尽
     */
    private static final int MAX_ROUNDS = 15;

    /**
     * 最大解析错误次数限制
     * 防止 AI 持续返回错误格式导致无限循环
     */
    private static final int MAX_PARSE_ERROR_ROUNDS = 3;

    /**
     * 最大执行时间（毫秒）
     * 防止任务执行时间过长导致资源耗尽
     * 默认 5 分钟
     */
    private static final long MAX_EXECUTION_TIME_MS = 5 * 60 * 1000;

    /**
     * 默认最大上下文长度（字符数）
     * 当配置未指定时使用
     */
    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 50000;

    /**
     * 默认单个结果最大长度（字符数）
     * 当配置未指定时使用
     */
    private static final int DEFAULT_MAX_RESULT_LENGTH = 5000;

    private final ClaudeCodeApiClient apiClient;
    private final ClaudeCodeStreamClient streamClient;
    private final AgentSessionService sessionService;
    private final ObjectMapper objectMapper;

    private WorkflowService workflowService;
    private NodeTypeService nodeTypeService;

    /**
     * Skill 文件路径（新会话时使用）
     */
    @Value("${agent.skill.file-path:skills/workflow-assistant.zip}")
    private String skillFilePath;

    /**
     * 构造函数
     *
     * @param apiClient      Claude Code API 客户端
     * @param streamClient   Claude Code 流式客户端
     * @param sessionService Agent 会话服务
     */
    public AgentExecutor(ClaudeCodeApiClient apiClient,
                         @org.springframework.beans.factory.annotation.Autowired(required = false) ClaudeCodeStreamClient streamClient,
                         AgentSessionService sessionService) {
        this.apiClient = apiClient;
        this.streamClient = streamClient;
        this.sessionService = sessionService;
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 日期时间模块
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性，防止 AI 返回额外字段时报错
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Autowired(required = false)
    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Autowired(required = false)
    public void setNodeTypeService(NodeTypeService nodeTypeService) {
        this.nodeTypeService = nodeTypeService;
    }

    @Autowired(required = false)
    private ContextBuilder contextBuilder;

    @Autowired
    private AgentContextProperties contextProperties;

    @Autowired(required = false)
    private LockService lockService;

    /**
     * 执行 Agent 任务（核心接口）
     *
     * @param request Agent 请求
     * @return Agent 响应
     */
    public AgentResponse execute(AgentRequest request) {
        return execute(request, null);
    }

    /**
     * 执行 Agent 任务（带回调）
     *
     * @param request Agent 请求
     * @param callback 执行回调
     * @return Agent 响应
     */
    public AgentResponse execute(AgentRequest request, AgentCallback callback) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始执行 Agent 任务: {}", request.getTaskContent());

            // 请求前回调
            if (callback != null) {
                callback.beforeExecute(request);
            }

            // 转换配置
            String configJson = request.getConfig() != null
                    ? request.getConfig().toJsonString()
                    : null;

            // 调用 Claude Code API
            TaskExecuteResponse apiResponse = apiClient.executeTask(
                    request.getTaskContent(),
                    configJson,
                    request.getSkillFileBytes(),
                    request.getSkillFileName()
            );

            long executionTime = System.currentTimeMillis() - startTime;

            // 构造响应
            AgentResponse response = AgentResponse.builder()
                    .success(apiResponse.getSuccess())
                    .response(apiResponse.getResponse())
                    .error(apiResponse.getError())
                    .errorCode(apiResponse.getCode())
                    .originalTaskContent(apiResponse.getTaskContent())
                    .executionTimeMs(executionTime)
                    .build();

            log.info("Agent 任务执行完成，耗时: {}ms，成功: {}", executionTime, response.getSuccess());

            // 请求后回调
            if (callback != null) {
                callback.afterExecute(request, response);
            }

            return response;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("Agent 任务执行异常: {}", e.getMessage(), e);

            AgentResponse response = AgentResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .errorCode(-2)
                    .originalTaskContent(request.getTaskContent())
                    .executionTimeMs(executionTime)
                    .build();

            if (callback != null) {
                callback.onError(request, response, e);
            }

            return response;
        }
    }

    /**
     * 处理用户消息（支持多轮交互）
     * 这个方法会被 ChatServiceImpl 调用
     *
     * @param userMessage     用户消息
     * @param workflowId      工作流ID
     * @param conversationId  会话ID（即 Claude CLI session ID）
     * @param callback        多轮会话回调
     */
    public void processMessage(String userMessage, String workflowId, String conversationId,
                              MultiRoundCallback callback) {
        try {
            log.info("开始处理多轮消息: conversationId={}, workflowId={}", conversationId, workflowId);

            // 判断是否是新会话（原始 conversationId 为空）
            boolean isNewSession = conversationId == null || conversationId.isEmpty();

            // 1. 获取或创建 session
            AgentSessionEntity session = sessionService.getOrCreateSession(workflowId, conversationId);

            // 2. 构建完整上下文
            String context = buildContext(userMessage, session);

            // 3. 开始多轮循环（传入是否新会话的标志）
            processRound(session, context, callback, isNewSession);

        } catch (Exception e) {
            log.error("处理多轮消息异常: {}", e.getMessage(), e);
            callback.onError("处理消息异常: " + e.getMessage());
        }
    }

    /**
     * 处理单轮交互
     *
     * @param session       会话实体
     * @param context       上下文内容
     * @param callback      回调
     * @param isNewSession  是否是新会话（原始 conversationId 为空）
     */
    private void processRound(AgentSessionEntity session, String context,
                              MultiRoundCallback callback, boolean isNewSession) {
        // 递增轮次计数
        int currentRound = incrementRoundCount(session);

        // 检查最大轮次限制
        if (currentRound > MAX_ROUNDS) {
            log.warn("超过最大轮次限制: currentRound={}, maxRounds={}", currentRound, MAX_ROUNDS);
            callback.onError(buildMaxRoundsError(session));
            sessionService.markAsError(session.getConversationId(), "超过最大轮次限制");
            return;
        }

        // 检查执行超时
        if (isTimeout(session)) {
            log.warn("执行超时: conversationId={}, elapsed={}ms, maxTime={}ms",
                    session.getConversationId(), getElapsedTime(session), MAX_EXECUTION_TIME_MS);
            callback.onError(buildTimeoutError(session));
            sessionService.markAsError(session.getConversationId(), "执行超时");
            return;
        }

        log.info("开始第 {} 轮对话, conversationId={}, elapsed={}ms",
                currentRound, session.getConversationId(), getElapsedTime(session));

        try {
            // 新会话需要传入 skillFile 和不传 sessionId，已有会话则传 sessionId 恢复
            byte[] skillFileBytes = null;
            String skillFileName = null;
            String sessionIdForApi = null;

            if (isNewSession) {
                log.info("新会话，加载 Skill 文件: {}", skillFilePath);
                skillFileBytes = loadSkillFile();
                skillFileName = "workflow-assistant.zip";
                sessionIdForApi = null;  // 新会话不传 sessionId，让 Claude CLI 生成
            } else {
                log.info("恢复会话，sessionId: {}", session.getConversationId());
                skillFileBytes = null;  // 已有会话不传 Skill 文件
                skillFileName = null;
                sessionIdForApi = session.getConversationId();  // 传 sessionId 恢复会话
            }

            // 调用 Claude Code API
            TaskExecuteResponse apiResponse = apiClient.executeTask(
                    context,
                    null,  // config
                    skillFileBytes,   // skillFile（新会话时传入）
                    skillFileName,    // skillFileName
                    sessionIdForApi   // sessionId（新会话时为 null，已有会话时传入）
            );

            if (!apiResponse.getSuccess()) {
                sessionService.markAsError(session.getConversationId(), apiResponse.getError());
                callback.onError("API调用失败: " + apiResponse.getError());
                return;
            }

            // 检查是否返回了新的 sessionId（首次调用时）
            if (apiResponse.getSessionId() != null && !apiResponse.getSessionId().equals(session.getConversationId())) {
                String oldConversationId = session.getConversationId();
                log.info("收到新的 sessionId: {}", apiResponse.getSessionId());
                // 更新数据库中的 session 的 conversationId
                session = sessionService.updateConversationId(oldConversationId, apiResponse.getSessionId());
                if (session == null) {
                    callback.onError("更新会话ID失败");
                    return;
                }
                // 通知回调
                callback.onSessionCreated(apiResponse.getSessionId());
            }

            // 解析 AI 响应
            AgentPlan plan = parseAgentPlan(apiResponse.getResponse());

            // 更新最后推理内容
            sessionService.updateLastReasoning(session.getConversationId(), plan.getReasoning());

            // 通知前端 AI 的思考过程
            callback.onReasoning(plan.getReasoning());

            switch (plan.getStatus()) {
                case "query":
                    // AI 需要查询信息
                    handleQuery(session, plan, callback);
                    break;

                case "action":
                    // AI 要执行操作
                    handleAction(session, plan, callback);
                    break;

                case "complete":
                    // 任务完成 - 只有 status 为 complete 时才标记会话为完成
                    callback.onComplete(plan.getSummary(), plan.getResult());
                    sessionService.markAsCompleted(session.getConversationId());
                    break;

                case "parse_error":
                    // 解析错误，将问题反馈给 Claude Code 让其重新生成
                    log.warn("AI 响应格式解析错误，将问题反馈给 Claude Code: {}", plan.getSummary());

                    // 递增解析错误计数
                    int parseErrorCount = incrementParseErrorCount(session);

                    // 检查解析错误次数限制
                    if (parseErrorCount > MAX_PARSE_ERROR_ROUNDS) {
                        log.warn("超过最大解析错误次数限制: parseErrorCount={}, maxParseErrorRounds={}",
                                parseErrorCount, MAX_PARSE_ERROR_ROUNDS);
                        callback.onError("AI 响应格式持续异常，已尝试 " + MAX_PARSE_ERROR_ROUNDS +
                                " 次仍无法解析。请尝试简化您的请求或稍后重试。");
                        sessionService.markAsError(session.getConversationId(), "超过最大解析错误次数限制");
                        return;
                    }

                    callback.onStatus("响应格式有误，正在请求重新生成... (尝试 " + parseErrorCount + "/" + MAX_PARSE_ERROR_ROUNDS + ")");
                    String errorContext = buildErrorContext(session, "响应格式解析错误", plan.getSummary());
                    processRound(session, errorContext, callback, false);
                    break;

                default:
                    // 未知的 status，将问题反馈给 Claude Code
                    log.warn("未知的计划状态: {}，将问题反馈给 Claude Code", plan.getStatus());
                    String unknownStatusContext = buildErrorContext(session,
                            "status 字段值无效",
                            "status 只能是 'query'、'action' 或 'complete'，但收到了: " + plan.getStatus());
                    processRound(session, unknownStatusContext, callback, false);
            }
        } catch (Exception e) {
            log.error("处理单轮交互异常: {}", e.getMessage(), e);
            // 不直接标记为错误，而是将异常信息反馈给 Claude Code 让其处理
            String exceptionContext = buildErrorContext(session, "处理异常", e.getMessage());
            processRound(session, exceptionContext, callback, false);
        }
    }

    /**
     * 处理查询请求
     */
    private void handleQuery(AgentSessionEntity session, AgentPlan plan,
                             MultiRoundCallback callback) {
        Map<String, Object> queryResults = new HashMap<>();

        List<AgentPlan.Query> queries = plan.getQueries();
        if (queries == null || queries.isEmpty()) {
            log.warn("查询请求为空，继续下一轮");
            String newContext = buildContextWithResults(session, queryResults, "query");
            processRound(session, newContext, callback, false);  // 后续轮次不是新会话
            return;
        }

        // 执行所有查询
        for (AgentPlan.Query query : queries) {
            callback.onStatus("正在查询: " + query.getDescription());
            log.info("执行查询: id={}, description={}", query.getId(), query.getDescription());

            Object result = executeQuery(query.getMethod(), query.getPath(), query.getParams());
            queryResults.put(query.getId(), result);
        }

        // 更新 session
        sessionService.updateQueryResults(session.getConversationId(), queryResults);

        // 继续下一轮
        String newContext = buildContextWithResults(session, queryResults, "query");
        processRound(session, newContext, callback, false);  // 后续轮次不是新会话
    }

    /**
     * 处理操作请求
     */
    private void handleAction(AgentSessionEntity session, AgentPlan plan,
                              MultiRoundCallback callback) {
        Map<String, Object> actionResults = new HashMap<>();

        List<AgentPlan.Action> actions = plan.getActions();
        if (actions == null || actions.isEmpty()) {
            log.warn("操作请求为空，继续下一轮");
            String newContext = buildContextWithResults(session, actionResults, "action");
            processRound(session, newContext, callback, false);  // 后续轮次不是新会话
            return;
        }

        // 执行所有操作
        for (AgentPlan.Action action : actions) {
            callback.onStatus("正在执行: " + action.getDescription());
            log.info("执行操作: id={}, method={}, path={}, description={}",
                    action.getId(), action.getMethod(), action.getPath(), action.getDescription());

            Object result = executeAction(action.getMethod(), action.getPath(), action.getBody());
            actionResults.put(action.getId(), result);

            // 如果是更新节点配置，通知前端
            if (action.getPath() != null && action.getPath().contains("/data/json")) {
                callback.onWorkflowUpdate(result);
            }
        }

        // 更新 session
        sessionService.updateActionResults(session.getConversationId(), actionResults);

        // 继续下一轮
        String newContext = buildContextWithResults(session, actionResults, "action");
        processRound(session, newContext, callback, false);  // 后续轮次不是新会话
    }

    /**
     * 执行查询操作
     * 根据 path 调用相应的服务方法获取数据
     */
    private Object executeQuery(String method, String path, Map<String, Object> params) {
        log.info("执行查询: method={}, path={}", method, path);
        Map<String, Object> result = new HashMap<>();

        try {
            // 解析 path，例如: /api/workflow/1 或 /api/workflow/node-types
            Object data = null;

            if (path == null || path.isEmpty()) {
                result.put("success", false);
                result.put("error", "路径为空");
                return result;
            }

            // 匹配 /api/workflow/{id} - 获取工作流详情
            if (path.matches("/api/workflow/[0-9a-fA-F\\-]+")) {
                if (workflowService != null) {
                    String workflowId = path.substring(path.lastIndexOf('/') + 1);
                    log.info("查询工作流详情: id={}", workflowId);
                    WorkflowResponse workflow = workflowService.getWorkflowById(workflowId);
                    data = workflow;
                } else {
                    log.warn("WorkflowService 未注入");
                }
            }
            // 匹配 /api/workflow/node-types - 获取节点类型列表
            else if (path.equals("/api/workflow/node-types")) {
                if (nodeTypeService != null) {
                    log.info("查询所有节点类型");
                    List<NodeTypeResponse> nodeTypes = nodeTypeService.getAllEnabledNodeTypes();
                    data = nodeTypes;
                } else {
                    log.warn("NodeTypeService 未注入");
                }
            }
            // 匹配 /api/workflow/list - 获取工作流列表
            else if (path.equals("/api/workflow/list")) {
                if (workflowService != null) {
                    log.info("查询工作流列表");
                    Pageable pageable = PageRequest.of(0, 100);
                    Page<WorkflowResponse> workflows = workflowService.getWorkflowList(pageable);
                    data = workflows.getContent();
                } else {
                    log.warn("WorkflowService 未注入");
                }
            }
            // 其他路径
            else {
                log.warn("未知的查询路径: {}", path);
                result.put("success", false);
                result.put("error", "未知的查询路径: " + path);
                return result;
            }

            result.put("success", true);
            result.put("method", method);
            result.put("path", path);
            result.put("data", data);
            result.put("message", "查询成功");

        } catch (Exception e) {
            log.error("查询执行异常: path={}, error={}", path, e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 执行操作（真正调用后端服务）
     */
    private Object executeAction(String method, String path, Map<String, Object> body) {
        log.info("执行操作: method={}, path={}", method, path);
        Map<String, Object> result = new HashMap<>();

        try {
            if (path == null || path.isEmpty()) {
                result.put("success", false);
                result.put("error", "路径为空");
                return result;
            }

            // 匹配 POST /api/workflow/{id}/data/json - 保存工作流数据
            if ("POST".equalsIgnoreCase(method) && path.matches("/api/workflow/[0-9a-fA-F\\-]+/data/json")) {
                if (workflowService != null && body != null) {
                    String workflowId = path.split("/")[3];
                    log.info("保存工作流数据: workflowId={}", workflowId);

                    // 将 Map 转换为 DTO 列表
                    List<WorkflowResponse.NodeDTO> nodes = null;
                    List<WorkflowResponse.ConnectionDTO> connections = null;
                    List<WorkflowResponse.AssociationDTO> associations = null;

                    Object nodesObj = body.get("nodes");
                    if (nodesObj instanceof List) {
                        nodes = objectMapper.convertValue(nodesObj,
                                new TypeReference<List<WorkflowResponse.NodeDTO>>() {});
                    }

                    Object connectionsObj = body.get("connections");
                    if (connectionsObj instanceof List) {
                        connections = objectMapper.convertValue(connectionsObj,
                                new TypeReference<List<WorkflowResponse.ConnectionDTO>>() {});
                    }

                    Object associationsObj = body.get("associations");
                    if (associationsObj instanceof List) {
                        associations = objectMapper.convertValue(associationsObj,
                                new TypeReference<List<WorkflowResponse.AssociationDTO>>() {});
                    }

                    // 调用 workflowService 保存数据
                    WorkflowResponse response = workflowService.saveWorkflowData(workflowId, nodes, connections, associations);

                    result.put("success", true);
                    result.put("method", method);
                    result.put("path", path);
                    result.put("workflowId", workflowId);
                    result.put("updatedNodes", nodes != null ? nodes.size() : 0);
                    result.put("data", response);
                    result.put("message", "工作流数据保存成功");
                    log.info("工作流数据保存成功: workflowId={}, nodes={}", workflowId, nodes != null ? nodes.size() : 0);
                } else {
                    log.warn("WorkflowService 未注入或 body 为空");
                    result.put("success", false);
                    result.put("error", "WorkflowService 未注入或 body 为空");
                }
            }
            // 匹配 POST /api/workflow/{id}/publish - 发布工作流
            else if ("POST".equalsIgnoreCase(method) && path.matches("/api/workflow/[0-9a-fA-F\\-]+/publish")) {
                if (workflowService != null) {
                    String workflowId = path.split("/")[3];
                    log.info("发布工作流: workflowId={}", workflowId);
                    workflowService.publishWorkflow(workflowId);
                    result.put("success", true);
                    result.put("method", method);
                    result.put("path", path);
                    result.put("workflowId", workflowId);
                    result.put("message", "工作流发布成功");
                } else {
                    log.warn("WorkflowService 未注入");
                    result.put("success", false);
                    result.put("error", "WorkflowService 未注入");
                }
            }
            // 匹配 POST /api/workflow/{id}/execute - 执行工作流
            else if ("POST".equalsIgnoreCase(method) && path.matches("/api/workflow/[0-9a-fA-F\\-]+/execute")) {
                if (workflowService != null) {
                    String workflowId = path.split("/")[3];
                    log.info("执行工作流: workflowId={}", workflowId);
                    // TODO: 调用 workflowService 执行工作流
                    result.put("success", true);
                    result.put("method", method);
                    result.put("path", path);
                    result.put("workflowId", workflowId);
                    result.put("executionId", "exec-" + System.currentTimeMillis());
                    result.put("message", "工作流开始执行");
                } else {
                    log.warn("WorkflowService 未注入");
                    result.put("success", false);
                    result.put("error", "WorkflowService 未注入");
                }
            }
            // 其他操作
            else {
                log.warn("未知的操作路径: method={}, path={}", method, path);
                result.put("success", false);
                result.put("error", "未知的操作路径: " + method + " " + path);
            }

        } catch (Exception e) {
            log.error("操作执行异常: method={}, path={}, error={}", method, path, e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 构建上下文
     */
    private String buildContext(String userMessage, AgentSessionEntity session) {
        StringBuilder sb = new StringBuilder();

        // 强调使用中文回复
        sb.append("【重要】必须按要求格式输出，请务必使用中文进行所有回复和输出，包括 reasoning、summary 等字段内容。\n\n");

        // 强调必须遵循 Skill 指令
        sb.append("【重要】你必须严格遵循添加目录中的 skill.md 文件定义的指令和输出格式。\n");
        sb.append("禁止跳过 Skill 指令直接探索当前项目代码。禁止使用内置的 mysql-connect 等技能绕过 API。\n");
        sb.append("所有数据查询必须通过 Skill 中定义的 HTTP API 接口进行。\n\n");

        sb.append("用户请求: ").append(userMessage).append("\n\n");
        sb.append("workflowId: ").append(session.getWorkflowId()).append("\n\n");

        // 添加之前的查询结果（带截断）
        if (session.getQueryResults() != null && !session.getQueryResults().equals("{}")) {
            String truncatedQueryResults = truncateContent(session.getQueryResults(), getMaxResultLength());
            sb.append("之前的查询结果:\n").append(truncatedQueryResults).append("\n\n");
        }

        // 添加之前的操作结果（带截断）
        if (session.getActionResults() != null && !session.getActionResults().equals("{}")) {
            String truncatedActionResults = truncateContent(session.getActionResults(), getMaxResultLength());
            sb.append("之前的操作结果:\n").append(truncatedActionResults).append("\n\n");
        }

        // 添加当前轮次信息
        int roundCount = session.getRoundCount() != null ? session.getRoundCount() : 0;
        sb.append("当前轮次: ").append(roundCount + 1).append("\n");

        // 最终长���检查和截断
        String context = sb.toString();
        if (context.length() > getMaxContextLength()) {
            context = emergencyTruncate(context, getMaxContextLength());
            log.info("上下文已截断: 原长度={}, 截断后长度={}", sb.length(), context.length());
        }

        return context;
    }

    /**
     * 构建带结果的上下文
     */
    private String buildContextWithResults(AgentSessionEntity session, Map<String, Object> newResults, String resultType) {
        StringBuilder sb = new StringBuilder();

        // 强调使用中文回复
        sb.append("【重要】请务必使用中文进行所有回复和输出。\n");
        // 强调必须遵循 Skill 指令
        sb.append("【重要】你必须严格遵循 skill.md 文件定义的指令和输出格式（query/action/complete）。\n\n");

        sb.append("本轮").append(resultType.equals("query") ? "查询" : "操作").append("结果:\n");
        try {
            String resultsJson = objectMapper.writeValueAsString(newResults);
            // 截断本轮结果
            resultsJson = truncateContent(resultsJson, getMaxResultLength());
            sb.append(resultsJson).append("\n\n");
        } catch (JsonProcessingException e) {
            sb.append("结果序列化失败\n\n");
        }

        // 重新获取最新的 session 数据（带截断）
        if (session.getQueryResults() != null && !session.getQueryResults().equals("{}")) {
            String truncatedQueryResults = truncateContent(session.getQueryResults(), getMaxResultLength());
            sb.append("累计查询结果:\n").append(truncatedQueryResults).append("\n\n");
        }

        if (session.getActionResults() != null && !session.getActionResults().equals("{}")) {
            String truncatedActionResults = truncateContent(session.getActionResults(), getMaxResultLength());
            sb.append("累计操作结果:\n").append(truncatedActionResults).append("\n\n");
        }

        // 最终长度检查和截断
        String context = sb.toString();
        if (context.length() > getMaxContextLength()) {
            context = emergencyTruncate(context, getMaxContextLength());
            log.info("带结果上下文已截断: 原长度={}, 截断后长度={}", sb.length(), context.length());
        }

        return context;
    }

    /**
     * 构建错误上下文，用于将问题反馈给 Claude Code
     *
     * @param session     会话实体
     * @param errorType   错误类型
     * @param errorDetail 错误详情
     * @return 错误上下文字符串
     */
    private String buildErrorContext(AgentSessionEntity session, String errorType, String errorDetail) {
        StringBuilder sb = new StringBuilder();

        // 强调使用中文回复
        sb.append("【重要】必须按要求格式输出，请务必使用中文进行所有回复和输出，包括 reasoning、summary 等字段内容。\n\n");

        sb.append("【错误反馈】\n\n");
        sb.append("你的上一轮响应存在问题，请根据以下信息修正后重新生成响应：\n\n");
        sb.append("错误类型: ").append(errorType).append("\n");
        sb.append("错误详情: ").append(errorDetail).append("\n\n");

        sb.append("【重要提醒】\n");
        sb.append("1. status 字段只能是以下三种值之一：'query'、'action'、'complete'\n");
        sb.append("2. 必须返回有效的 JSON 格式\n");
        sb.append("3. status 为 'query' 时必须包含 queries 数组\n");
        sb.append("4. status 为 'action' 时必须包含 actions 数组\n");
        sb.append("5. status 为 'complete' 时表示任务完成\n\n");

        sb.append("workflowId: ").append(session.getWorkflowId()).append("\n\n");

        // 添加之前的查询和操作结果，帮助 AI 理解上下文
        if (session.getQueryResults() != null && !session.getQueryResults().equals("{}")) {
            sb.append("之前的查询结果:\n").append(session.getQueryResults()).append("\n\n");
        }

        if (session.getActionResults() != null && !session.getActionResults().equals("{}")) {
            sb.append("之前的操作结果:\n").append(session.getActionResults()).append("\n\n");
        }

        sb.append("请使用中文重新生成符合格式要求的响应。");

        return sb.toString();
    }

    /**
     * 递增轮次计数
     *
     * @param session 会话实体
     * @return 当前轮次数
     */
    private int incrementRoundCount(AgentSessionEntity session) {
        int currentRound = (session.getRoundCount() != null ? session.getRoundCount() : 0) + 1;
        session.setRoundCount(currentRound);

        // 异步更新数据库
        try {
            sessionService.updateRoundCount(session.getConversationId(), currentRound);
        } catch (Exception e) {
            log.warn("更新轮次计数失败: {}", e.getMessage());
        }

        return currentRound;
    }

    /**
     * 递增解析错误计数
     *
     * @param session 会话实体
     * @return 当前解析错误次数
     */
    private int incrementParseErrorCount(AgentSessionEntity session) {
        int currentCount = (session.getParseErrorCount() != null ? session.getParseErrorCount() : 0) + 1;
        session.setParseErrorCount(currentCount);

        // 异步更新数据库
        try {
            sessionService.updateParseErrorCount(session.getConversationId(), currentCount);
        } catch (Exception e) {
            log.warn("更新解析错误计数失败: {}", e.getMessage());
        }

        return currentCount;
    }

    /**
     * 构建最大轮次错误消息
     *
     * @param session 会话实体
     * @return 友好的错误消息
     */
    private String buildMaxRoundsError(AgentSessionEntity session) {
        int executedRounds = session.getRoundCount() != null ? session.getRoundCount() : 0;
        int queryCount = session.getQueryResults() != null && !session.getQueryResults().equals("{}") ? 1 : 0;
        int actionCount = session.getActionResults() != null && !session.getActionResults().equals("{}") ? 1 : 0;

        return String.format(
                "任务执行超过最大轮次限制（%d 轮）。\n\n" +
                "可能原因：\n" +
                "1. 任务过于复杂，建议拆分为多个简单任务分别执行\n" +
                "2. 请求描述不够明确，请尝试更清晰地描述您的需求\n" +
                "3. 系统暂时繁忙，请稍后重试\n\n" +
                "执行统计：已执行 %d 轮对话",
                MAX_ROUNDS,
                executedRounds
        );
    }

    /**
     * 检查是否超时
     *
     * @param session 会话实体
     * @return true 表示已超时
     */
    private boolean isTimeout(AgentSessionEntity session) {
        Long startTime = session.getStartTime();
        if (startTime == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        boolean timeout = elapsed > MAX_EXECUTION_TIME_MS;
        if (timeout) {
            log.warn("执行超时: elapsed={}ms, maxTime={}ms", elapsed, MAX_EXECUTION_TIME_MS);
        }
        return timeout;
    }

    /**
     * 获取已执行时间（毫秒）
     *
     * @param session 会话实体
     * @return 已执行时间
     */
    private long getElapsedTime(AgentSessionEntity session) {
        Long startTime = session.getStartTime();
        if (startTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 构建超时错误消息
     *
     * @param session 会话实体
     * @return 友好的错误消息
     */
    private String buildTimeoutError(AgentSessionEntity session) {
        long elapsedSeconds = getElapsedTime(session) / 1000;
        int executedRounds = session.getRoundCount() != null ? session.getRoundCount() : 0;

        return String.format(
                "任务执行超时（已执行 %d 秒，超过 %d 秒限制）。\n\n" +
                "可能原因：\n" +
                "1. 任务过于复杂，建议拆分为多个简单任务分别执行\n" +
                "2. AI 正在处理大量数据，请耐心等待或简化请求\n" +
                "3. 系统暂时繁忙，请稍后重试\n\n" +
                "执行统计：已执行 %d 轮对话，耗时 %d 秒",
                elapsedSeconds,
                MAX_EXECUTION_TIME_MS / 1000,
                executedRounds,
                elapsedSeconds
        );
    }

    /**
     * 截断内容字符串
     *
     * @param content   原始内容
     * @param maxLength 最大长度
     * @return 截断后的内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        log.debug("内容已截断: 原长度={}, 最大长度={}", content.length(), maxLength);
        return content.substring(0, maxLength) + "...(内容已截断)";
    }

    /**
     * 紧急截断（保留开头和结尾）
     * 当上下文整体超过限制时使用
     *
     * @param context   上下文内容
     * @param maxLength 最大长度
     * @return 截断后的上下文
     */
    private String emergencyTruncate(String context, int maxLength) {
        if (context.length() <= maxLength) {
            return context;
        }

        // 保留开头 40% 和结尾 40%，中间用省略号代替
        int headLength = (int) (maxLength * 0.4);
        int tailLength = (int) (maxLength * 0.4);
        String separator = "\n\n...(中间内容已省略以节省 Token)...\n\n";

        return context.substring(0, headLength) +
                separator +
                context.substring(context.length() - tailLength);
    }

    /**
     * 获取最大上下文长度（从配置）
     *
     * @return 最大上下文长度
     */
    private int getMaxContextLength() {
        if (contextProperties != null && contextProperties.getMaxLength() > 0) {
            return contextProperties.getMaxLength();
        }
        return DEFAULT_MAX_CONTEXT_LENGTH;
    }

    /**
     * 获取最大结果长度（从配置）
     *
     * @return 最大结果长度
     */
    private int getMaxResultLength() {
        if (contextProperties != null && contextProperties.getMaxResultLength() > 0) {
            return contextProperties.getMaxResultLength();
        }
        return DEFAULT_MAX_RESULT_LENGTH;
    }

    /**
     * 检查是否启用截断
     *
     * @return 是否启用截断
     */
    private boolean isTruncationEnabled() {
        if (contextProperties != null) {
            return contextProperties.isTruncationEnabled();
        }
        return true; // 默认启用
    }

    /**
     * 解析 AI 响应为 AgentPlan
     */
    private AgentPlan parseAgentPlan(String response) {
        AgentPlan plan = new AgentPlan();

        if (response == null || response.isBlank()) {
            // 响应为空，返回解析错误，让 Claude Code 重新生成
            plan.setStatus("parse_error");
            plan.setSummary("AI 响应为空，请检查并重新生成有效的 JSON 格式响应。");
            return plan;
        }

        // 尝试提取 JSON 内容（可能被 markdown 代码块包裹）
        String jsonContent = extractJsonContent(response);

        if (jsonContent != null) {
            try {
                Map<String, Object> responseMap = objectMapper.readValue(jsonContent,
                        new TypeReference<Map<String, Object>>() {});

                String status = (String) responseMap.get("status");

                // 验证 status 是否是有效的值
                if (status == null || (!status.equals("query") && !status.equals("action") && !status.equals("complete"))) {
                    log.warn("status 字段无效或缺失: {}", status);
                    plan.setStatus("parse_error");
                    plan.setSummary("响应中的 status 字段无效。status 只能是 'query'、'action' 或 'complete'。当前值: " + status + "。请修正后重新生成。");
                    return plan;
                }

                plan.setStatus(status);
                plan.setReasoning((String) responseMap.get("reasoning"));
                plan.setSummary((String) responseMap.get("summary"));
                plan.setResult(responseMap.get("result"));

                // 解析 queries（支持单数 query 和复数 queries）
                Object queriesObj = responseMap.get("queries");
                if (queriesObj instanceof List) {
                    plan.setQueries(objectMapper.convertValue(queriesObj,
                            new TypeReference<List<AgentPlan.Query>>() {}));
                } else {
                    // 尝试单数形式 query
                    Object queryObj = responseMap.get("query");
                    if (queryObj instanceof Map) {
                        AgentPlan.Query singleQuery = objectMapper.convertValue(queryObj,
                                AgentPlan.Query.class);
                        plan.setQueries(List.of(singleQuery));
                    }
                }

                // 解析 actions（支持单数 action 和复数 actions）
                Object actionsObj = responseMap.get("actions");
                if (actionsObj instanceof List) {
                    plan.setActions(objectMapper.convertValue(actionsObj,
                            new TypeReference<List<AgentPlan.Action>>() {}));
                } else {
                    // 尝试单数形式 action
                    Object actionObj = responseMap.get("action");
                    if (actionObj instanceof Map) {
                        AgentPlan.Action singleAction = objectMapper.convertValue(actionObj,
                                AgentPlan.Action.class);
                        plan.setActions(List.of(singleAction));
                    }
                }

                // 额外验证：query 状态必须有 queries，action 状态必须有 actions
                if ("query".equals(status) && (plan.getQueries() == null || plan.getQueries().isEmpty())) {
                    log.warn("query 状态但 queries 为空");
                    plan.setStatus("parse_error");
                    plan.setSummary("status 为 'query' 时必须包含 'queries' 数组。请添加至少一个查询请求。");
                    return plan;
                }

                if ("action".equals(status) && (plan.getActions() == null || plan.getActions().isEmpty())) {
                    log.warn("action 状态但 actions 为空");
                    plan.setStatus("parse_error");
                    plan.setSummary("status 为 'action' 时必须包含 'actions' 数组。请添加至少一个操作请求。");
                    return plan;
                }

                log.info("解析 AgentPlan 成功: status={}, queries={}, actions={}",
                        plan.getStatus(),
                        plan.getQueries() != null ? plan.getQueries().size() : 0,
                        plan.getActions() != null ? plan.getActions().size() : 0);
                return plan;
            } catch (JsonProcessingException e) {
                log.warn("解析 AI 响应为 JSON 失败: {}", e.getMessage());
                // 返回解析错误，让 Claude Code 重新生成
                plan.setStatus("parse_error");
                plan.setSummary("响应格式解析失败: " + e.getMessage() + "。请确保返回有效的 JSON 格式，包含 status、reasoning 等必要字段。原始响应: " +
                        (response.length() > 500 ? response.substring(0, 500) + "..." : response));
                return plan;
            }
        }

        // 非 JSON 格式，返回解析错误
        log.warn("响应不是有效的 JSON 格式: {}", response.substring(0, Math.min(200, response.length())));
        plan.setStatus("parse_error");
        plan.setSummary("响应不是有效的 JSON 格式。请返回标准的 JSON 格式响应，包含 status、reasoning、queries/actions 等字段。原始响应: " +
                (response.length() > 500 ? response.substring(0, 500) + "..." : response));
        return plan;
    }

    /**
     * 从响应中提取 JSON 内容
     * 支持纯 JSON 和 markdown 代码块包裹的 JSON
     */
    private String extractJsonContent(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String trimmed = response.trim();

        // 如果是纯 JSON，直接返回
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            log.debug("响应是纯 JSON，直接返回");
            return trimmed;
        }

        // 尝试提取 markdown 代码块中的 JSON
        // 匹配 ```json ... ``` 或 ``` ... ```
        // 使用更可靠的方法：找到代码块开始和结束标记
        int codeBlockStart = response.indexOf("```");
        if (codeBlockStart != -1) {
            // 跳过 ``` 和可能的 json 标记
            int jsonStart = response.indexOf('{', codeBlockStart);
            if (jsonStart != -1) {
                // 找到代码块结束标记
                // 从 jsonStart 之后开始找 ```，但要确保是独立的代码块结束标记
                int searchStart = jsonStart + 1;
                int codeBlockEnd = -1;

                // 查找结束的 ```，需要确保它不是在字符串内部
                while (searchStart < response.length()) {
                    int possibleEnd = response.indexOf("```", searchStart);
                    if (possibleEnd == -1) {
                        break;
                    }
                    // 检查这个 ``` 前面是否有换行（正常的代码块结束标记）
                    if (possibleEnd > 0 && (response.charAt(possibleEnd - 1) == '\n' || response.charAt(possibleEnd - 1) == '\r')) {
                        codeBlockEnd = possibleEnd;
                        break;
                    }
                    searchStart = possibleEnd + 1;
                }

                if (codeBlockEnd != -1) {
                    // 提取 JSON 内容（到代码块结束前的最后一个 }）
                    String content = response.substring(jsonStart, codeBlockEnd);
                    // 找到最后一个 }
                    int lastBrace = content.lastIndexOf('}');
                    if (lastBrace != -1) {
                        String json = content.substring(0, lastBrace + 1);
                        log.debug("从 markdown 代码块中提取到 JSON，长度: {}", json.length());
                        log.debug("JSON 前 200 字符: {}", json.substring(0, Math.min(200, json.length())));
                        return json;
                    }
                }
            }
        }

        // 回退方案：尝试查找第一个 { 和最后一个 }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            String json = response.substring(start, end + 1);
            log.debug("使用回退方案提取 JSON，长度: {}", json.length());
            return json;
        }

        return null;
    }

    /**
     * 简化接口：仅执行任务
     *
     * @param taskContent 任务内容
     * @return Agent 响应
     */
    public AgentResponse executeSimple(String taskContent) {
        return execute(AgentRequest.builder()
                .taskContent(taskContent)
                .build());
    }

    /**
     * 简化接口：执行任务并配置
     *
     * @param taskContent 任务内容
     * @param timeout     超时时间（秒）
     * @param debug       是否开启调试
     * @return Agent 响应
     */
    public AgentResponse executeWithConfig(String taskContent, Integer timeout, Boolean debug) {
        AgentConfig config = AgentConfig.builder()
                .timeout(timeout)
                .debug(debug)
                .build();
        return execute(AgentRequest.builder()
                .taskContent(taskContent)
                .config(config)
                .build());
    }

    /**
     * 异步执行 Agent 任务
     *
     * @param request Agent 请求
     * @param callback 执行回调
     */
    public void executeAsync(AgentRequest request, AgentCallback callback) {
        new Thread(() -> execute(request, callback)).start();
    }

    /**
     * 检查 Claude Code 服务健康状态
     *
     * @return true 表示服务正常，false 表示服务异常
     */
    public boolean checkHealth() {
        try {
            apiClient.healthCheck();
            return true;
        } catch (Exception e) {
            log.error("Claude Code 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Agent 执行回调接口
     */
    public interface AgentCallback {

        /**
         * 执行前回调（可选）
         *
         * @param request Agent 请求
         */
        default void beforeExecute(AgentRequest request) {
            log.debug("Agent 任务即将执行: {}", request.getTaskContent());
        }

        /**
         * 执行后回调（可选）
         *
         * @param request  Agent 请求
         * @param response Agent 响应
         */
        default void afterExecute(AgentRequest request, AgentResponse response) {
            log.debug("Agent 任务执行完成，成功: {}", response.getSuccess());
        }

        /**
         * 异常回调（可选）
         *
         * @param request  Agent 请求
         * @param response Agent 响应
         * @param e        异常
         */
        default void onError(AgentRequest request, AgentResponse response, Exception e) {
            log.error("Agent 任务执行异常: {}", e.getMessage());
        }
    }

    /**
     * 加载 Skill 文件
     *
     * @return Skill 文件字节数组
     */
    private byte[] loadSkillFile() {
        try {
            Path path = Paths.get(skillFilePath);

            // 如果是相对路径，尝试从工作目录查找
            if (!path.isAbsolute()) {
                // 尝试从当前工作目录
                Path cwdPath = Paths.get(System.getProperty("user.dir"), skillFilePath);
                if (Files.exists(cwdPath)) {
                    path = cwdPath;
                } else {
                    // 尝试从 classpath
                    String classpathSkillPath = Paths.get("src/main/resources", skillFilePath).toString();
                    Path resourcePath = Paths.get(System.getProperty("user.dir"), classpathSkillPath);
                    if (Files.exists(resourcePath)) {
                        path = resourcePath;
                    }
                }
            }

            if (!Files.exists(path)) {
                log.warn("Skill 文件不存在: {}", path.toAbsolutePath());
                return null;
            }

            byte[] bytes = Files.readAllBytes(path);
            log.info("成功加载 Skill 文件: {}, 大小: {} bytes", path.toAbsolutePath(), bytes.length);
            return bytes;

        } catch (IOException e) {
            log.error("加载 Skill 文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 多轮会话回调接口
     */
    public interface MultiRoundCallback {

        /**
         * 会话创建回调（首次调用时返回 sessionId）
         *
         * @param sessionId Claude CLI 返回的会话ID
         */
        default void onSessionCreated(String sessionId) {
            log.debug("会话创建: sessionId={}", sessionId);
        }

        /**
         * AI 推理过程回调
         *
         * @param reasoning 推理内容
         */
        void onReasoning(String reasoning);

        /**
         * 状态更新回调
         *
         * @param status 状态信息
         */
        void onStatus(String status);

        /**
         * 工作流更新回调
         *
         * @param result 更新结果
         */
        void onWorkflowUpdate(Object result);

        /**
         * 任务完成回调
         *
         * @param summary 摘要
         * @param result  结果
         */
        void onComplete(String summary, Object result);

        /**
         * 错误回调
         *
         * @param error 错误信息
         */
        void onError(String error);
    }

    /**
     * 流式回调接口（用于真正的实时流式处理）
     */
    public interface StreamCallback {

        /**
         * 会话开始回调
         *
         * @param sessionId 会话ID
         */
        default void onStart(String sessionId) {
            log.debug("流式会话开始: sessionId={}", sessionId);
        }

        /**
         * 内容块回调（实时接收 AI 输出）
         *
         * @param chunk 流式数据块（包含 content, contentType, toolName 等）
         */
        void onChunk(StreamChunk chunk);

        /**
         * 工作流更新回调（当 AI 修改工作流时触发）
         *
         * @param result 更新结果
         */
        default void onWorkflowUpdate(Object result) {
            log.debug("工作流更新: {}", result);
        }

        /**
         * 操作确认请求回调
         * <p>
         * 当 AI 请求执行非查询操作时，需要用户确认。
         * 前端应显示确认对话框，用户确认后调用 /api/chat/action/confirm
         * </p>
         *
         * @param pendingActionId 待确认操作ID
         * @param actions         待执行的操作列表
         * @param workflowId      工作流ID
         */
        default void onConfirmationRequired(String pendingActionId, java.util.List<Map<String, Object>> actions, String workflowId) {
            log.info("操作需要确认: pendingActionId={}, actionCount={}", pendingActionId, actions.size());
        }

        /**
         * 任务完成回调
         *
         * @param sessionId 会话ID
         * @param duration  执行耗时（毫秒）
         */
        default void onDone(String sessionId, Long duration) {
            log.debug("流式会话完成: sessionId={}, duration={}ms", sessionId, duration);
        }

        /**
         * 错误回调
         *
         * @param error 错误信息
         */
        void onError(String error);
    }

    /**
     * 流式处理用户消息（真正的实时流式）
     * 支持多轮对话和 Skill 文件
     *
     * @param userMessage    用户消息
     * @param workflowId     工作流ID
     * @param conversationId 会话ID（即 Claude CLI session ID）
     * @param callback       流式回调
     */
    public void processMessageStream(String userMessage, String workflowId, String conversationId,
                                      StreamCallback callback) {
        // 确定会话ID
        String sessionId = conversationId;
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "new-" + java.util.UUID.randomUUID().toString();
        }

        // 如果有锁服务，先获取锁
        if (lockService != null) {
            if (!lockService.tryLock(sessionId)) {
                log.warn("会话正在处理中，拒绝请求: sessionId={}", sessionId);
                callback.onError("会话正在处理中，请稍后重试");
                return;
            }

            try {
                // 执行处理
                processMessageStreamWithMultiRound(userMessage, workflowId, conversationId, callback, true);
            } finally {
                // 释放锁
                lockService.unlock(sessionId);
                log.debug("释放会话锁: sessionId={}", sessionId);
            }
        } else {
            // 没有锁服务，直接执行
            processMessageStreamWithMultiRound(userMessage, workflowId, conversationId, callback, true);
        }
    }

    /**
     * 流式处理用户消息（支持多轮循环）
     *
     * @param userMessage    用户消息
     * @param workflowId     工作流ID
     * @param conversationId 会话ID
     * @param callback       流式回调
     * @param isFirstRound   是否是第一轮（第一轮需要加载 Skill 文件）
     */
    private void processMessageStreamWithMultiRound(String userMessage, String workflowId, String conversationId,
                                                     StreamCallback callback, boolean isFirstRound) {
        if (streamClient == null) {
            log.warn("流式客户端未初始化，回退到同步模式");
            callback.onError("流式客户端未初始化");
            return;
        }

        try {
            log.info("开始流式处理消息: conversationId={}, workflowId={}, isFirstRound={}",
                    conversationId, workflowId, isFirstRound);

            // 判断是否是新会话
            boolean isNewSession = conversationId == null || conversationId.isEmpty();

            // 获取或创建 session
            AgentSessionEntity session = sessionService.getOrCreateSession(workflowId, conversationId);

            // 递增轮次计数（仅在非第一轮时）
            if (!isFirstRound) {
                int currentRound = incrementRoundCount(session);

                // 检查最大轮次限制
                if (currentRound > MAX_ROUNDS) {
                    log.warn("超过最大轮次限制: currentRound={}, maxRounds={}", currentRound, MAX_ROUNDS);
                    callback.onError(buildMaxRoundsError(session));
                    // 异步标记错误状态
                    Mono.fromRunnable(() -> {
                        try {
                            sessionService.markAsError(session.getConversationId(), "超过最大轮次限制");
                        } catch (Exception e) {
                            log.error("标记会话错误失败: {}", e.getMessage());
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    return;
                }

                // 检查执行超时
                if (isTimeout(session)) {
                    log.warn("执行超时: elapsed={}ms", getElapsedTime(session));
                    callback.onError(buildTimeoutError(session));
                    // 异步标记错误状态
                    Mono.fromRunnable(() -> {
                        try {
                            sessionService.markAsError(session.getConversationId(), "执行超时");
                        } catch (Exception e) {
                            log.error("标记会话错误失败: {}", e.getMessage());
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    return;
                }

                log.info("流式处理第 {} 轮对话, conversationId={}, elapsed={}ms",
                        currentRound, session.getConversationId(), getElapsedTime(session));
            }

            // 构建上下文（使用和同步方式相同的上下文构建）
            String context = buildContext(userMessage, session);

            // 确定是否需要加载 Skill 文件（第一轮且新会话时需要）
            byte[] skillFileBytes = null;
            String skillFileName = null;
            String sessionIdForApi = null;

            if (isNewSession && isFirstRound) {
                log.info("新会话第一轮，加载 Skill 文件: {}", skillFilePath);
                skillFileBytes = loadSkillFile();
                skillFileName = "workflow-assistant.zip";
                sessionIdForApi = null;  // 新会话不传 sessionId，让 Claude CLI 生成
            } else {
                log.info("已有会话或后续轮次，sessionId: {}", session.getConversationId());
                skillFileBytes = null;  // 已有会话不传 Skill 文件
                skillFileName = null;
                sessionIdForApi = session.getConversationId();  // 传 sessionId 恢复会话
            }

            // 收集完整响应用于多轮判断
            StringBuilder fullResponse = new StringBuilder();
            java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);

            // 调用流式 API（使用完整参数，包含 skillFile）
            // 使用真正的流式处理，每个 chunk 实时转发
            streamClient.executeTaskStream(context, null, skillFileBytes, skillFileName, sessionIdForApi)
                    .subscribe(
                            chunk -> {
                                // 实时处理每个 chunk
                                handleStreamChunk(session, chunk, callback, isNewSession && isFirstRound);

                                // 收集文本内容用于多轮判断
                                if ("chunk".equals(chunk.getType()) && chunk.getContentOrMessage() != null) {
                                    // 收集所有有效内容类型：text、result、assistant
                                    String contentType = chunk.getContentType();
                                    if ("text".equals(contentType) || "result".equals(contentType)
                                            || "assistant".equals(contentType) || contentType == null) {
                                        fullResponse.append(chunk.getContentOrMessage());
                                    }
                                }

                                // 如果收到 done 事件，标记完成并处理多轮逻辑
                                if ("done".equals(chunk.getType()) && !completed.getAndSet(true)) {
                                    // 尝试解析 AgentPlan，判断是否需要继续下一轮
                                    handleStreamCompletion(session, fullResponse.toString(), workflowId, callback, isFirstRound);
                                }
                            },
                            error -> {
                                log.error("流式处理错误: {}", error.getMessage());
                                callback.onError("流式处理错误: " + error.getMessage());
                            },
                            () -> {
                                // 流完成时的回调
                                log.info("流式传输完成: workflowId={}", workflowId);
                                // 如果还没有处理完成（可能没有收到 done 事件），在这里处理
                                if (!completed.getAndSet(true)) {
                                    handleStreamCompletion(session, fullResponse.toString(), workflowId, callback, isFirstRound);
                                }
                            }
                    );

        } catch (Exception e) {
            log.error("流式处理异常: {}", e.getMessage(), e);
            callback.onError("流式处理异常: " + e.getMessage());
        }
    }

    /**
     * 处理流式完成后的多轮判断
     */
    private void handleStreamCompletion(AgentSessionEntity session, String fullResponse,
                                         String workflowId, StreamCallback callback, boolean isFirstRound) {
        try {
            // 尝试解析 AI 响应为 AgentPlan
            AgentPlan plan = parseAgentPlan(fullResponse);

            log.info("流式响应解析完成: status={}, isFirstRound={}", plan.getStatus(), isFirstRound);

            switch (plan.getStatus()) {
                case "query":
                    // AI 需要查询信息
                    log.info("检测到 query 请求，执行查询...");
                    Map<String, Object> queryResults = executeQueriesInStream(plan.getQueries(), session);
                    // 构建带结果的上下文，递归调用下一轮
                    String queryContext = buildContextWithResults(session, queryResults, "query");
                    processMessageStreamWithMultiRound(queryContext, workflowId, session.getConversationId(),
                            callback, false);
                    break;

                case "action":
                    // AI 要执行操作
                    log.info("检测到 action 请求，执行操作...");
                    Map<String, Object> actionResults = executeActionsInStream(plan.getActions(), session, callback);

                    // 检查是否有待确认的操作（非查询操作）
                    // 如果有待确认操作，actionResults 为空，需要等待用户确认
                    boolean hasMutationAction = false;
                    for (AgentPlan.Action action : plan.getActions()) {
                        if (action.getMethod() != null && isMutationMethod(action.getMethod())) {
                            hasMutationAction = true;
                            break;
                        }
                    }

                    if (hasMutationAction) {
                        // 有待确认操作，等待用户确认后通过 /api/chat/action/confirm 继续执行
                        log.info("有 {} 个操作待确认，等待用户确认...", plan.getActions().size());
                        // 不继续下一轮，等待用户确认
                    } else {
                        // 只有查询操作（GET），构建带结果的上下文，递归调用下一轮
                        String actionContext = buildContextWithResults(session, actionResults, "action");
                        processMessageStreamWithMultiRound(actionContext, workflowId, session.getConversationId(),
                                callback, false);
                    }
                    break;

                case "complete":
                    // 任务完成
                    log.info("任务完成: {}", plan.getSummary());
                    callback.onDone(session.getConversationId(), null);
                    // 异步更新状态
                    Mono.fromRunnable(() -> {
                        try {
                            sessionService.markAsCompleted(session.getConversationId());
                        } catch (Exception e) {
                            log.error("标记会话完成失败: {}", e.getMessage());
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    break;

                case "parse_error":
                    // 解析错误，将问题反馈给 AI
                    log.warn("响应格式解析错误: {}", plan.getSummary());

                    // 递增解析错误计数
                    int parseErrorCount = incrementParseErrorCount(session);

                    // 检查解析错误次数限制
                    if (parseErrorCount > MAX_PARSE_ERROR_ROUNDS) {
                        log.warn("超过最大解析错误次数限制: parseErrorCount={}, maxParseErrorRounds={}",
                                parseErrorCount, MAX_PARSE_ERROR_ROUNDS);
                        callback.onError("AI 响应格式持续异常，已尝试 " + MAX_PARSE_ERROR_ROUNDS +
                                " 次仍无法解析。请尝试简化您的请求或稍后重试。");
                        // 异步标记错误状态
                        Mono.fromRunnable(() -> {
                            try {
                                sessionService.markAsError(session.getConversationId(), "超过最大解析错误次数限制");
                            } catch (Exception e) {
                                log.error("标记会话错误失败: {}", e.getMessage());
                            }
                        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        return;
                    }

                    String errorContext = buildErrorContext(session, "响应格式解析错误", plan.getSummary());
                    processMessageStreamWithMultiRound(errorContext, workflowId, session.getConversationId(),
                            callback, false);
                    break;

                default:
                    // 未知的 status，可能是自然语言响应（非 JSON 格式）
                    // 这种情况下直接完成任务
                    log.info("非结构化响应，直接完成");
                    callback.onDone(session.getConversationId(), null);
                    break;
            }
        } catch (Exception e) {
            log.error("处理流式完成异常: {}", e.getMessage(), e);
            // 如果解析失败，可能是纯自然语言响应，直接完成
            callback.onDone(session.getConversationId(), null);
        }
    }

    /**
     * 在流式模式下执行查询
     */
    private Map<String, Object> executeQueriesInStream(List<AgentPlan.Query> queries, AgentSessionEntity session) {
        Map<String, Object> queryResults = new HashMap<>();

        if (queries == null || queries.isEmpty()) {
            log.warn("查询请求为空");
            return queryResults;
        }

        for (AgentPlan.Query query : queries) {
            log.info("流式模式执行查询: id={}, path={}", query.getId(), query.getPath());
            Object result = executeQuery(query.getMethod(), query.getPath(), query.getParams());
            queryResults.put(query.getId(), result);
        }

        // 更新 session 的查询结果
        sessionService.updateQueryResults(session.getConversationId(), queryResults);

        return queryResults;
    }

    /**
     * 在流式模式下处理操作请求
     * <p>
     * 对于非查询操作（POST、PUT、PATCH、DELETE），会先发送确认请求给前端。
     * 用户确认后，前端调用 /api/chat/action/confirm 来执行操作。
     * </p>
     */
    private Map<String, Object> executeActionsInStream(List<AgentPlan.Action> actions, AgentSessionEntity session,
                                                        StreamCallback callback) {
        Map<String, Object> actionResults = new HashMap<>();

        if (actions == null || actions.isEmpty()) {
            log.warn("操作请求为空");
            return actionResults;
        }

        // 检查是否有非查询操作
        List<Map<String, Object>> mutationActions = new ArrayList<>();
        for (AgentPlan.Action action : actions) {
            String method = action.getMethod();
            if (method != null && isMutationMethod(method)) {
                Map<String, Object> actionMap = new HashMap<>();
                actionMap.put("id", action.getId());
                actionMap.put("method", action.getMethod());
                actionMap.put("path", action.getPath());
                actionMap.put("description", action.getDescription());
                actionMap.put("body", action.getBody());
                mutationActions.add(actionMap);
            }
        }

        // 如果有非查询操作，发送确认请求
        if (!mutationActions.isEmpty()) {
            log.info("检测到 {} 个非查询操作，需要用户确认", mutationActions.size());

            // 生成待确认操作ID
            String pendingActionId = "pending-" + java.util.UUID.randomUUID().toString();

            // 调用确认回调
            callback.onConfirmationRequired(pendingActionId, mutationActions, session.getWorkflowId());

            // 返回空结果，等待用户确认后通过 /api/chat/action/confirm 执行
            return actionResults;
        }

        // 如果只有查询操作（GET），直接执行
        for (AgentPlan.Action action : actions) {
            log.info("流式模式执行查询操作: id={}, method={}, path={}",
                    action.getId(), action.getMethod(), action.getPath());

            Object result = executeAction(action.getMethod(), action.getPath(), action.getBody());
            actionResults.put(action.getId(), result);
        }

        // 更新 session 的操作结果
        sessionService.updateActionResults(session.getConversationId(), actionResults);

        return actionResults;
    }

    /**
     * 判断 HTTP 方法是否为非查询方法（需要确认）
     */
    private boolean isMutationMethod(String method) {
        if (method == null) {
            return false;
        }
        String upperMethod = method.toUpperCase();
        return "POST".equals(upperMethod) || "PUT".equals(upperMethod)
                || "PATCH".equals(upperMethod) || "DELETE".equals(upperMethod);
    }

    /**
     * 处理流式数据块
     * 注意：这个方法在 Reactor 的 Netty 线程中执行，不能进行阻塞操作
     */
    private void handleStreamChunk(AgentSessionEntity session, StreamChunk chunk,
                                    StreamCallback callback, boolean isNewSession) {
        String chunkType = chunk.getType();
        log.info("处理流式块: type={}, sessionId={}, content={}", chunkType, chunk.getSessionId(),
                chunk.getContentOrMessage() != null ? chunk.getContentOrMessage().substring(0, Math.min(50, chunk.getContentOrMessage().length())) + "..." : "null");

        // 处理 type 为 null 的情况
        if (chunkType == null) {
            // 如果 type 为 null 但有内容，作为普通 chunk 处理
            if (chunk.getContentOrMessage() != null && !chunk.getContentOrMessage().isEmpty()) {
                callback.onChunk(chunk);
            } else {
                log.debug("忽略无内容的 null 类型块");
            }
            return;
        }

        switch (chunkType) {
            case "start":
                // 会话开始，更新 sessionId
                if (chunk.getSessionId() != null && isNewSession) {
                    String oldId = session.getConversationId();
                    String newId = chunk.getSessionId();
                    // 更新内存中的 session 对象
                    session.setConversationId(newId);
                    // 异步执行数据库更新，不阻塞当前流
                    Mono.fromRunnable(() -> {
                        try {
                            sessionService.updateConversationId(oldId, newId);
                            log.info("会话ID更新成功: {} -> {}", oldId, newId);
                        } catch (Exception e) {
                            log.error("更新会话ID失败: {}", e.getMessage());
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();

                    callback.onStart(newId);
                }
                break;

            case "chunk":
                // 直接转发内容块给前端（包含 contentType 等元数据）
                if (chunk.getContentOrMessage() != null) {
                    log.info("转发 chunk 到回调，contentType={}, toolName={}, contentLength={}",
                            chunk.getContentType(), chunk.getToolName(), chunk.getContentOrMessage().length());
                    callback.onChunk(chunk);
                }
                break;

            case "done":
                // 注意：在多轮模式下，done 事件由 handleStreamCompletion 统一处理
                // 这里只记录日志，不调用 callback.onDone()
                // 因为 collectList().subscribe() 会在收集完成后调用 handleStreamCompletion
                log.debug("收到 done 事件，sessionId={}, duration={}", chunk.getSessionId(), chunk.getDuration());
                break;

            case "error":
                // 错误
                String errorMsg = chunk.getContentOrMessage() != null
                        ? chunk.getContentOrMessage()
                        : "未知错误";
                callback.onError(errorMsg);
                // 异步更新状态
                Mono.fromRunnable(() -> {
                    try {
                        sessionService.markAsError(session.getConversationId(), errorMsg);
                    } catch (Exception e) {
                        log.error("标记会话错误失败: {}", e.getMessage());
                    }
                }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                break;

            default:
                log.debug("未知的流式块类型: {}", chunk.getType());
        }
    }

    /**
     * 构建流式上下文（简化版，用于直接流式响应）
     */
    private String buildStreamContext(String userMessage, AgentSessionEntity session) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户请求: ").append(userMessage).append("\n\n");
        sb.append("workflowId: ").append(session.getWorkflowId()).append("\n\n");
        sb.append("请直接响应用户请求，以自然语言形式回复。");
        return sb.toString();
    }

    /**
     * Agent 计划内部类
     */
    @lombok.Data
    public static class AgentPlan {
        private String status;
        private String reasoning;
        private String summary;
        private Object result;
        private List<Query> queries;
        private List<Action> actions;

        @lombok.Data
        public static class Query {
            private String id;
            private String method;
            private String path;
            private String description;
            private Map<String, Object> params;
        }

        @lombok.Data
        public static class Action {
            private String id;
            private String method;      // HTTP 方法：GET, POST, PUT, DELETE
            private String path;        // API 路径：/api/workflow/1/data/json
            private String description;
            private Map<String, Object> body;  // 请求体数据
        }
    }
}
