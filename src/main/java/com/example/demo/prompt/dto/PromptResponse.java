package com.example.demo.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt响应DTO
 *
 * 用于返回Prompt的详细信息，包括基本信息、模板内容和时间戳
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Prompt响应")
public class PromptResponse {

    /** Prompt唯一标识ID */
    @Schema(description = "Prompt ID")
    private Long id;

    /** Prompt名称，用于标识和描述该Prompt的用途 */
    @Schema(description = "Prompt名称")
    private String name;

    /** Prompt描述，详细说明该Prompt的功能和使用场景 */
    @Schema(description = "Prompt描述")
    private String description;

    /** Prompt模板内容，包含变量占位符的原始模板字符串 */
    @Schema(description = "Prompt模板内容")
    private String template;

    /** 从模板中提取的变量名称列表 */
    @Schema(description = "变量名称列表")
    private List<String> variableNames;

    /** Prompt创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** Prompt最后更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
