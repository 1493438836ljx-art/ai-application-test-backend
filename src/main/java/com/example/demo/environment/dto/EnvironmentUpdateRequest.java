package com.example.demo.environment.dto;

import com.example.demo.common.enums.EnvironmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新环境请求DTO
 * <p>
 * 用于接收更新环境的请求参数，所有字段均为可选
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "更新环境请求")
public class EnvironmentUpdateRequest {

    /** 环境名称 */
    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "环境名称")
    private String name;

    /** 环境描述 */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "环境描述")
    private String description;

    /** 环境类型 */
    @Schema(description = "环境类型")
    private EnvironmentType type;

    /** 环境配置（JSON格式） */
    @Schema(description = "环境配置(JSON格式)")
    private String config;

    /** API端点URL */
    @Size(max = 500, message = "API端点长度不能超过500个字符")
    @Schema(description = "API端点URL")
    private String apiEndpoint;

    /** 认证类型 */
    @Size(max = 50, message = "认证类型长度不能超过50个字符")
    @Schema(description = "认证类型")
    private String authType;

    /** 认证配置（JSON格式） */
    @Schema(description = "认证配置(JSON格式)")
    private String authConfig;

    /** 是否激活 */
    @Schema(description = "是否激活")
    private Boolean isActive;
}
