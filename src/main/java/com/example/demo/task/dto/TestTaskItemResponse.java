package com.example.demo.task.dto;

import com.example.demo.common.enums.TestTaskItemStatus;
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
@Schema(description = "测试任务执行项响应")
public class TestTaskItemResponse {

    @Schema(description = "执行项ID")
    private Long id;

    @Schema(description = "所属任务ID")
    private Long taskId;

    @Schema(description = "测试用例ID")
    private Long testCaseId;

    @Schema(description = "序号")
    private Integer sequence;

    @Schema(description = "状态")
    private TestTaskItemStatus status;

    @Schema(description = "输入内容")
    private String input;

    @Schema(description = "期望输出")
    private String expectedOutput;

    @Schema(description = "实际输出")
    private String actualOutput;

    @Schema(description = "评分")
    private Double score;

    @Schema(description = "评估原因")
    private String reason;

    @Schema(description = "执行时间(ms)")
    private Long executionTimeMs;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
