package com.example.demo.skill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Skill查询请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "Skill查询请求")
public class SkillQueryRequest {

    @Schema(description = "名称关键字（模糊查询）", example = "data")
    private String name;

    @Schema(description = "执行方式：AUTOMATED/AI", example = "AUTOMATED")
    private String executionType;

    @Schema(description = "分类：SYSTEM/USER", example = "USER")
    private String category;

    @Schema(description = "访问控制类型：PUBLIC/PRIVATE/WHITELIST/PROJECT", example = "PUBLIC")
    private String accessType;

    @Schema(description = "状态：PUBLISHED/DRAFT", example = "PUBLISHED")
    private String status;

    @Schema(description = "创建人", example = "admin")
    private String createdBy;

    @Schema(description = "是否容器", example = "false")
    private Boolean isContainer;
}
