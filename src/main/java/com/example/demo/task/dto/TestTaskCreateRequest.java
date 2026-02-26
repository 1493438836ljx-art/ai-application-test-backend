package com.example.demo.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建测试任务请求DTO
 * 用于接收创建测试任务的请求参数
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "创建测试任务请求")
public class TestTaskCreateRequest {

    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /** 任务描述 */
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    @Schema(description = "任务描述")
    private String description;

    /** 测评集ID */
    @NotNull(message = "测评集ID不能为空")
    @Schema(description = "测评集ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long testSetId;

    /** 环境ID */
    @NotNull(message = "环境ID不能为空")
    @Schema(description = "环境ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long environmentId;

    /** 执行插件ID */
    @NotNull(message = "执行插件ID不能为空")
    @Schema(description = "执行插件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long executionPluginId;

    /** 评估插件ID */
    @NotNull(message = "评估插件ID不能为空")
    @Schema(description = "评估插件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long evaluationPluginId;

    /** 执行插件配置(JSON格式) */
    @Schema(description = "执行插件配置(JSON格式)")
    private String executionConfig;

    /** 评估插件配置(JSON格式) */
    @Schema(description = "评估插件配置(JSON格式)")
    private String evaluationConfig;
}
