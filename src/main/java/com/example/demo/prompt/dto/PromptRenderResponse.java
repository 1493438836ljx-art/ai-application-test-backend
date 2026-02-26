package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Prompt渲染响应DTO
 *
 * 用于返回Prompt渲染后的结果，包括渲染内容和缺失的变量信息
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Prompt渲染响应")
public class PromptRenderResponse {

    /** 渲染后的Prompt内容，模板中的变量占位符已被实际值替换 */
    @Schema(description = "渲染后的Prompt内容")
    private String renderedContent;

    /** 缺失的变量名列表，记录模板中存在但未提供值的变量 */
    @Schema(description = "缺失的变量名列表")
    private List<String> missingVariables;
}
