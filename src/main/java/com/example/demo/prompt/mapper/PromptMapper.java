package com.example.demo.prompt.mapper;

import com.example.demo.prompt.dto.PromptCreateRequest;
import com.example.demo.prompt.dto.PromptResponse;
import com.example.demo.prompt.dto.PromptUpdateRequest;
import com.example.demo.prompt.entity.Prompt;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromptMapper {

    Prompt toEntity(PromptCreateRequest request);

    @Mapping(target = "variableNames", expression = "java(parseVariableNames(prompt.getVariableNames()))")
    PromptResponse toResponse(Prompt prompt);

    List<PromptResponse> toResponseList(List<Prompt> prompts);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PromptUpdateRequest request, @MappingTarget Prompt prompt);

    default List<String> parseVariableNames(String variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(variableNames.split(","));
    }
}
