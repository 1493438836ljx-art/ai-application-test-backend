package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PluginType {

    EXECUTION("EXECUTION", "执行插件"),
    EVALUATION("EVALUATION", "评估插件");

    private final String code;
    private final String description;

    public static PluginType fromCode(String code) {
        for (PluginType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PluginType code: " + code);
    }
}
