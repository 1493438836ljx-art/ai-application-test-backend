package com.example.demo.plugin.loader;

import com.example.demo.common.enums.PluginType;
import com.example.demo.plugin.spi.EvaluationPlugin;
import com.example.demo.plugin.spi.ExecutionPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 插件加载器，负责管理和加载所有执行插件和评估插件
 * <p>
 * 在Spring容器初始化时自动扫描并注册所有实现了
 * ExecutionPlugin和EvaluationPlugin接口的插件
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class PluginLoader {

    /** 执行插件映射表，键为插件名称，值为插件实例 */
    private final Map<String, ExecutionPlugin> executionPlugins;

    /** 评估插件映射表，键为插件名称，值为插件实例 */
    private final Map<String, EvaluationPlugin> evaluationPlugins;

    /**
     * 构造函数，通过依赖注入获取所有插件并注册
     *
     * @param executionPluginList 所有执行插件的列表
     * @param evaluationPluginList 所有评估插件的列表
     */
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

    /**
     * 根据名称获取执行插件
     *
     * @param name 插件名称
     * @return 执行插件实例（可能为空）
     */
    public Optional<ExecutionPlugin> getExecutionPlugin(String name) {
        return Optional.ofNullable(executionPlugins.get(name));
    }

    /**
     * 根据名称获取评估插件
     *
     * @param name 插件名称
     * @return 评估插件实例（可能为空）
     */
    public Optional<EvaluationPlugin> getEvaluationPlugin(String name) {
        return Optional.ofNullable(evaluationPlugins.get(name));
    }

    /**
     * 获取所有执行插件
     *
     * @return 执行插件列表
     */
    public List<ExecutionPlugin> getAllExecutionPlugins() {
        return new ArrayList<>(executionPlugins.values());
    }

    /**
     * 获取所有评估插件
     *
     * @return 评估插件列表
     */
    public List<EvaluationPlugin> getAllEvaluationPlugins() {
        return new ArrayList<>(evaluationPlugins.values());
    }

    /**
     * 获取所有执行插件的名称列表
     *
     * @return 执行插件名称列表
     */
    public List<String> getExecutionPluginNames() {
        return new ArrayList<>(executionPlugins.keySet());
    }

    /**
     * 获取所有评估插件的名称列表
     *
     * @return 评估插件名称列表
     */
    public List<String> getEvaluationPluginNames() {
        return new ArrayList<>(evaluationPlugins.keySet());
    }
}
