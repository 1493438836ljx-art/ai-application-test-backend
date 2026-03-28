package com.example.demo.skill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill更新请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "Skill更新请求")
public class SkillUpdateRequest {

    @Size(max = 100, message = "Skill名称长度不能超过100")
    @Schema(description = "Skill名称", example = "data-processor-v2")
    private String name;

    @Size(max = 2000, message = "描述长度不能超过2000")
    @Schema(description = "Skill描述", example = "更新后的数据处理技能")
    private String description;

    @Schema(description = "执行方式：AUTOMATED/AI", example = "AUTOMATED")
    private String executionType;

    @Schema(description = "分类：SYSTEM/USER", example = "USER")
    private String category;

    @Schema(description = "访问控制类型：PUBLIC/PRIVATE/WHITELIST/PROJECT", example = "PUBLIC")
    private String accessType;

    @Schema(description = "是否容器", example = "false")
    private Boolean isContainer;

    @Schema(description = "是否支持增加入参", example = "false")
    private Boolean allowAddInputParams;

    @Schema(description = "是否支持增加出参", example = "false")
    private Boolean allowAddOutputParams;

    @Schema(description = "更新人", example = "admin")
    private String updatedBy;

    @Valid
    @Schema(description = "入参列表")
    private List<InputParameterDTO> inputParameters = new ArrayList<>();

    @Valid
    @Schema(description = "出参列表")
    private List<OutputParameterDTO> outputParameters = new ArrayList<>();

    /**
     * 入参DTO
     */
    @Data
    @Schema(description = "入参信息")
    public static class InputParameterDTO {
        @Size(max = 50, message = "参数类型长度不能超过50")
        @Schema(description = "参数类型", example = "String")
        private String paramType;

        @Size(max = 100, message = "参数名称长度不能超过100")
        @Schema(description = "参数名称", example = "inputFile")
        private String paramName;

        @Size(max = 1000, message = "默认值长度不能超过1000")
        @Schema(description = "默认值", example = "")
        private String defaultValue;

        @Size(max = 500, message = "参数描述长度不能超过500")
        @Schema(description = "参数描述", example = "输入文件路径")
        private String description;

        @Schema(description = "是否必填", example = "true")
        private Boolean required;
    }

    /**
     * 出参DTO
     */
    @Data
    @Schema(description = "出参信息")
    public static class OutputParameterDTO {
        @Size(max = 50, message = "参数类型长度不能超过50")
        @Schema(description = "参数类型", example = "Array")
        private String paramType;

        @Size(max = 100, message = "参数名称长度不能超过100")
        @Schema(description = "参数名称", example = "result")
        private String paramName;

        @Size(max = 500, message = "参数描述长度不能超过500")
        @Schema(description = "参数描述", example = "处理结果数组")
        private String description;

        @Schema(description = "是否必填", example = "true")
        private Boolean required;
    }
}
