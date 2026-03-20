package com.example.demo.agent.api;

import com.example.demo.agent.dto.AgentConfig;
import com.example.demo.agent.dto.AgentRequest;
import com.example.demo.agent.dto.AgentResponse;
import com.example.demo.agent.framework.AgentExecutor.AgentCallback;

/**
 * Agent 框架统一服务接口
 * <p>
 * 提供给外部系统调用的统一接口，用于执行 Claude Code Agent 任务
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 注入 AgentService
 * @Autowired
 * private AgentService agentService;
 *
 * // 简单调用
 * AgentResponse response = agentService.execute("列出当前目录文件");
 *
 * // 带配置调用
 * AgentResponse response = agentService.execute("创建文件", 60, false);
 *
 * // 完整调用
 * AgentRequest request = AgentRequest.builder()
 *     .taskContent("执行任务")
 *     .config(AgentConfig.builder().timeout(120).debug(true).build())
 *     .build();
 * AgentResponse response = agentService.execute(request);
 *
 * // 带回调调用
 * AgentResponse response = agentService.execute(request, new AgentCallback() {
 *     @Override
 *     public void beforeExecute(AgentRequest req) {
 *         System.out.println("开始执行: " + req.getTaskContent());
 *     }
 *
 *     @Override
 *     public void afterExecute(AgentRequest req, AgentResponse res) {
 *         System.out.println("执行完成: " + res.getSuccess());
 *     }
 * });
 *
 * // 异步调用
 * agentService.executeAsync(request, callback);
 * }</pre>
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface AgentService {

    /**
     * 执行 Agent 任务（简单接口）
     *
     * @param taskContent 任务内容
     * @return Agent 响应
     */
    AgentResponse execute(String taskContent);

    /**
     * 执行 Agent 任务（带配置）
     *
     * @param taskContent 任务内容
     * @param timeout     超时时间（秒），默认 120
     * @param debug       是否开启调试，默认 false
     * @return Agent 响应
     */
    AgentResponse execute(String taskContent, Integer timeout, Boolean debug);

    /**
     * 执行 Agent 任务（完整请求）
     *
     * @param request Agent 请求
     * @return Agent 响应
     */
    AgentResponse execute(AgentRequest request);

    /**
     * 执行 Agent 任务（带回调）
     *
     * @param request  Agent 请求
     * @param callback 执行回调
     * @return Agent 响应
     */
    AgentResponse execute(AgentRequest request, AgentCallback callback);

    /**
     * 异步执行 Agent 任务
     *
     * @param request  Agent 请求
     * @param callback 执行回调
     */
    void executeAsync(AgentRequest request, AgentCallback callback);

    /**
     * 执行 Agent 任务（带 Skill 文件）
     *
     * @param taskContent  任务内容
     * @param config       Agent 配置
     * @param skillFile    Skill 文件字节数组
     * @param skillFileName Skill 文件名
     * @return Agent 响应
     */
    AgentResponse executeWithSkill(String taskContent, AgentConfig config,
                                    byte[] skillFile, String skillFileName);

    /**
     * 检查 Claude Code 服务健康状态
     *
     * @return true 表示服务正常，false 表示服务异常
     */
    boolean checkHealth();
}
