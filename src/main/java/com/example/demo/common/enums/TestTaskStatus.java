package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestTaskStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    PAUSED("PAUSED", "已暂停"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "执行失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    public static TestTaskStatus fromCode(String code) {
        for (TestTaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TestTaskStatus code: " + code);
    }
}
