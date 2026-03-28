package com.example.demo.skill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "Skill创建请求")
public class SkillCreateRequest {

    @NotBlank(message = "Skill名称不能为空")
    @Size(max = 100, message = "Skill名称长度不能超过100")
    @Schema(description = "Skill名称", example = "data-processor")
    private String name;

    @Size(max = 2000, message = "描述长度不能超过2000")
    @Schema(description = "Skill描述", example = "数据处理技能，用于Excel文件的读取和转换")
    private String description;

    @Size(max = 500, message = "套件路径长度不能超过500")
    @Schema(description = "可执行套件文件路径（由后端自动设置）", hidden = true)
    private String suitePath;

    @NotBlank(message = "执行方式不能为空")
    @Schema(description = "执行方式：AUTOMATED/AI", example = "AUTOMATED")
    private String executionType;

    @NotBlank(message = "分类不能为空")
    @Schema(description = "分类：SYSTEM/USER", example = "USER")
    private String category;

    @NotBlank(message = "访问类型不能为空")
    @Schema(description = "访问控制类型：PUBLIC/PRIVATE/WHITELIST/PROJECT", example = "PRIVATE")
    private String accessType;

    @NotNull(message = "是否容器不能为空")
    @Schema(description = "是否容器", example = "false")
    private Boolean isContainer;

    @Schema(description = "是否支持增加入参", example = "false")
    private Boolean allowAddInputParams;

    @Schema(description = "是否支持增加出参", example = "false")
    private Boolean allowAddOutputParams;

    @Schema(description = "创建人", example = "admin")
    private String createdBy;

    @Valid
    @Schema(description = "入参列表")
    private List<InputParameterDTO> inputParameters = new ArrayList<>();

    @Valid
    @Schema(description = "出参列表")
    private List<OutputParameterDTO> outputParameters = new ArrayList<>();

    @Valid
    @Schema(description = "访问控制列表（当accessType为WHITELIST或PROJECT时使用）")
    private List<AccessControlDTO> accessControls = new ArrayList<>();

    /**
     * 入参DTO
     */
    @Data
    @Schema(description = "入参信息")
    public static class InputParameterDTO {
        @Schema(description = "参数顺序（从1开始，由后端自动分配）")
        private Integer paramOrder;

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

        @NotNull(message = "是否必填不能为空")
        @Schema(description = "是否必填", example = "true")
        private Boolean required = true;
    }

    /**
     * 出参DTO
     */
    @Data
    @Schema(description = "出参信息")
    public static class OutputParameterDTO {
        @Schema(description = "参数顺序（从1开始，由后端自动分配）")
        private Integer paramOrder;

        @Size(max = 50, message = "参数类型长度不能超过50")
        @Schema(description = "参数类型", example = "Array")
        private String paramType;

        @Size(max = 100, message = "参数名称长度不能超过100")
        @Schema(description = "参数名称", example = "result")
        private String paramName;

        @Size(max = 500, message = "参数描述长度不能超过500")
        @Schema(description = "参数描述", example = "处理结果数组")
        private String description;

        @NotNull(message = "是否必填不能为空")
        @Schema(description = "是否必填", example = "true")
        private Boolean required = true;
    }

    /**
     * 访问控制DTO
     */
    @Data
    @Schema(description = "访问控制信息")
    public static class AccessControlDTO {
        @NotBlank(message = "目标类型不能为空")
        @Schema(description = "目标类型：USER/PROJECT", example = "USER")
        private String targetType;

        @NotBlank(message = "目标ID不能为空")
        @Schema(description = "目标ID（用户ID或项目ID）", example = "user-123")
        private String targetId;
    }
}
