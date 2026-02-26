package com.example.demo.plugin.spi;

/**
 * 评估插件接口，定义推理结果评估的标准行为
 * <p>
 * 所有评估插件都需要实现此接口，用于评估AI推理结果的质量
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface EvaluationPlugin {

    /**
     * 获取插件名称
     *
     * @return 插件的唯一标识名称
     */
    String getName();

    /**
     * 获取插件描述
     *
     * @return 插件的功能描述信息
     */
    String getDescription();

    /**
     * 评估推理结果
     *
     * @param context 评估上下文，包含期望输出和实际输出
     * @return 评估结果，包含得分和评估原因
     */
    EvaluationResult evaluate(EvaluationContext context);
}
