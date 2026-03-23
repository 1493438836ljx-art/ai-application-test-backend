package com.example.demo.workflow.dto;

import com.example.demo.workflow.entity.WorkflowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工作流创建请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "工作流创建请求")
public class WorkflowCreateRequest {

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 100, message = "工作流名称长度不能超过100")
    @Schema(description = "工作流名称", example = "AI文本测试工作流")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "工作流描述", example = "用于测试AI文本生成能力的工作流")
    private String description;

    @Schema(description = "创建人", example = "admin")
    private String createdBy;
}
