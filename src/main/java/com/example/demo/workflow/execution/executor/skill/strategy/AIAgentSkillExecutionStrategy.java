package com.example.demo.workflow.execution.executor.skill.strategy;

import com.example.demo.workflow.execution.executor.skill.dto.SkillExecutionRequest;
import com.example.demo.workflow.execution.executor.skill.dto.SkillExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI代理执行策略
 * 通过AI Agent执行Skill（当前为骨架实现）
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@Component
public class AIAgentSkillExecutionStrategy implements SkillExecutionStrategy {

    @Override
    public String getExecutionType() {
        return "AI";
    }

    @Override
    public String getStrategyName() {
        return "AI代理执行策略";
    }

    @Override
    public SkillExecutionResult execute(SkillExecutionRequest request) {
        log.info("执行AI代理: skillId={}, skillName={}",
                request.getSkillId(), request.getSkillName());

        long startTime = System.currentTimeMillis();

        try {
            // TODO: 实现真正的AI代理执行逻辑
            // 1. 构建Prompt
            // 2. 调用AI Agent API
            // 3. 解析AI响应
            // 4. 提取输出参数

            // 当前返回模拟结果
            log.warn("AI代理执行策略尚未完全实现，返回模拟结果");

            // 模拟输出：简单地将输入参数相加
            Map<String, Object> outputs = new HashMap<>();
            if (request.getInputParameters() != null) {
                // 查找数值类型的输入参数并尝试计算
                Number sum = null;
                for (SkillExecutionRequest.SkillParameterDef param : request.getInputParameters()) {
                    if (param.getValue() instanceof Number) {
                        if (sum == null) {
                            sum = ((Number) param.getValue()).doubleValue();
                        } else {
                            sum = sum.doubleValue() + ((Number) param.getValue()).doubleValue();
                        }
                    }
                }
                if (sum != null && request.getOutputParameters() != null && !request.getOutputParameters().isEmpty()) {
                    SkillExecutionRequest.SkillParameterDef outputParam = request.getOutputParameters().get(0);
                    if (outputParam != null) {
                        outputs.put(outputParam.getName(), sum.intValue());
                    }
                }
            }

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("AI代理执行完成(模拟): skillId={}, durationMs={}, outputs={}",
                    request.getSkillId(), durationMs, outputs);

            return SkillExecutionResult.success(outputs, "AI代理执行完成(模拟)", durationMs);

        } catch (Exception e) {
            log.error("AI代理执行异常: skillId={}", request.getSkillId(), e);
            return SkillExecutionResult.failure("AI代理执行失败: " + e.getMessage());
        }
    }
}
