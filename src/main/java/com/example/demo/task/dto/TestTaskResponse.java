package com.example.demo.task.dto;

import com.example.demo.common.enums.TestTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测试任务响应")
public class TestTaskResponse {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "测评集ID")
    private Long testSetId;

    @Schema(description = "测评集名称")
    private String testSetName;

    @Schema(description = "环境ID")
    private Long environmentId;

    @Schema(description = "环境名称")
    private String environmentName;

    @Schema(description = "执行插件ID")
    private Long executionPluginId;

    @Schema(description = "执行插件名称")
    private String executionPluginName;

    @Schema(description = "评估插件ID")
    private Long evaluationPluginId;

    @Schema(description = "评估插件名称")
    private String evaluationPluginName;

    @Schema(description = "任务状态")
    private TestTaskStatus status;

    @Schema(description = "总执行项数")
    private Integer totalItems;

    @Schema(description = "已完成项数")
    private Integer completedItems;

    @Schema(description = "成功项数")
    private Integer successItems;

    @Schema(description = "失败项数")
    private Integer failedItems;

    @Schema(description = "执行进度(%)")
    private Double progress;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
