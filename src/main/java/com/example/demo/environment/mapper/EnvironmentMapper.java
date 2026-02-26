package com.example.demo.environment.mapper;

import com.example.demo.environment.dto.EnvironmentCreateRequest;
import com.example.demo.environment.dto.EnvironmentResponse;
import com.example.demo.environment.dto.EnvironmentUpdateRequest;
import com.example.demo.environment.entity.Environment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EnvironmentMapper {

    Environment toEntity(EnvironmentCreateRequest request);

    EnvironmentResponse toResponse(Environment environment);

    List<EnvironmentResponse> toResponseList(List<Environment> environments);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(EnvironmentUpdateRequest request, @MappingTarget Environment environment);
}
