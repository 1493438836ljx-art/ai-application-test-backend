package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Prompt渲染响应")
public class PromptRenderResponse {

    @Schema(description = "渲染后的Prompt内容")
    private String renderedContent;

    @Schema(description = "缺失的变量名列表")
    private java.util.List<String> missingVariables;
}
