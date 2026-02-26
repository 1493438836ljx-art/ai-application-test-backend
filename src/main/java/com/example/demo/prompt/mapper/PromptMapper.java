package com.example.demo.prompt.mapper;

import com.example.demo.prompt.dto.PromptCreateRequest;
import com.example.demo.prompt.dto.PromptResponse;
import com.example.demo.prompt.dto.PromptUpdateRequest;
import com.example.demo.prompt.entity.Prompt;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Prompt对象映射器
 * <p>
 * 使用MapStruct实现DTO与实体类之间的转换
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromptMapper {

    /**
     * 创建请求DTO转实体
     *
     * @param request 创建请求
     * @return Prompt实体
     */
    Prompt toEntity(PromptCreateRequest request);

    /**
     * 实体转响应DTO
     *
     * @param prompt Prompt实体
     * @return 响应DTO
     */
    @Mapping(target = "variableNames", expression = "java(parseVariableNames(prompt.getVariableNames()))")
    PromptResponse toResponse(Prompt prompt);

    /**
     * 实体列表转响应DTO列表
     *
     * @param prompts Prompt实体列表
     * @return 响应DTO列表
     */
    List<PromptResponse> toResponseList(List<Prompt> prompts);

    /**
     * 更新请求DTO更新实体
     *
     * @param request 更新请求
     * @param prompt  目标实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PromptUpdateRequest request, @MappingTarget Prompt prompt);

    /**
     * 解析变量名字符串为列表
     *
     * @param variableNames 变量名字符串（逗号分隔）
     * @return 变量名列表
     */
    default List<String> parseVariableNames(String variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(variableNames.split(","));
    }
}
