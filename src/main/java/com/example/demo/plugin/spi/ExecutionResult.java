package com.example.demo.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    private boolean success;
    private String output;
    private String errorMessage;
    private Long executionTimeMs;
    private Integer tokenUsage;
}
