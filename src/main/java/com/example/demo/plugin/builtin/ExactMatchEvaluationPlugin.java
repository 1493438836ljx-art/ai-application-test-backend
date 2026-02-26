package com.example.demo.plugin.builtin;

import com.example.demo.plugin.spi.EvaluationContext;
import com.example.demo.plugin.spi.EvaluationResult;
import com.example.demo.plugin.spi.EvaluationPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExactMatchEvaluationPlugin implements EvaluationPlugin {

    public static final String NAME = "exact-match";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "精确匹配评估：比较实际输出与期望输出是否完全一致";
    }

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
