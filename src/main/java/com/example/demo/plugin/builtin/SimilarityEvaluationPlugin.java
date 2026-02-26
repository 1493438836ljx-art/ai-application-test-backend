package com.example.demo.plugin.builtin;

import com.example.demo.plugin.spi.EvaluationContext;
import com.example.demo.plugin.spi.EvaluationResult;
import com.example.demo.plugin.spi.EvaluationPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 相似度评估插件，计算实际输出与期望输出的文本相似度
 * <p>
 * 使用Jaccard相似度算法计算两个文本之间的相似程度，
 * 支持通过配置参数设置相似度阈值
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class SimilarityEvaluationPlugin implements EvaluationPlugin {

    /** 插件名称常量 */
    public static final String NAME = "similarity";

    /** 默认相似度阈值 */
    private static final double DEFAULT_THRESHOLD = 0.8;

    /**
     * 获取插件名称
     *
     * @return 插件名称 "similarity"
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
        return "相似度评估：计算实际输出与期望输出的文本相似度";
    }

    /**
     * 评估推理结果
     * <p>
     * 计算期望输出和实际输出之间的Jaccard相似度，
     * 并与阈值比较确定评估是否成功
     * </p>
     *
     * @param context 评估上下文，包含期望输出和实际输出
     * @return 评估结果，包含相似度得分和评估状态
     */
    @Override
    public EvaluationResult evaluate(EvaluationContext context) {
        String expected = context.getExpectedOutput();
        String actual = context.getActualOutput();

        if (expected == null || actual == null) {
            return EvaluationResult.builder()
                    .success(false)
                    .score(0)
                    .reason("输入为空")
                    .build();
        }

        double similarity = calculateJaccardSimilarity(expected, actual);

        double threshold = DEFAULT_THRESHOLD;
        if (context.getPluginConfig() != null) {
            Object thresholdObj = context.getPluginConfig().get("threshold");
            if (thresholdObj instanceof Number) {
                threshold = ((Number) thresholdObj).doubleValue();
            }
        }

        boolean success = similarity >= threshold;

        return EvaluationResult.builder()
                .success(success)
                .score(similarity)
                .reason(String.format("相似度: %.2f%%, 阈值: %.2f%%", similarity * 100, threshold * 100))
                .build();
    }

    /**
     * 计算两个字符串的Jaccard相似度
     * <p>
     * Jaccard相似度 = 交集大小 / 并集大小
     * </p>
     *
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 相似度值，范围0到1
     */
    private double calculateJaccardSimilarity(String s1, String s2) {
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        String[] words1 = s1.toLowerCase().split("\\s+");
        String[] words2 = s2.toLowerCase().split("\\s+");

        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }
}
