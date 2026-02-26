package com.example.demo.result.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试报告响应数据传输对象
 *
 * 用于封装返回给前端的测试报告数据
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测试报告响应")
public class TestReportResponse {

    /** 报告ID */
    @Schema(description = "报告ID")
    private Long id;

    /** 任务ID */
    @Schema(description = "任务ID")
    private Long taskId;

    /** 任务名称 */
    @Schema(description = "任务名称")
    private String taskName;

    /** 测评集ID */
    @Schema(description = "测评集ID")
    private Long testSetId;

    /** 测评集名称 */
    @Schema(description = "测评集名称")
    private String testSetName;

    /** 环境名称 */
    @Schema(description = "环境名称")
    private String environmentName;

    /** 执行插件名称 */
    @Schema(description = "执行插件名称")
    private String executionPluginName;

    /** 评估插件名称 */
    @Schema(description = "评估插件名称")
    private String evaluationPluginName;

    /** 总执行项数 */
    @Schema(description = "总执行项数")
    private Integer totalItems;

    /** 成功项数 */
    @Schema(description = "成功项数")
    private Integer successItems;

    /** 失败项数 */
    @Schema(description = "失败项数")
    private Integer failedItems;

    /** 成功率(%) */
    @Schema(description = "成功率(%)")
    private Double successRate;

    /** 平均评分 */
    @Schema(description = "平均评分")
    private Double averageScore;

    /** 总执行时间(ms) */
    @Schema(description = "总执行时间(ms)")
    private Long totalExecutionTimeMs;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    /** 报告摘要 */
    @Schema(description = "报告摘要")
    private String summary;

    /** 报告详情 */
    @Schema(description = "报告详情")
    private String details;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
