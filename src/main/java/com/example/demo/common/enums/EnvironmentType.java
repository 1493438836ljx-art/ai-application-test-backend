package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnvironmentType {

    HTTP_API("HTTP_API", "HTTP API"),
    SDK("SDK", "SDK集成"),
    CUSTOM("CUSTOM", "自定义");

    private final String code;
    private final String description;

    public static EnvironmentType fromCode(String code) {
        for (EnvironmentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EnvironmentType code: " + code);
    }
}
