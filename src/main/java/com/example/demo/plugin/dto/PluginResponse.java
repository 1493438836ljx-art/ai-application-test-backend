package com.example.demo.plugin.dto;

import com.example.demo.common.enums.PluginType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 插件响应DTO，用于返回插件信息的响应数据
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "插件响应")
public class PluginResponse {

    /** 插件ID */
    @Schema(description = "插件ID")
    private Long id;

    /** 插件名称 */
    @Schema(description = "插件名称")
    private String name;

    /** 插件描述 */
    @Schema(description = "插件描述")
    private String description;

    /** 插件类型 */
    @Schema(description = "插件类型")
    private PluginType type;

    /** 实现类名 */
    @Schema(description = "实现类名")
    private String className;

    /** 默认配置 */
    @Schema(description = "默认配置")
    private String defaultConfig;

    /** 是否内置插件 */
    @Schema(description = "是否内置插件")
    private Boolean isBuiltin;

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
