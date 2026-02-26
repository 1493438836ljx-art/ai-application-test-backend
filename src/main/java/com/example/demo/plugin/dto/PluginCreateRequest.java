package com.example.demo.plugin.dto;

import com.example.demo.common.enums.PluginType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建插件请求")
public class PluginCreateRequest {

    @NotBlank(message = "插件名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "插件名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "插件描述")
    private String description;

    @NotNull(message = "插件类型不能为空")
    @Schema(description = "插件类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private PluginType type;

    @Size(max = 500, message = "类名长度不能超过500个字符")
    @Schema(description = "插件实现类名")
    private String className;

    @Schema(description = "默认配置(JSON格式)")
    private String defaultConfig;
}
