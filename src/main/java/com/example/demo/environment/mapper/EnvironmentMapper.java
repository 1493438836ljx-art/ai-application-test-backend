package com.example.demo.environment.mapper;

import com.example.demo.environment.dto.EnvironmentCreateRequest;
import com.example.demo.environment.dto.EnvironmentResponse;
import com.example.demo.environment.dto.EnvironmentUpdateRequest;
import com.example.demo.environment.entity.Environment;
import org.mapstruct.*;

import java.util.List;

/**
 * 环境对象映射器接口
 * <p>
 * 提供环境实体与DTO之间的转换方法，基于MapStruct实现
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EnvironmentMapper {

    /**
     * 将创建请求DTO转换为实体对象
     *
     * @param request 创建环境请求DTO
     * @return 环境实体对象
     */
    Environment toEntity(EnvironmentCreateRequest request);

    /**
     * 将实体对象转换为响应DTO
     *
     * @param environment 环境实体对象
     * @return 环境响应DTO
     */
    EnvironmentResponse toResponse(Environment environment);

    /**
     * 将实体列表转换为响应DTO列表
     *
     * @param environments 环境实体列表
     * @return 环境响应DTO列表
     */
    List<EnvironmentResponse> toResponseList(List<Environment> environments);

    /**
     * 使用更新请求DTO更新实体对象
     * <p>
     * 仅更新非空字段，忽略空值
     * </p>
     *
     * @param request 更新环境请求DTO
     * @param environment 待更新的环境实体对象（映射目标）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(EnvironmentUpdateRequest request, @MappingTarget Environment environment);
}
