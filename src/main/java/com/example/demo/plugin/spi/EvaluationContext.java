package com.example.demo.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 评估上下文类，封装评估插件运行时所需的上下文信息
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {

    /** 任务项ID，关联具体的测试任务项 */
    private Long taskItemId;

    /** 输入内容，原始的测试输入 */
    private String input;

    /** 期望输出，测试用例中的预期结果 */
    private String expectedOutput;

    /** 实际输出，AI推理返回的实际结果 */
    private String actualOutput;

    /** 插件配置，评估插件的自定义参数 */
    private Map<String, Object> pluginConfig;
}
