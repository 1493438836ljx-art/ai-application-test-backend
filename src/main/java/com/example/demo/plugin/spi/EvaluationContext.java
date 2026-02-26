package com.example.demo.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {

    private Long taskItemId;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private Map<String, Object> pluginConfig;
}
