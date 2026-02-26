package com.example.demo.plugin.mapper;

import com.example.demo.plugin.dto.PluginCreateRequest;
import com.example.demo.plugin.dto.PluginResponse;
import com.example.demo.plugin.dto.PluginUpdateRequest;
import com.example.demo.plugin.entity.Plugin;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PluginMapper {

    Plugin toEntity(PluginCreateRequest request);

    PluginResponse toResponse(Plugin plugin);

    List<PluginResponse> toResponseList(List<Plugin> plugins);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PluginUpdateRequest request, @MappingTarget Plugin plugin);
}
