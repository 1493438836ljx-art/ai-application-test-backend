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

/**
 * 插件服务类，提供插件的业务逻辑处理
 * <p>
 * 负责插件的创建、查询、更新、删除以及内置插件的注册
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    /** 插件数据访问层 */
    private final PluginRepository pluginRepository;

    /** 插件对象映射器 */
    private final PluginMapper pluginMapper;

    /** 插件加载器 */
    private final PluginLoader pluginLoader;

    /**
     * 初始化方法，在Bean创建后自动注册内置插件
     */
    @PostConstruct
    public void init() {
        registerBuiltinPlugins();
    }

    /**
     * 注册所有内置插件到数据库
     */
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

    /**
     * 注册单个内置插件
     *
     * @param name 插件名称
     * @param description 插件描述
     * @param type 插件类型
     */
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

    /**
     * 创建新插件
     *
     * @param request 创建插件请求
     * @return 创建后的插件响应
     */
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

    /**
     * 分页查询插件列表
     *
     * @param type 插件类型（可选筛选条件）
     * @param pageable 分页参数
     * @return 插件响应分页结果
     */
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

    /**
     * 根据类型获取所有激活的插件
     *
     * @param type 插件类型
     * @return 插件响应列表
     */
    @Transactional(readOnly = true)
    public List<PluginResponse> getPluginsByType(PluginType type) {
        List<Plugin> plugins = pluginRepository.findByTypeAndIsActiveTrue(type);
        return pluginMapper.toResponseList(plugins);
    }

    /**
     * 根据ID获取插件详情
     *
     * @param id 插件ID
     * @return 插件响应
     */
    @Transactional(readOnly = true)
    public PluginResponse getPluginById(Long id) {
        Plugin plugin = findPluginById(id);
        return pluginMapper.toResponse(plugin);
    }

    /**
     * 更新插件信息
     *
     * @param id 插件ID
     * @param request 更新插件请求
     * @return 更新后的插件响应
     */
    @Transactional
    public PluginResponse updatePlugin(Long id, PluginUpdateRequest request) {
        Plugin plugin = findPluginById(id);
        pluginMapper.updateEntity(request, plugin);
        Plugin updated = pluginRepository.save(plugin);
        return pluginMapper.toResponse(updated);
    }

    /**
     * 删除插件
     * <p>
     * 内置插件不允许删除
     * </p>
     *
     * @param id 插件ID
     */
    @Transactional
    public void deletePlugin(Long id) {
        Plugin plugin = findPluginById(id);
        if (plugin.getIsBuiltin()) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "内置插件不能删除");
        }
        pluginRepository.delete(plugin);
    }

    /**
     * 根据ID查找插件，不存在则抛出异常
     *
     * @param id 插件ID
     * @return 插件实体
     */
    private Plugin findPluginById(Long id) {
        return pluginRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND));
    }
}
