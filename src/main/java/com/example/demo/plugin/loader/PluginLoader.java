package com.example.demo.plugin.loader;

import com.example.demo.common.enums.PluginType;
import com.example.demo.plugin.spi.EvaluationPlugin;
import com.example.demo.plugin.spi.ExecutionPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class PluginLoader {

    private final Map<String, ExecutionPlugin> executionPlugins;
    private final Map<String, EvaluationPlugin> evaluationPlugins;

    public PluginLoader(List<ExecutionPlugin> executionPluginList, List<EvaluationPlugin> evaluationPluginList) {
        this.executionPlugins = new HashMap<>();
        this.evaluationPlugins = new HashMap<>();

        for (ExecutionPlugin plugin : executionPluginList) {
            executionPlugins.put(plugin.getName(), plugin);
            log.info("Registered execution plugin: {}", plugin.getName());
        }

        for (EvaluationPlugin plugin : evaluationPluginList) {
            evaluationPlugins.put(plugin.getName(), plugin);
            log.info("Registered evaluation plugin: {}", plugin.getName());
        }
    }

    public Optional<ExecutionPlugin> getExecutionPlugin(String name) {
        return Optional.ofNullable(executionPlugins.get(name));
    }

    public Optional<EvaluationPlugin> getEvaluationPlugin(String name) {
        return Optional.ofNullable(evaluationPlugins.get(name));
    }

    public List<ExecutionPlugin> getAllExecutionPlugins() {
        return new ArrayList<>(executionPlugins.values());
    }

    public List<EvaluationPlugin> getAllEvaluationPlugins() {
        return new ArrayList<>(evaluationPlugins.values());
    }

    public List<String> getExecutionPluginNames() {
        return new ArrayList<>(executionPlugins.keySet());
    }

    public List<String> getEvaluationPluginNames() {
        return new ArrayList<>(evaluationPlugins.keySet());
    }
}
