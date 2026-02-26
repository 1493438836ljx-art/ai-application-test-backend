package com.example.demo.environment.dto;

import com.example.demo.common.enums.EnvironmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "环境响应")
public class EnvironmentResponse {

    @Schema(description = "环境ID")
    private Long id;

    @Schema(description = "环境名称")
    private String name;

    @Schema(description = "环境描述")
    private String description;

    @Schema(description = "环境类型")
    private EnvironmentType type;

    @Schema(description = "环境配置")
    private String config;

    @Schema(description = "API端点")
    private String apiEndpoint;

    @Schema(description = "认证类型")
    private String authType;

    @Schema(description = "认证配置")
    private String authConfig;

    @Schema(description = "是否激活")
    private Boolean isActive;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
