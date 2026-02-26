package com.example.demo.environment.dto;

import com.example.demo.common.enums.EnvironmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 环境响应DTO
 * <p>
 * 用于返回环境详细信息的响应数据
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "环境响应")
public class EnvironmentResponse {

    /** 环境ID */
    @Schema(description = "环境ID")
    private Long id;

    /** 环境名称 */
    @Schema(description = "环境名称")
    private String name;

    /** 环境描述 */
    @Schema(description = "环境描述")
    private String description;

    /** 环境类型 */
    @Schema(description = "环境类型")
    private EnvironmentType type;

    /** 环境配置 */
    @Schema(description = "环境配置")
    private String config;

    /** API端点 */
    @Schema(description = "API端点")
    private String apiEndpoint;

    /** 认证类型 */
    @Schema(description = "认证类型")
    private String authType;

    /** 认证配置 */
    @Schema(description = "认证配置")
    private String authConfig;

    /** 是否激活 */
    @Schema(description = "是否激活")
    private Boolean isActive;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
