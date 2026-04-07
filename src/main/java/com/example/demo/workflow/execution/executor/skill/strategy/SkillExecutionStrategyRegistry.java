package com.example.demo.workflow.execution.executor.skill.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill执行策略注册表
 * 自动收集所有策略实现，根据executionType分发
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Component
public class SkillExecutionStrategyRegistry {

    private final Map<String, SkillExecutionStrategy> strategyMap = new HashMap<>();

    /**
     * 构造函数，Spring自动注入所有策略实现
     */
    public SkillExecutionStrategyRegistry(List<SkillExecutionStrategy> strategies) {
        strategies.forEach(strategy -> {
            String type = strategy.getExecutionType();
            if (strategyMap.containsKey(type)) {
                log.warn("覆盖已存在的Skill执行策略: type={}, oldStrategy={}, newStrategy={}",
                        type,
                        strategyMap.get(type).getStrategyName(),
                        strategy.getStrategyName());
            }
            strategyMap.put(type, strategy);
            log.info("注册Skill执行策略: type={}, strategy={}",
                    type, strategy.getStrategyName());
        });

        if (strategyMap.isEmpty()) {
            log.warn("没有注册任何Skill执行策略");
        }
    }

    /**
     * 获取执行策略
     *
     * @param executionType 执行类型
     * @return 对应的执行策略，默认返回AUTOMATED策略
     */
    public SkillExecutionStrategy getStrategy(String executionType) {
        SkillExecutionStrategy strategy = strategyMap.get(executionType);
        if (strategy == null) {
            log.warn("未找到执行类型 {} 对应的策略，使用默认AUTOMATED策略", executionType);
            return strategyMap.get("AUTOMATED");
        }
        return strategy;
    }

    /**
     * 获取所有已注册的策略类型
     */
    public java.util.Set<String> getRegisteredTypes() {
        return strategyMap.keySet();
    }
}
