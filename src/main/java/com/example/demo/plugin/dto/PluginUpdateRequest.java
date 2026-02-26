package com.example.demo.plugin.dto;

import com.example.demo.common.enums.PluginType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新插件请求")
public class PluginUpdateRequest {

    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "插件名称")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "插件描述")
    private String description;

    @Schema(description = "插件类型")
    private PluginType type;

    @Schema(description = "默认配置(JSON格式)")
    private String defaultConfig;

    @Schema(description = "是否激活")
    private Boolean isActive;
}
