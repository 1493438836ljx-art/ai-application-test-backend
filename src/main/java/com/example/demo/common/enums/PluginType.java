package com.example.demo.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 插件类型枚举
 * <p>
 * 定义测试框架支持的两类插件：执行插件和评估插件
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PluginType {

    /** 执行插件 - 负责调用AI应用执行推理任务 */
    EXECUTION("EXECUTION", "执行插件"),

    /** 评估插件 - 负责评估推理结果的质量 */
    EVALUATION("EVALUATION", "评估插件");

    /** 类型编码 */
    private final String code;

    /** 类型描述 */
    private final String description;

    /**
     * 根据编码获取插件类型
     *
     * @param code 类型编码
     * @return 对应的插件类型枚举值
     * @throws IllegalArgumentException 如果编码不存在
     */
    public static PluginType fromCode(String code) {
        for (PluginType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PluginType code: " + code);
    }
}
