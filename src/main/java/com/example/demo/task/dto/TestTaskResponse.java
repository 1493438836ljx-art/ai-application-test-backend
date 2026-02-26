package com.example.demo.task.dto;

import com.example.demo.common.enums.TestTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试任务响应DTO
 * 用于返回测试任务的详细信息
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测试任务响应")
public class TestTaskResponse {

    /** 任务ID */
    @Schema(description = "任务ID")
    private Long id;

    /** 任务名称 */
    @Schema(description = "任务名称")
    private String name;

    /** 任务描述 */
    @Schema(description = "任务描述")
    private String description;

    /** 测评集ID */
    @Schema(description = "测评集ID")
    private Long testSetId;

    /** 测评集名称 */
    @Schema(description = "测评集名称")
    private String testSetName;

    /** 环境ID */
    @Schema(description = "环境ID")
    private Long environmentId;

    /** 环境名称 */
    @Schema(description = "环境名称")
    private String environmentName;

    /** 执行插件ID */
    @Schema(description = "执行插件ID")
    private Long executionPluginId;

    /** 执行插件名称 */
    @Schema(description = "执行插件名称")
    private String executionPluginName;

    /** 评估插件ID */
    @Schema(description = "评估插件ID")
    private Long evaluationPluginId;

    /** 评估插件名称 */
    @Schema(description = "评估插件名称")
    private String evaluationPluginName;

    /** 任务状态 */
    @Schema(description = "任务状态")
    private TestTaskStatus status;

    /** 总执行项数 */
    @Schema(description = "总执行项数")
    private Integer totalItems;

    /** 已完成项数 */
    @Schema(description = "已完成项数")
    private Integer completedItems;

    /** 成功项数 */
    @Schema(description = "成功项数")
    private Integer successItems;

    /** 失败项数 */
    @Schema(description = "失败项数")
    private Integer failedItems;

    /** 执行进度(%) */
    @Schema(description = "执行进度(%)")
    private Double progress;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
