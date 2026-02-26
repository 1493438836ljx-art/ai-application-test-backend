package com.example.demo.plugin.dto;

import com.example.demo.common.enums.PluginType;
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
@Schema(description = "插件响应")
public class PluginResponse {

    @Schema(description = "插件ID")
    private Long id;

    @Schema(description = "插件名称")
    private String name;

    @Schema(description = "插件描述")
    private String description;

    @Schema(description = "插件类型")
    private PluginType type;

    @Schema(description = "实现类名")
    private String className;

    @Schema(description = "默认配置")
    private String defaultConfig;

    @Schema(description = "是否内置插件")
    private Boolean isBuiltin;

    @Schema(description = "是否激活")
    private Boolean isActive;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
