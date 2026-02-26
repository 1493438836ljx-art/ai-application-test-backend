package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新Prompt请求")
public class PromptUpdateRequest {

    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "Prompt名称")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "Prompt描述")
    private String description;

    @Schema(description = "Prompt模板内容")
    private String template;
}
