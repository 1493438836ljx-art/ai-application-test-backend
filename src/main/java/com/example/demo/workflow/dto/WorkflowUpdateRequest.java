package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工作流更新请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
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

    @Schema(description = "更新人", example = "admin")
    private String updatedBy;
}
