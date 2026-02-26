package com.example.demo.environment.dto;

import com.example.demo.common.enums.EnvironmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新环境请求")
public class EnvironmentUpdateRequest {

    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "环境名称")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "环境描述")
    private String description;

    @Schema(description = "环境类型")
    private EnvironmentType type;

    @Schema(description = "环境配置(JSON格式)")
    private String config;

    @Size(max = 500, message = "API端点长度不能超过500个字符")
    @Schema(description = "API端点URL")
    private String apiEndpoint;

    @Size(max = 50, message = "认证类型长度不能超过50个字符")
    @Schema(description = "认证类型")
    private String authType;

    @Schema(description = "认证配置(JSON格式)")
    private String authConfig;

    @Schema(description = "是否激活")
    private Boolean isActive;
}
