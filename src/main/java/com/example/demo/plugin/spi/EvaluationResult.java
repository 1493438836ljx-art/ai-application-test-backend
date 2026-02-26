package com.example.demo.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    private boolean success;
    private double score;
    private String reason;
    private String errorMessage;
}
