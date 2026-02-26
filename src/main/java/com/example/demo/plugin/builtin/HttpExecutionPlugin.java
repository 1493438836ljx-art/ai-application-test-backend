package com.example.demo.plugin.builtin;

import com.example.demo.plugin.spi.ExecutionContext;
import com.example.demo.plugin.spi.ExecutionResult;
import com.example.demo.plugin.spi.ExecutionPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP执行插件，通过HTTP API调用生成式AI服务进行推理
 * <p>
 * 支持API Key和Bearer Token两种认证方式
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class HttpExecutionPlugin implements ExecutionPlugin {

    /** 插件名称常量 */
    public static final String NAME = "http-execution";

    /** REST模板，用于发送HTTP请求 */
    private final RestTemplate restTemplate = new RestTemplate();

    /** JSON对象映射器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取插件名称
     *
     * @return 插件名称 "http-execution"
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * 获取插件描述
     *
     * @return 插件功能描述
     */
    @Override
    public String getDescription() {
        return "通过HTTP API调用生成式AI进行推理";
    }

    /**
     * 执行HTTP推理请求
     * <p>
     * 根据上下文配置构建HTTP请求，调用AI服务API并返回推理结果
     * </p>
     *
     * @param context 执行上下文，包含API端点、认证信息和输入内容
     * @return 执行结果，包含输出内容和执行状态
     */
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            String apiEndpoint = context.getApiEndpoint();
            if (apiEndpoint == null || apiEndpoint.isEmpty()) {
                return ExecutionResult.builder()
                        .success(false)
                        .errorMessage("API端点未配置")
                        .build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (context.getAuthConfig() != null) {
                String authType = (String) context.getAuthConfig().get("type");
                if ("API_KEY".equalsIgnoreCase(authType)) {
                    String apiKey = (String) context.getAuthConfig().get("apiKey");
                    String headerName = (String) context.getAuthConfig().getOrDefault("headerName", "X-API-Key");
                    headers.set(headerName, apiKey);
                } else if ("BEARER".equalsIgnoreCase(authType)) {
                    String token = (String) context.getAuthConfig().get("token");
                    headers.setBearerAuth(token);
                }
            }

            Map<String, Object> requestBody = Map.of(
                    "message", context.getInput()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiEndpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            long executionTime = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful()) {
                return ExecutionResult.builder()
                        .success(true)
                        .output(response.getBody())
                        .executionTimeMs(executionTime)
                        .build();
            } else {
                return ExecutionResult.builder()
                        .success(false)
                        .errorMessage("HTTP错误: " + response.getStatusCode())
                        .executionTimeMs(executionTime)
                        .build();
            }

        } catch (RestClientException e) {
            log.error("HTTP execution failed", e);
            return ExecutionResult.builder()
                    .success(false)
                    .errorMessage("请求失败: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("Execution failed with unexpected error", e);
            return ExecutionResult.builder()
                    .success(false)
                    .errorMessage("执行异常: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}
