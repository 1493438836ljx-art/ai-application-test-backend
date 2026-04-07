package com.huawei.cloudopenlabs.skill.dto;

import com.example.demo.skill.entity.*;
import com.huawei.cloudopenlabs.skill.entity.SkillAccessType;
import com.huawei.cloudopenlabs.skill.entity.SkillCategory;
import com.huawei.cloudopenlabs.skill.entity.SkillExecutionType;
import com.huawei.cloudopenlabs.skill.entity.SkillStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class SkillResponse {

    private String id;

    private String name;

    private String description;

    private String suitePath;

    private String suiteFilename;

    private SkillExecutionType executionType;

    private SkillCategory category;

    private SkillAccessType accessType;

    private Boolean isContainer;

    private Boolean allowAddInputParams;

    private Boolean allowAddOutputParams;

    private SkillStatus status;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private List<InputParameterDTO> inputParameters;

    private List<OutputParameterDTO> outputParameters;

    private Integer inputParamCount;

    private Integer outputParamCount;

    private List<AccessControlDTO> accessControls;

    /**
     * 入参DTO
     */
    @Data
    public static class InputParameterDTO {
        private String id;

        private Integer paramOrder;

        private String paramType;

        private String paramName;

        private String defaultValue;

        private String description;

        private Boolean required;
    }

    /**
     * 出参DTO
     */
    @Data
    public static class OutputParameterDTO {
        private String id;

        private Integer paramOrder;

        private String paramType;

        private String paramName;

        private String description;

        private Boolean required;
    }

    /**
     * 访问控制DTO
     */
    @Data
    public static class AccessControlDTO {
        private String id;

        private String targetType;

        private String targetId;
    }
}
