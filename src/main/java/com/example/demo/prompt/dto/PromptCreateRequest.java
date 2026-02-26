package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建Prompt请求DTO
 *
 * 用于接收创建新Prompt的请求参数，包含Prompt的基本信息
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "创建Prompt请求")
public class PromptCreateRequest {

    /** Prompt名称，用于标识和描述该Prompt的用途 */
    @NotBlank(message = "Prompt名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "Prompt名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /** Prompt描述，详细说明该Prompt的功能和使用场景 */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "Prompt描述")
    private String description;

    /** Prompt模板内容，支持变量占位符如 {{variable}} 格式 */
    @NotBlank(message = "Prompt模板不能为空")
    @Schema(description = "Prompt模板内容，支持变量占位符如 {{variable}}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String template;
}
