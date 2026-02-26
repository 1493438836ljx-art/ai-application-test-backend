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
public class ExecutionContext {

    private Long taskItemId;
    private String input;
    private String apiEndpoint;
    private Map<String, Object> authConfig;
    private Map<String, Object> pluginConfig;
}
