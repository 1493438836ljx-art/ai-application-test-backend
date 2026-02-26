package com.example.demo.result.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultSummary {

    private Long taskId;
    private String taskName;
    private Integer totalItems;
    private Integer successItems;
    private Integer failedItems;
    private Double successRate;
    private Double averageScore;
    private Long totalExecutionTimeMs;

    private List<ItemResult> itemResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private Long itemId;
        private Integer sequence;
        private String status;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Double score;
        private String reason;
        private Long executionTimeMs;
    }
}
