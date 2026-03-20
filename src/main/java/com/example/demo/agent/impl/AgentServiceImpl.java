package com.example.demo.agent.impl;

import com.example.demo.agent.api.AgentService;
import com.example.demo.agent.dto.AgentConfig;
import com.example.demo.agent.dto.AgentRequest;
import com.example.demo.agent.dto.AgentResponse;
import com.example.demo.agent.framework.AgentExecutor;
import com.example.demo.agent.framework.AgentExecutor.AgentCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Agent 框架服务实现类
 * <p>
 * 实现 AgentService 接口，代理调用 AgentExecutor
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "claude.code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentServiceImpl implements AgentService {

    private final AgentExecutor agentExecutor;

    @Override
    public AgentResponse execute(String taskContent) {
        return agentExecutor.executeSimple(taskContent);
    }

    @Override
    public AgentResponse execute(String taskContent, Integer timeout, Boolean debug) {
        return agentExecutor.executeWithConfig(taskContent, timeout, debug);
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        return agentExecutor.execute(request);
    }

    @Override
    public AgentResponse execute(AgentRequest request, AgentCallback callback) {
        return agentExecutor.execute(request, callback);
    }

    @Override
    public void executeAsync(AgentRequest request, AgentCallback callback) {
        agentExecutor.executeAsync(request, callback);
    }

    @Override
    public AgentResponse executeWithSkill(String taskContent, AgentConfig config,
                                          byte[] skillFile, String skillFileName) {
        AgentRequest request = AgentRequest.builder()
                .taskContent(taskContent)
                .config(config != null ? config : AgentConfig.builder().build())
                .skillFileBytes(skillFile)
                .skillFileName(skillFileName)
                .build();
        return agentExecutor.execute(request);
    }

    @Override
    public boolean checkHealth() {
        return agentExecutor.checkHealth();
    }
}
