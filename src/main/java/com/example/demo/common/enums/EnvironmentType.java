package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 环境类型枚举
 * <p>
 * 定义生成式AI应用/软件的对接方式类型
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum EnvironmentType {

    /** HTTP API方式 - 通过HTTP接口调用AI应用 */
    HTTP_API("HTTP_API", "HTTP API"),

    /** SDK集成方式 - 通过SDK直接集成 */
    SDK("SDK", "SDK集成"),

    /** 自定义方式 - 用户自定义对接方式 */
    CUSTOM("CUSTOM", "自定义");

    /** 类型编码 */
    private final String code;

    /** 类型描述 */
    private final String description;

    /**
     * 根据编码获取环境类型
     *
     * @param code 类型编码
     * @return 对应的环境类型枚举值
     * @throws IllegalArgumentException 如果编码不存在
     */
    public static EnvironmentType fromCode(String code) {
        for (EnvironmentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EnvironmentType code: " + code);
    }
}
