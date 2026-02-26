package com.example.demo.plugin.builtin;

import com.example.demo.plugin.spi.EvaluationContext;
import com.example.demo.plugin.spi.EvaluationResult;
import com.example.demo.plugin.spi.EvaluationPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimilarityEvaluationPlugin implements EvaluationPlugin {

    public static final String NAME = "similarity";

    private static final double DEFAULT_THRESHOLD = 0.8;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "相似度评估：计算实际输出与期望输出的文本相似度";
    }

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
