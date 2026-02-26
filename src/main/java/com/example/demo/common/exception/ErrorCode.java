package com.example.demo.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误 1xxx
    INVALID_PARAMETER(1001, "参数无效"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),
    RESOURCE_ALREADY_EXISTS(1003, "资源已存在"),
    OPERATION_FAILED(1004, "操作失败"),
    INTERNAL_ERROR(1005, "内部错误"),

    // 测评集相关 2xxx
    TEST_SET_NOT_FOUND(2001, "测评集不存在"),
    TEST_CASE_NOT_FOUND(2002, "测试用例不存在"),
    TEST_SET_IMPORT_FAILED(2003, "测评集导入失败"),
    TEST_SET_EXPORT_FAILED(2004, "测评集导出失败"),

    // Prompt相关 3xxx
    PROMPT_NOT_FOUND(3001, "Prompt不存在"),
    PROMPT_RENDER_FAILED(3002, "Prompt渲染失败"),

    // 环境相关 4xxx
    ENVIRONMENT_NOT_FOUND(4001, "环境不存在"),
    ENVIRONMENT_CONNECTION_FAILED(4002, "环境连接失败"),
    ENVIRONMENT_TYPE_NOT_SUPPORTED(4003, "不支持的环境类型"),

    // 插件相关 5xxx
    PLUGIN_NOT_FOUND(5001, "插件不存在"),
    PLUGIN_TYPE_NOT_SUPPORTED(5002, "不支持的插件类型"),
    PLUGIN_EXECUTION_FAILED(5003, "插件执行失败"),
    PLUGIN_EVALUATION_FAILED(5004, "插件评估失败"),

    // 任务相关 6xxx
    TASK_NOT_FOUND(6001, "任务不存在"),
    TASK_ALREADY_RUNNING(6002, "任务已在运行中"),
    TASK_NOT_RUNNING(6003, "任务未在运行中"),
    TASK_EXECUTION_FAILED(6004, "任务执行失败"),
    TASK_ITEM_NOT_FOUND(6005, "任务执行项不存在"),

    // 结果相关 7xxx
    RESULT_NOT_FOUND(7001, "测试结果不存在"),
    REPORT_NOT_FOUND(7002, "测试报告不存在"),
    REPORT_GENERATION_FAILED(7003, "报告生成失败"),
    EXPORT_FAILED(7004, "导出失败");

    private final Integer code;
    private final String message;
}
