package com.example.demo.task.dto;

import com.example.demo.common.enums.TestTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务进度响应DTO
 * 用于返回测试任务的执行进度信息
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务进度响应")
public class TaskProgressResponse {

    /** 任务ID */
    @Schema(description = "任务ID")
    private Long taskId;

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

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;
}
