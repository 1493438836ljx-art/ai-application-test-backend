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

/**
 * 环境服务类
 * <p>
 * 提供环境管理的业务逻辑，包括环境的增删改查及连接测试功能
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    /** 环境数据访问层 */
    private final EnvironmentRepository environmentRepository;

    /** 环境对象映射器 */
    private final EnvironmentMapper environmentMapper;

    /** REST请求模板 */
    private final RestTemplate restTemplate = new RestTemplate();

    /** JSON对象映射器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建新环境
     *
     * @param request 创建环境请求DTO
     * @return 创建成功后的环境响应DTO
     */
    @Transactional
    public EnvironmentResponse createEnvironment(EnvironmentCreateRequest request) {
        Environment environment = environmentMapper.toEntity(request);
        if (environment.getIsActive() == null) {
            environment.setIsActive(true);
        }
        Environment saved = environmentRepository.save(environment);
        return environmentMapper.toResponse(saved);
    }

    /**
     * 分页查询环境列表
     *
     * @param name 环境名称关键字（可选，用于模糊搜索）
     * @param pageable 分页参数
     * @return 环境响应DTO分页列表
     */
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

    /**
     * 根据ID获取环境详情
     *
     * @param id 环境ID
     * @return 环境响应DTO
     * @throws BusinessException 环境不存在时抛出异常
     */
    @Transactional(readOnly = true)
    public EnvironmentResponse getEnvironmentById(Long id) {
        Environment environment = findEnvironmentById(id);
        return environmentMapper.toResponse(environment);
    }

    /**
     * 更新环境信息
     *
     * @param id 环境ID
     * @param request 更新环境请求DTO
     * @return 更新后的环境响应DTO
     * @throws BusinessException 环境不存在时抛出异常
     */
    @Transactional
    public EnvironmentResponse updateEnvironment(Long id, EnvironmentUpdateRequest request) {
        Environment environment = findEnvironmentById(id);
        environmentMapper.updateEntity(request, environment);
        Environment updated = environmentRepository.save(environment);
        return environmentMapper.toResponse(updated);
    }

    /**
     * 删除环境
     *
     * @param id 环境ID
     * @throws BusinessException 环境不存在时抛出异常
     */
    @Transactional
    public void deleteEnvironment(Long id) {
        if (!environmentRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND);
        }
        environmentRepository.deleteById(id);
    }

    /**
     * 测试环境连接
     * <p>
     * 根据环境类型选择相应的测试方法
     * </p>
     *
     * @param id 环境ID
     * @param request 连接测试请求DTO
     * @return 连接测试响应DTO
     * @throws BusinessException 环境不存在时抛出异常
     */
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

    /**
     * 测试HTTP类型环境的连接
     *
     * @param environment 环境实体
     * @param request 连接测试请求DTO
     * @return 连接测试响应DTO
     */
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

    /**
     * 测试SDK类型环境的连接
     * <p>
     * 当前为模拟实现，返回固定的成功响应
     * </p>
     *
     * @param environment 环境实体
     * @param request 连接测试请求DTO
     * @return 连接测试响应DTO
     */
    private ConnectionTestResponse testSdkConnection(Environment environment, ConnectionTestRequest request) {
        return ConnectionTestResponse.builder()
                .success(true)
                .message("SDK连接测试成功（模拟）")
                .responseTimeMs(10L)
                .testOutput("SDK connection verified")
                .build();
    }

    /**
     * 构建认证请求头
     * <p>
     * 根据环境的认证类型和配置构建相应的HTTP请求头
     * </p>
     *
     * @param environment 环境实体
     * @return 包含认证信息的HTTP请求头
     */
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

    /**
     * 根据ID查找环境实体
     *
     * @param id 环境ID
     * @return 环境实体
     * @throws BusinessException 环境不存在时抛出异常
     */
    private Environment findEnvironmentById(Long id) {
        return environmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND));
    }
}
