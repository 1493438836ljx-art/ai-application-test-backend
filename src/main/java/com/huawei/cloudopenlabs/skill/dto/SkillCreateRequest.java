/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.skill.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill创建请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class SkillCreateRequest {

    @NotBlank(message = "Skill名称不能为空")
    @Size(max = 100, message = "Skill名称长度不能超过100")
    private String name;

    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;

    @Size(max = 500, message = "套件路径长度不能超过500")
    private String suitePath;

    @NotBlank(message = "执行方式不能为空")
    private String executionType;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "访问类型不能为空")
    private String accessType;

    @NotNull(message = "是否容器不能为空")
    private Boolean isContainer;

    private Boolean allowAddInputParams;

    private Boolean allowAddOutputParams;

    private String createdBy;

    @Valid
    private List<InputParameterDTO> inputParameters = new ArrayList<>();

    @Valid
    private List<OutputParameterDTO> outputParameters = new ArrayList<>();

    @Valid
    private List<AccessControlDTO> accessControls = new ArrayList<>();

    /**
     * 入参DTO
     */
    @Data
    public static class InputParameterDTO {
        private Integer paramOrder;

        @Size(max = 50, message = "参数类型长度不能超过50")
        private String paramType;

        @Size(max = 100, message = "参数名称长度不能超过100")
        private String paramName;

        @Size(max = 1000, message = "默认值长度不能超过1000")
        private String defaultValue;

        @Size(max = 500, message = "参数描述长度不能超过500")
        private String description;

        @NotNull(message = "是否必填不能为空")
        private Boolean required = true;
    }

    /**
     * 出参DTO
     */
    @Data
    public static class OutputParameterDTO {
        private Integer paramOrder;

        @Size(max = 50, message = "参数类型长度不能超过50")
        private String paramType;

        @Size(max = 100, message = "参数名称长度不能超过100")
        private String paramName;

        @Size(max = 500, message = "参数描述长度不能超过500")
        private String description;

        @NotNull(message = "是否必填不能为空")
        private Boolean required = true;
    }

    /**
     * 访问控制DTO
     */
    @Data
    public static class AccessControlDTO {
        @NotBlank(message = "目标类型不能为空")
        private String targetType;

        @NotBlank(message = "目标ID不能为空")
        private String targetId;
    }
}
