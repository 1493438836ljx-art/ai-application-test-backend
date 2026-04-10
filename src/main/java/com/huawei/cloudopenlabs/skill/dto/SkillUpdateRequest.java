/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.skill.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill更新请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class SkillUpdateRequest {

    @Size(max = 100, message = "Skill名称长度不能超过100")
    private String name;

    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;

    private String executionType;

    private String category;

    private String accessType;

    private Boolean isContainer;

    private Boolean allowAddInputParams;

    private Boolean allowAddOutputParams;

    private String updatedBy;

    @Valid
    private List<InputParameterDTO> inputParameters = new ArrayList<>();

    @Valid
    private List<OutputParameterDTO> outputParameters = new ArrayList<>();

    /**
     * 入参DTO
     */
    @Data
    public static class InputParameterDTO {
        @Size(max = 50, message = "参数类型长度不能超过50")
        private String paramType;

        @Size(max = 100, message = "参数名称长度不能超过100")
        private String paramName;

        @Size(max = 1000, message = "默认值长度不能超过1000")
        private String defaultValue;

        @Size(max = 500, message = "参数描述长度不能超过500")
        private String description;

        private Boolean required;
    }

    /**
     * 出参DTO
     */
    @Data
    public static class OutputParameterDTO {
        @Size(max = 50, message = "参数类型长度不能超过50")
        private String paramType;

        @Size(max = 100, message = "参数名称长度不能超过100")
        private String paramName;

        @Size(max = 500, message = "参数描述长度不能超过500")
        private String description;

        private Boolean required;
    }
}
