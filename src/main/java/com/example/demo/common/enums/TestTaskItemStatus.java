package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestTaskItemStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String description;

    public static TestTaskItemStatus fromCode(String code) {
        for (TestTaskItemStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TestTaskItemStatus code: " + code);
    }
}
