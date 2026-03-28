package com.example.demo.skill.dto;

import com.example.demo.skill.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "Skill响应")
public class SkillResponse {

    @Schema(description = "Skill ID")
    private String id;

    @Schema(description = "Skill名称")
    private String name;

    @Schema(description = "Skill描述")
    private String description;

    @Schema(description = "可执行套件文件路径")
    private String suitePath;

    @Schema(description = "可执行套件文件名（带后缀）")
    private String suiteFilename;

    @Schema(description = "执行方式")
    private SkillExecutionType executionType;

    @Schema(description = "分类")
    private SkillCategory category;

    @Schema(description = "访问控制类型")
    private SkillAccessType accessType;

    @Schema(description = "是否容器")
    private Boolean isContainer;

    @Schema(description = "是否支持增加入参")
    private Boolean allowAddInputParams;

    @Schema(description = "是否支持增加出参")
    private Boolean allowAddOutputParams;

    @Schema(description = "状态")
    private SkillStatus status;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新人")
    private String updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "入参列表")
    private List<InputParameterDTO> inputParameters;

    @Schema(description = "出参列表")
    private List<OutputParameterDTO> outputParameters;

    @Schema(description = "入参数量")
    private Integer inputParamCount;

    @Schema(description = "出参数量")
    private Integer outputParamCount;

    @Schema(description = "访问控制列表")
    private List<AccessControlDTO> accessControls;

    /**
     * 入参DTO
     */
    @Data
    @Schema(description = "入参信息")
    public static class InputParameterDTO {
        @Schema(description = "参数ID")
        private String id;

        @Schema(description = "参数顺序")
        private Integer paramOrder;

        @Schema(description = "参数类型")
        private String paramType;

        @Schema(description = "参数名称")
        private String paramName;

        @Schema(description = "默认值")
        private String defaultValue;

        @Schema(description = "参数描述")
        private String description;

        @Schema(description = "是否必填")
        private Boolean required;
    }

    /**
     * 出参DTO
     */
    @Data
    @Schema(description = "出参信息")
    public static class OutputParameterDTO {
        @Schema(description = "参数ID")
        private String id;

        @Schema(description = "参数顺序")
        private Integer paramOrder;

        @Schema(description = "参数类型")
        private String paramType;

        @Schema(description = "参数名称")
        private String paramName;

        @Schema(description = "参数描述")
        private String description;

        @Schema(description = "是否必填")
        private Boolean required;
    }

    /**
     * 访问控制DTO
     */
    @Data
    @Schema(description = "访问控制信息")
    public static class AccessControlDTO {
        @Schema(description = "访问控制ID")
        private String id;

        @Schema(description = "目标类型")
        private String targetType;

        @Schema(description = "目标ID")
        private String targetId;
    }
}
