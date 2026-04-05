package com.example.demo.workflow.execution.error;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行异常基类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
public class WorkflowExecutionException extends RuntimeException {

    private final ErrorCode errorCode;
    private final ErrorType errorType;
    private final String nodeUuid;
    private final String nodeName;
    private final Map<String, Object> context;

    public WorkflowExecutionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorType = errorCode.getType();
        this.nodeUuid = null;
        this.nodeName = null;
        this.context = new HashMap<>();
    }

    public WorkflowExecutionException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorCode.getType();
        this.nodeUuid = null;
        this.nodeName = null;
        this.context = new HashMap<>();
    }

    public WorkflowExecutionException(ErrorCode errorCode, String nodeUuid, String nodeName, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorCode.getType();
        this.nodeUuid = nodeUuid;
        this.nodeName = nodeName;
        this.context = new HashMap<>();
    }

    public WorkflowExecutionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.errorType = errorCode.getType();
        this.nodeUuid = null;
        this.nodeName = null;
        this.context = new HashMap<>();
    }

    public WorkflowExecutionException(ErrorCode errorCode, String nodeUuid, String nodeName, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorCode.getType();
        this.nodeUuid = nodeUuid;
        this.nodeName = nodeName;
        this.context = new HashMap<>();
    }

    /**
     * 添加上下文信息
     */
    public WorkflowExecutionException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * 获取完整错误信息
     */
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode.getCode()).append("] ");
        sb.append(errorCode.getMessage());

        if (nodeUuid != null) {
            sb.append(" - 节点: ").append(nodeName != null ? nodeName : nodeUuid);
        }

        if (getMessage() != null && !getMessage().equals(errorCode.getMessage())) {
            sb.append(" - 详情: ").append(getMessage());
        }

        return sb.toString();
    }

    /**
     * 判断是否可重试
     */
    public boolean isRetryable() {
        return errorType == ErrorType.RECOVERABLE;
    }
}
