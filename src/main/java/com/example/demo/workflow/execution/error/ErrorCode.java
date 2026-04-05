package com.example.demo.workflow.execution.error;

import lombok.Getter;

/**
 * 错误代码枚举
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
public enum ErrorCode {

    // ==================== 节点执行错误 (1xxx) ====================
    NODE_EXECUTION_FAILED(1001, "节点执行失败", ErrorType.BUSINESS),
    NODE_TIMEOUT(1002, "节点执行超时", ErrorType.RECOVERABLE),
    NODE_SKIPPED(1003, "节点被跳过", ErrorType.BUSINESS),
    NODE_INPUT_INVALID(1004, "节点输入参数无效", ErrorType.BUSINESS),
    NODE_OUTPUT_INVALID(1005, "节点输出参数无效", ErrorType.BUSINESS),
    NODE_NOT_FOUND(1006, "节点不存在", ErrorType.SYSTEM),
    NODE_EXECUTOR_NOT_FOUND(1007, "未找到节点执行器", ErrorType.SYSTEM),

    // ==================== Skill 执行错误 (2xxx) ====================
    SKILL_NOT_FOUND(2001, "Skill 不存在", ErrorType.SYSTEM),
    SKILL_NOT_PUBLISHED(2002, "Skill 未发布", ErrorType.SYSTEM),
    SKILL_EXECUTION_FAILED(2003, "Skill 执行失败", ErrorType.BUSINESS),
    SKILL_TIMEOUT(2004, "Skill 执行超时", ErrorType.RECOVERABLE),
    SKILL_INCOMPATIBLE(2005, "Skill 版本不兼容", ErrorType.SYSTEM),

    // ==================== 工作流错误 (5xxx) ====================
    WORKFLOW_NOT_FOUND(5001, "工作流不存在", ErrorType.SYSTEM),
    WORKFLOW_NOT_PUBLISHED(5002, "工作流未发布", ErrorType.SYSTEM),
    WORKFLOW_CYCLIC_DEPENDENCY(5003, "工作流存在循环依赖", ErrorType.SYSTEM),
    WORKFLOW_EXECUTION_FAILED(5004, "工作流执行失败", ErrorType.BUSINESS),
    WORKFLOW_TIMEOUT(5005, "工作流执行超时", ErrorType.RECOVERABLE),
    WORKFLOW_NO_START_NODE(5006, "工作流缺少开始节点", ErrorType.SYSTEM),
    WORKFLOW_NO_END_NODE(5007, "工作流缺少结束节点", ErrorType.SYSTEM),

    // ==================== 参数解析错误 (6xxx) ====================
    PARAM_RESOLVE_FAILED(6001, "参数解析失败", ErrorType.BUSINESS),
    PARAM_REFERENCE_INVALID(6002, "无效的参数引用", ErrorType.BUSINESS),
    PARAM_TYPE_MISMATCH(6003, "参数类型不匹配", ErrorType.BUSINESS),
    PARAM_REQUIRED_MISSING(6004, "必填参数缺失", ErrorType.BUSINESS),

    // ==================== 系统错误 (9xxx) ====================
    INTERNAL_ERROR(9001, "系统内部错误", ErrorType.SYSTEM),
    CONFIGURATION_ERROR(9002, "配置错误", ErrorType.SYSTEM),
    RESOURCE_EXHAUSTED(9003, "资源不足", ErrorType.RECOVERABLE),
    DATABASE_ERROR(9004, "数据库错误", ErrorType.SYSTEM),
    THREAD_POOL_REJECTED(9005, "线程池任务被拒绝", ErrorType.RECOVERABLE);

    private final int code;
    private final String message;
    private final ErrorType type;

    ErrorCode(int code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }
}
