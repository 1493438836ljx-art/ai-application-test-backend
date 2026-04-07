package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工作流更新请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Schema(description = "工作流更新请求")
public class WorkflowUpdateRequest {

    @Size(max = 100, message = "工作流名称长度不能超过100")
    @Schema(description = "工作流名称", example = "AI文本测试工作流")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "工作流描述", example = "用于测试AI文本生成能力的工作流")
    private String description;

    @Size(max = 50, message = "分类长度不能超过50")
    @Schema(description = "工作流分类", example = "测试")
    private String category;

    @Schema(description = "触发类型", example = "MANUAL")
    private String triggerType;

    @Schema(description = "触发配置（JSON格式）", example = "{}")
    private String triggerConfig;

    @Schema(description = "更新人", example = "admin")
    private String updatedBy;
}
