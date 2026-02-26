package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建Prompt请求")
public class PromptCreateRequest {

    @NotBlank(message = "Prompt名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "Prompt名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "Prompt描述")
    private String description;

    @NotBlank(message = "Prompt模板不能为空")
    @Schema(description = "Prompt模板内容，支持变量占位符如 {{variable}}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String template;
}
