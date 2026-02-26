package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Prompt渲染请求DTO
 *
 * 用于接收Prompt渲染的请求参数，将变量值注入到Prompt模板中生成最终内容
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Prompt渲染请求")
public class PromptRenderRequest {

    /** 变量值映射，key为变量名，value为变量值，用于替换模板中的占位符 */
    @Schema(description = "变量值映射，key为变量名，value为变量值")
    private Map<String, Object> variables;
}
