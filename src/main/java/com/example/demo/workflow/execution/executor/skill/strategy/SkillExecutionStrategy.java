package com.example.demo.workflow.execution.executor.skill.strategy;

import com.example.demo.workflow.execution.executor.skill.dto.SkillExecutionRequest;
import com.example.demo.workflow.execution.executor.skill.dto.SkillExecutionResult;

/**
 * Skill执行策略接口
 * 定义统一的执行接口，不同的执行方式实现此接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
public interface SkillExecutionStrategy {

    /**
     * 获取执行类型
     * @return "AUTOMATED" / "AI"
     */
    String getExecutionType();

    /**
     * 执行Skill
     *
     * @param request 执行请求（包含所有执行所需信息）
     * @return 执行结果
     */
    SkillExecutionResult execute(SkillExecutionRequest request);

    /**
     * 获取策略名称（用于日志）
     */
    default String getStrategyName() {
        return "默认执行策略";
    }
}
