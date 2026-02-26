package com.example.demo.result.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 测试结果摘要数据传输对象
 *
 * 用于封装测试任务的详细执行结果摘要，包含统计信息和每个测试项的执行结果
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultSummary {

    /** 任务ID */
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 总测试项数 */
    private Integer totalItems;

    /** 成功项数 */
    private Integer successItems;

    /** 失败项数 */
    private Integer failedItems;

    /** 成功率 */
    private Double successRate;

    /** 平均评分 */
    private Double averageScore;

    /** 总执行时间（毫秒） */
    private Long totalExecutionTimeMs;

    /** 测试项结果列表 */
    private List<ItemResult> itemResults;

    /**
     * 测试项结果内部类
     *
     * 封装单个测试项的执行结果详情
     *
     * @author AI Test Platform Team
     * @version 1.0.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {

        /** 测试项ID */
        private Long itemId;

        /** 执行顺序 */
        private Integer sequence;

        /** 执行状态 */
        private String status;

        /** 输入内容 */
        private String input;

        /** 期望输出 */
        private String expectedOutput;

        /** 实际输出 */
        private String actualOutput;

        /** 评分 */
        private Double score;

        /** 评分原因 */
        private String reason;

        /** 执行时间（毫秒） */
        private Long executionTimeMs;
    }
}
