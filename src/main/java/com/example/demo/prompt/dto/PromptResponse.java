package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Prompt响应")
public class PromptResponse {

    @Schema(description = "Prompt ID")
    private Long id;

    @Schema(description = "Prompt名称")
    private String name;

    @Schema(description = "Prompt描述")
    private String description;

    @Schema(description = "Prompt模板内容")
    private String template;

    @Schema(description = "变量名称列表")
    private List<String> variableNames;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
