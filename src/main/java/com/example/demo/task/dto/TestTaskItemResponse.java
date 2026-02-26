package com.example.demo.task.dto;

import com.example.demo.common.enums.TestTaskItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试任务执行项响应DTO
 * 用于返回测试任务执行项的详细信息
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测试任务执行项响应")
public class TestTaskItemResponse {

    /** 执行项ID */
    @Schema(description = "执行项ID")
    private Long id;

    /** 所属任务ID */
    @Schema(description = "所属任务ID")
    private Long taskId;

    /** 测试用例ID */
    @Schema(description = "测试用例ID")
    private Long testCaseId;

    /** 序号 */
    @Schema(description = "序号")
    private Integer sequence;

    /** 状态 */
    @Schema(description = "状态")
    private TestTaskItemStatus status;

    /** 输入内容 */
    @Schema(description = "输入内容")
    private String input;

    /** 期望输出 */
    @Schema(description = "期望输出")
    private String expectedOutput;

    /** 实际输出 */
    @Schema(description = "实际输出")
    private String actualOutput;

    /** 评分 */
    @Schema(description = "评分")
    private Double score;

    /** 评估原因 */
    @Schema(description = "评估原因")
    private String reason;

    /** 执行时间(ms) */
    @Schema(description = "执行时间(ms)")
    private Long executionTimeMs;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
