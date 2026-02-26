package com.example.demo.plugin.spi;

public interface EvaluationPlugin {

    String getName();

    String getDescription();

    EvaluationResult evaluate(EvaluationContext context);
}
