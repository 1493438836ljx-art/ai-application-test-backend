package com.example.demo.plugin.spi;

public interface ExecutionPlugin {

    String getName();

    String getDescription();

    ExecutionResult execute(ExecutionContext context);
}
