package com.example.demo.plugin.service;

import com.example.demo.common.enums.PluginType;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.plugin.dto.*;
import com.example.demo.plugin.entity.Plugin;
import com.example.demo.plugin.loader.PluginLoader;
import com.example.demo.plugin.mapper.PluginMapper;
import com.example.demo.plugin.repository.PluginRepository;
import com.example.demo.plugin.spi.EvaluationPlugin;
import com.example.demo.plugin.spi.ExecutionPlugin;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginRepository pluginRepository;
    private final PluginMapper pluginMapper;
    private final PluginLoader pluginLoader;

    @PostConstruct
    public void init() {
        registerBuiltinPlugins();
    }

    private void registerBuiltinPlugins() {
        List<ExecutionPlugin> executionPlugins = pluginLoader.getAllExecutionPlugins();
        for (ExecutionPlugin plugin : executionPlugins) {
            registerBuiltinPlugin(plugin.getName(), plugin.getDescription(), PluginType.EXECUTION);
        }

        List<EvaluationPlugin> evaluationPlugins = pluginLoader.getAllEvaluationPlugins();
        for (EvaluationPlugin plugin : evaluationPlugins) {
            registerBuiltinPlugin(plugin.getName(), plugin.getDescription(), PluginType.EVALUATION);
        }
    }

    private void registerBuiltinPlugin(String name, String description, PluginType type) {
        if (!pluginRepository.existsByName(name)) {
            Plugin plugin = Plugin.builder()
                    .name(name)
                    .description(description)
                    .type(type)
                    .isBuiltin(true)
                    .isActive(true)
                    .build();
            pluginRepository.save(plugin);
            log.info("Registered built-in plugin: {}", name);
        }
    }

    @Transactional
    public PluginResponse createPlugin(PluginCreateRequest request) {
        Plugin plugin = pluginMapper.toEntity(request);
        if (plugin.getIsBuiltin() == null) {
            plugin.setIsBuiltin(false);
        }
        if (plugin.getIsActive() == null) {
            plugin.setIsActive(true);
        }
        Plugin saved = pluginRepository.save(plugin);
        return pluginMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PluginResponse> getPlugins(PluginType type, Pageable pageable) {
        Page<Plugin> plugins;
        if (type != null) {
            plugins = pluginRepository.findAll((root, query, cb) -> cb.equal(root.get("type"), type), pageable);
        } else {
            plugins = pluginRepository.findAll(pageable);
        }
        return plugins.map(pluginMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PluginResponse> getPluginsByType(PluginType type) {
        List<Plugin> plugins = pluginRepository.findByTypeAndIsActiveTrue(type);
        return pluginMapper.toResponseList(plugins);
    }

    @Transactional(readOnly = true)
    public PluginResponse getPluginById(Long id) {
        Plugin plugin = findPluginById(id);
        return pluginMapper.toResponse(plugin);
    }

    @Transactional
    public PluginResponse updatePlugin(Long id, PluginUpdateRequest request) {
        Plugin plugin = findPluginById(id);
        pluginMapper.updateEntity(request, plugin);
        Plugin updated = pluginRepository.save(plugin);
        return pluginMapper.toResponse(updated);
    }

    @Transactional
    public void deletePlugin(Long id) {
        Plugin plugin = findPluginById(id);
        if (plugin.getIsBuiltin()) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "内置插件不能删除");
        }
        pluginRepository.delete(plugin);
    }

    private Plugin findPluginById(Long id) {
        return pluginRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND));
    }
}
