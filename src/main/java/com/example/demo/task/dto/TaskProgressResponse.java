package com.example.demo.task.dto;

import com.example.demo.common.enums.TestTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务进度响应")
public class TaskProgressResponse {

    @Schema(description = "任务ID")
    private Long taskId;

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

    @Schema(description = "错误信息")
    private String errorMessage;
}
