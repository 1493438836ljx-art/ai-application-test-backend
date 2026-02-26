package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新Prompt请求DTO
 *
 * 用于接收更新Prompt的请求参数，所有字段均为可选，仅更新传入的字段
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "更新Prompt请求")
public class PromptUpdateRequest {

    /** Prompt名称，用于标识和描述该Prompt的用途 */
    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "Prompt名称")
    private String name;

    /** Prompt描述，详细说明该Prompt的功能和使用场景 */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "Prompt描述")
    private String description;

    /** Prompt模板内容，支持变量占位符如 {{variable}} 格式 */
    @Schema(description = "Prompt模板内容")
    private String template;
}
