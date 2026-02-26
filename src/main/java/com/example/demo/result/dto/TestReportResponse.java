package com.example.demo.result.dto;

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
@Schema(description = "测试报告响应")
public class TestReportResponse {

    @Schema(description = "报告ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "测评集ID")
    private Long testSetId;

    @Schema(description = "测评集名称")
    private String testSetName;

    @Schema(description = "环境名称")
    private String environmentName;

    @Schema(description = "执行插件名称")
    private String executionPluginName;

    @Schema(description = "评估插件名称")
    private String evaluationPluginName;

    @Schema(description = "总执行项数")
    private Integer totalItems;

    @Schema(description = "成功项数")
    private Integer successItems;

    @Schema(description = "失败项数")
    private Integer failedItems;

    @Schema(description = "成功率(%)")
    private Double successRate;

    @Schema(description = "平均评分")
    private Double averageScore;

    @Schema(description = "总执行时间(ms)")
    private Long totalExecutionTimeMs;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "报告摘要")
    private String summary;

    @Schema(description = "报告详情")
    private String details;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
