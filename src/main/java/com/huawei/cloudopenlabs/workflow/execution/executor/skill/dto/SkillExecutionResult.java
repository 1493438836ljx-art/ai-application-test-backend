package com.huawei.cloudopenlabs.workflow.execution.executor.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Skill执行结果
 * 包含执行完成后的所有信息
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillExecutionResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 输出参数（参数名 -> 值）
     */
    private Map<String, Object> outputs;

    /**
     * 执行日志
     */
    private String logs;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 创建成功结果
     */
    public static SkillExecutionResult success(Map<String, Object> outputs) {
        return SkillExecutionResult.builder()
                .success(true)
                .outputs(outputs)
                .build();
    }

    /**
     * 创建成功结果（带日志）
     */
    public static SkillExecutionResult success(Map<String, Object> outputs, String logs) {
        return SkillExecutionResult.builder()
                .success(true)
                .outputs(outputs)
                .logs(logs)
                .build();
    }

    /**
     * 创建成功结果（带日志和耗时）
     */
    public static SkillExecutionResult success(Map<String, Object> outputs, String logs, Long durationMs) {
        return SkillExecutionResult.builder()
                .success(true)
                .outputs(outputs)
                .logs(logs)
                .durationMs(durationMs)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static SkillExecutionResult failure(String errorMessage) {
        return SkillExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败结果（带堆栈）
     */
    public static SkillExecutionResult failure(String errorMessage, String errorStack) {
        return SkillExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorStack(errorStack)
                .build();
    }
}
