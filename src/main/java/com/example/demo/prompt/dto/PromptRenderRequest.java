package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Prompt渲染请求")
public class PromptRenderRequest {

    @Schema(description = "变量值映射，key为变量名，value为变量值")
    private Map<String, Object> variables;
}
