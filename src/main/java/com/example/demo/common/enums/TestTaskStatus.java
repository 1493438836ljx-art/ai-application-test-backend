package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 测试任务状态枚举
 * <p>
 * 定义测试任务的生命周期状态，状态转换流程：
 * PENDING -> RUNNING -> COMPLETED/FAILED/CANCELLED
 * 或 PENDING -> RUNNING -> PAUSED -> RUNNING
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum TestTaskStatus {

    /** 待执行 - 任务已创建，等待开始执行 */
    PENDING("PENDING", "待执行"),

    /** 执行中 - 任务正在执行测试用例 */
    RUNNING("RUNNING", "执行中"),

    /** 已暂停 - 任务被暂停，可恢复执行 */
    PAUSED("PAUSED", "已暂停"),

    /** 已完成 - 任务执行完成（全部用例执行成功） */
    COMPLETED("COMPLETED", "已完成"),

    /** 执行失败 - 任务执行过程中发生错误 */
    FAILED("FAILED", "执行失败"),

    /** 已取消 - 任务被手动取消 */
    CANCELLED("CANCELLED", "已取消");

    /** 状态编码 */
    private final String code;

    /** 状态描述 */
    private final String description;

    /**
     * 根据编码获取任务状态
     *
     * @param code 状态编码
     * @return 对应的任务状态枚举值
     * @throws IllegalArgumentException 如果编码不存在
     */
    public static TestTaskStatus fromCode(String code) {
        for (TestTaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TestTaskStatus code: " + code);
    }
}
