package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 测试任务项状态枚举
 * <p>
 * 定义单个测试用例的执行状态
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum TestTaskItemStatus {

    /** 待执行 - 测试用例等待执行 */
    PENDING("PENDING", "待执行"),

    /** 执行中 - 测试用例正在执行 */
    RUNNING("RUNNING", "执行中"),

    /** 执行成功 - 测试用例执行成功 */
    SUCCESS("SUCCESS", "执行成功"),

    /** 执行失败 - 测试用例执行失败 */
    FAILED("FAILED", "执行失败"),

    /** 已跳过 - 测试用例被跳过 */
    SKIPPED("SKIPPED", "已跳过");

    /** 状态编码 */
    private final String code;

    /** 状态描述 */
    private final String description;

    /**
     * 根据编码获取任务项状态
     *
     * @param code 状态编码
     * @return 对应的任务项状态枚举值
     * @throws IllegalArgumentException 如果编码不存在
     */
    public static TestTaskItemStatus fromCode(String code) {
        for (TestTaskItemStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TestTaskItemStatus code: " + code);
    }
}
