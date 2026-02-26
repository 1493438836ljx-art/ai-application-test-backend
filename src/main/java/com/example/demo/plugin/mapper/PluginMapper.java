package com.example.demo.plugin.mapper;

import com.example.demo.plugin.dto.PluginCreateRequest;
import com.example.demo.plugin.dto.PluginResponse;
import com.example.demo.plugin.dto.PluginUpdateRequest;
import com.example.demo.plugin.entity.Plugin;
import org.mapstruct.*;

import java.util.List;

/**
 * 插件对象映射器，用于DTO与实体之间的转换
 * <p>
 * 使用MapStruct框架实现对象之间的自动映射
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PluginMapper {

    /**
     * 将创建请求DTO转换为实体对象
     *
     * @param request 创建插件请求
     * @return 插件实体
     */
    Plugin toEntity(PluginCreateRequest request);

    /**
     * 将实体对象转换为响应DTO
     *
     * @param plugin 插件实体
     * @return 插件响应DTO
     */
    PluginResponse toResponse(Plugin plugin);

    /**
     * 将实体列表转换为响应DTO列表
     *
     * @param plugins 插件实体列表
     * @return 插件响应DTO列表
     */
    List<PluginResponse> toResponseList(List<Plugin> plugins);

    /**
     * 使用更新请求DTO更新实体对象
     * <p>
     * 仅更新非空字段，忽略null值
     * </p>
     *
     * @param request 更新插件请求
     * @param plugin 待更新的插件实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PluginUpdateRequest request, @MappingTarget Plugin plugin);
}
