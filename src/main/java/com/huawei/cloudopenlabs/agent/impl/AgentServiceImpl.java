/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.impl;

import com.huawei.cloudopenlabs.agent.api.AgentService;
import com.huawei.cloudopenlabs.agent.dto.AgentConfig;
import com.huawei.cloudopenlabs.agent.dto.AgentRequest;
import com.huawei.cloudopenlabs.agent.dto.AgentResponse;
import com.huawei.cloudopenlabs.agent.framework.AgentExecutor;
import com.huawei.cloudopenlabs.agent.framework.AgentExecutor.AgentCallback;
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
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
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
