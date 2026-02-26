package com.example.demo.environment.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.enums.EnvironmentType;
import com.example.demo.environment.dto.*;
import com.example.demo.environment.entity.Environment;
import com.example.demo.environment.mapper.EnvironmentMapper;
import com.example.demo.environment.repository.EnvironmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentMapper environmentMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public EnvironmentResponse createEnvironment(EnvironmentCreateRequest request) {
        Environment environment = environmentMapper.toEntity(request);
        if (environment.getIsActive() == null) {
            environment.setIsActive(true);
        }
        Environment saved = environmentRepository.save(environment);
        return environmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<EnvironmentResponse> getEnvironments(String name, Pageable pageable) {
        Page<Environment> environments;
        if (StringUtils.isNotBlank(name)) {
            environments = environmentRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            environments = environmentRepository.findAll(pageable);
        }
        return environments.map(environmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse getEnvironmentById(Long id) {
        Environment environment = findEnvironmentById(id);
        return environmentMapper.toResponse(environment);
    }

    @Transactional
    public EnvironmentResponse updateEnvironment(Long id, EnvironmentUpdateRequest request) {
        Environment environment = findEnvironmentById(id);
        environmentMapper.updateEntity(request, environment);
        Environment updated = environmentRepository.save(environment);
        return environmentMapper.toResponse(updated);
    }

    @Transactional
    public void deleteEnvironment(Long id) {
        if (!environmentRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND);
        }
        environmentRepository.deleteById(id);
    }

    public ConnectionTestResponse testConnection(Long id, ConnectionTestRequest request) {
        Environment environment = findEnvironmentById(id);

        if (environment.getType() == EnvironmentType.HTTP_API) {
            return testHttpConnection(environment, request);
        } else if (environment.getType() == EnvironmentType.SDK) {
            return testSdkConnection(environment, request);
        } else {
            return ConnectionTestResponse.builder()
                    .success(false)
                    .message("不支持的环境类型: " + environment.getType())
                    .build();
        }
    }

    private ConnectionTestResponse testHttpConnection(Environment environment, ConnectionTestRequest request) {
        if (StringUtils.isBlank(environment.getApiEndpoint())) {
            return ConnectionTestResponse.builder()
                    .success(false)
                    .message("API端点未配置")
                    .build();
        }

        try {
            HttpHeaders headers = buildAuthHeaders(environment);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "message", request.getTestInput() != null ? request.getTestInput() : "Hello, this is a test."
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                    environment.getApiEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            long responseTime = System.currentTimeMillis() - startTime;

            boolean success = response.getStatusCode().is2xxSuccessful();

            return ConnectionTestResponse.builder()
                    .success(success)
                    .message(success ? "连接成功" : "连接失败: " + response.getStatusCode())
                    .responseTimeMs(responseTime)
                    .testOutput(success ? response.getBody() : null)
                    .build();

        } catch (RestClientException e) {
            log.error("HTTP connection test failed", e);
            return ConnectionTestResponse.builder()
                    .success(false)
                    .message("连接失败: " + e.getMessage())
                    .build();
        }
    }

    private ConnectionTestResponse testSdkConnection(Environment environment, ConnectionTestRequest request) {
        return ConnectionTestResponse.builder()
                .success(true)
                .message("SDK连接测试成功（模拟）")
                .responseTimeMs(10L)
                .testOutput("SDK connection verified")
                .build();
    }

    private HttpHeaders buildAuthHeaders(Environment environment) {
        HttpHeaders headers = new HttpHeaders();

        String authType = environment.getAuthType();
        String authConfig = environment.getAuthConfig();

        if (StringUtils.isBlank(authType) || "NONE".equalsIgnoreCase(authType)) {
            return headers;
        }

        try {
            Map<String, Object> config = objectMapper.readValue(authConfig, Map.class);

            if ("API_KEY".equalsIgnoreCase(authType)) {
                String apiKey = (String) config.get("apiKey");
                String headerName = (String) config.getOrDefault("headerName", "X-API-Key");
                headers.set(headerName, apiKey);
            } else if ("BEARER".equalsIgnoreCase(authType)) {
                String token = (String) config.get("token");
                headers.setBearerAuth(token);
            } else if ("BASIC".equalsIgnoreCase(authType)) {
                String username = (String) config.get("username");
                String password = (String) config.get("password");
                headers.setBasicAuth(username, password);
            }
        } catch (Exception e) {
            log.warn("Failed to parse auth config", e);
        }

        return headers;
    }

    private Environment findEnvironmentById(Long id) {
        return environmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND));
    }
}
