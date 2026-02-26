package com.example.demo.plugin.builtin;

import com.example.demo.plugin.spi.EvaluationContext;
import com.example.demo.plugin.spi.EvaluationResult;
import com.example.demo.plugin.spi.EvaluationPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 精确匹配评估插件，比较实际输出与期望输出是否完全一致
 * <p>
 * 该插件对输出进行去除首尾空白后进行精确字符串匹配，
 * 匹配成功得分为1.0，否则为0.0
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class ExactMatchEvaluationPlugin implements EvaluationPlugin {

    /** 插件名称常量 */
    public static final String NAME = "exact-match";

    /**
     * 获取插件名称
     *
     * @return 插件名称 "exact-match"
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * 获取插件描述
     *
     * @return 插件功能描述
     */
    @Override
    public String getDescription() {
        return "精确匹配评估：比较实际输出与期望输出是否完全一致";
    }

    /**
     * 评估推理结果
     * <p>
     * 比较期望输出和实际输出是否精确匹配（去除首尾空白后比较）
     * </p>
     *
     * @param context 评估上下文，包含期望输出和实际输出
     * @return 评估结果，包含得分和匹配状态
     */
    @Override
    public EvaluationResult evaluate(EvaluationContext context) {
        String expected = context.getExpectedOutput();
        String actual = context.getActualOutput();

        if (expected == null) {
            return EvaluationResult.builder()
                    .success(false)
                    .score(0)
                    .reason("期望输出为空")
                    .build();
        }

        if (actual == null) {
            return EvaluationResult.builder()
                    .success(false)
                    .score(0)
                    .reason("实际输出为空")
                    .build();
        }

        boolean match = expected.trim().equals(actual.trim());

        return EvaluationResult.builder()
                .success(match)
                .score(match ? 1.0 : 0.0)
                .reason(match ? "精确匹配成功" : "输出不匹配")
                .build();
    }
}
