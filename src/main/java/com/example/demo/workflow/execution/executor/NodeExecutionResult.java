package com.example.demo.workflow.execution.executor;

import lombok.Builder;
import lombok.Data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行结果
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
public class NodeExecutionResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 输出参数
     */
    @Builder.Default
    private Map<String, Object> outputs = new HashMap<>();

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 执行日志
     */
    private String logs;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 跳过原因（当节点被跳过时）
     */
    private String skipReason;

    /**
     * 下一步要执行的节点（用于条件分支）
     */
    private List<String> nextNodes;

    /**
     * 创建成功结果
     */
    public static NodeExecutionResult success() {
        return NodeExecutionResult.builder()
                .success(true)
                .outputs(new HashMap<>())
                .build();
    }

    /**
     * 创建成功结果（带输出）
     */
    public static NodeExecutionResult success(Map<String, Object> outputs) {
        return NodeExecutionResult.builder()
                .success(true)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static NodeExecutionResult failure(String errorMessage) {
        return NodeExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败结果（带异常）
     */
    public static NodeExecutionResult failure(String errorMessage, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        return NodeExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorStack(sw.toString())
                .build();
    }

    /**
     * 创建跳过结果
     */
    public static NodeExecutionResult skipped(String reason) {
        return NodeExecutionResult.builder()
                .success(true)
                .skipReason(reason)
                .build();
    }

    /**
     * 创建带分支的结果
     */
    public static NodeExecutionResult withBranch(String branchNodeUuid) {
        List<String> nextNodes = new ArrayList<>();
        nextNodes.add(branchNodeUuid);
        return NodeExecutionResult.builder()
                .success(true)
                .nextNodes(nextNodes)
                .build();
    }

    /**
     * 创建带多个分支的结果
     */
    public static NodeExecutionResult withBranches(List<String> branchNodeUuids) {
        return NodeExecutionResult.builder()
                .success(true)
                .nextNodes(branchNodeUuids)
                .build();
    }

    /**
     * 是否被跳过
     */
    public boolean isSkipped() {
        return success && skipReason != null;
    }

    /**
     * 是否有分支
     */
    public boolean hasBranch() {
        return nextNodes != null && !nextNodes.isEmpty();
    }
}
