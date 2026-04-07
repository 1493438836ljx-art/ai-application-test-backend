package com.huawei.cloudopenlabs.skill.dto;

import lombok.Data;

/**
 * Skill查询请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class SkillQueryRequest {

    private String name;

    private String executionType;

    private String category;

    private String accessType;

    private String status;

    private String createdBy;

    private Boolean isContainer;
}
