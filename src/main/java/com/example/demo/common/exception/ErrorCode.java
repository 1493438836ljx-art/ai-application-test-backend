package com.example.demo.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * <p>
 * 定义系统中所有业务错误的编码和消息，按模块分段：
 * <ul>
 *   <li>1xxx - 通用错误</li>
 *   <li>2xxx - 测评集相关错误</li>
 *   <li>3xxx - Prompt相关错误</li>
 *   <li>4xxx - 环境相关错误</li>
 *   <li>5xxx - 插件相关错误</li>
 *   <li>6xxx - 任务相关错误</li>
 *   <li>7xxx - 结果相关错误</li>
 * </ul>
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 通用错误 1xxx ====================

    /** 参数无效 */
    INVALID_PARAMETER(1001, "参数无效"),

    /** 资源不存在 */
    RESOURCE_NOT_FOUND(1002, "资源不存在"),

    /** 资源已存在 */
    RESOURCE_ALREADY_EXISTS(1003, "资源已存在"),

    /** 操作失败 */
    OPERATION_FAILED(1004, "操作失败"),

    /** 内部错误 */
    INTERNAL_ERROR(1005, "内部错误"),

    // ==================== 测评集相关 2xxx ====================

    /** 测评集不存在 */
    TEST_SET_NOT_FOUND(2001, "测评集不存在"),

    /** 测试用例不存在 */
    TEST_CASE_NOT_FOUND(2002, "测试用例不存在"),

    /** 测评集导入失败 */
    TEST_SET_IMPORT_FAILED(2003, "测评集导入失败"),

    /** 测评集导出失败 */
    TEST_SET_EXPORT_FAILED(2004, "测评集导出失败"),

    // ==================== Prompt相关 3xxx ====================

    /** Prompt不存在 */
    PROMPT_NOT_FOUND(3001, "Prompt不存在"),

    /** Prompt渲染失败 */
    PROMPT_RENDER_FAILED(3002, "Prompt渲染失败"),

    // ==================== 环境相关 4xxx ====================

    /** 环境不存在 */
    ENVIRONMENT_NOT_FOUND(4001, "环境不存在"),

    /** 环境连接失败 */
    ENVIRONMENT_CONNECTION_FAILED(4002, "环境连接失败"),

    /** 不支持的环境类型 */
    ENVIRONMENT_TYPE_NOT_SUPPORTED(4003, "不支持的环境类型"),

    // ==================== 插件相关 5xxx ====================

    /** 插件不存在 */
    PLUGIN_NOT_FOUND(5001, "插件不存在"),

    /** 不支持的插件类型 */
    PLUGIN_TYPE_NOT_SUPPORTED(5002, "不支持的插件类型"),

    /** 插件执行失败 */
    PLUGIN_EXECUTION_FAILED(5003, "插件执行失败"),

    /** 插件评估失败 */
    PLUGIN_EVALUATION_FAILED(5004, "插件评估失败"),

    // ==================== 任务相关 6xxx ====================

    /** 任务不存在 */
    TASK_NOT_FOUND(6001, "任务不存在"),

    /** 任务已在运行中 */
    TASK_ALREADY_RUNNING(6002, "任务已在运行中"),

    /** 任务未在运行中 */
    TASK_NOT_RUNNING(6003, "任务未在运行中"),

    /** 任务执行失败 */
    TASK_EXECUTION_FAILED(6004, "任务执行失败"),

    /** 任务执行项不存在 */
    TASK_ITEM_NOT_FOUND(6005, "任务执行项不存在"),

    // ==================== 结果相关 7xxx ====================

    /** 测试结果不存在 */
    RESULT_NOT_FOUND(7001, "测试结果不存在"),

    /** 测试报告不存在 */
    REPORT_NOT_FOUND(7002, "测试报告不存在"),

    /** 报告生成失败 */
    REPORT_GENERATION_FAILED(7003, "报告生成失败"),

    /** 导出失败 */
    EXPORT_FAILED(7004, "导出失败");

    /** 错误码 */
    private final Integer code;

    /** 错误消息 */
    private final String message;
}
