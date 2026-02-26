package com.example.demo.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新测试任务请求")
public class TestTaskUpdateRequest {

    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "任务名称")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "执行插件配置(JSON格式)")
    private String executionConfig;

    @Schema(description = "评估插件配置(JSON格式)")
    private String evaluationConfig;
}
