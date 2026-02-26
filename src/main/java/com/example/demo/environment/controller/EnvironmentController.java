package com.example.demo.environment.controller;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageResponse;
import com.example.demo.environment.dto.*;
import com.example.demo.environment.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "环境管理", description = "AI应用环境配置的CRUD操作及连接测试")
@RestController
@RequestMapping("/api/v1/environments")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @Operation(summary = "创建环境", description = "创建一个新的AI应用环境配置")
    @PostMapping
    public ApiResponse<EnvironmentResponse> createEnvironment(@Valid @RequestBody EnvironmentCreateRequest request) {
        return ApiResponse.success(environmentService.createEnvironment(request));
    }

    @Operation(summary = "获取环境列表", description = "分页查询环境列表，支持按名称模糊搜索")
    @GetMapping
    public ApiResponse<PageResponse<EnvironmentResponse>> getEnvironments(
            @Parameter(description = "环境名称（模糊搜索）") @RequestParam(required = false) String name,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EnvironmentResponse> result = environmentService.getEnvironments(name, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "获取环境详情", description = "根据ID获取环境详细信息")
    @GetMapping("/{id}")
    public ApiResponse<EnvironmentResponse> getEnvironmentById(
            @Parameter(description = "环境ID") @PathVariable Long id) {
        return ApiResponse.success(environmentService.getEnvironmentById(id));
    }

    @Operation(summary = "更新环境", description = "更新环境配置信息")
    @PutMapping("/{id}")
    public ApiResponse<EnvironmentResponse> updateEnvironment(
            @Parameter(description = "环境ID") @PathVariable Long id,
            @Valid @RequestBody EnvironmentUpdateRequest request) {
        return ApiResponse.success(environmentService.updateEnvironment(id, request));
    }

    @Operation(summary = "删除环境", description = "删除指定的环境配置")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEnvironment(
            @Parameter(description = "环境ID") @PathVariable Long id) {
        environmentService.deleteEnvironment(id);
        return ApiResponse.success();
    }

    @Operation(summary = "测试环境连接", description = "测试与AI应用环境的连接是否正常")
    @PostMapping("/{id}/test-connection")
    public ApiResponse<ConnectionTestResponse> testConnection(
            @Parameter(description = "环境ID") @PathVariable Long id,
            @RequestBody(required = false) ConnectionTestRequest request) {
        if (request == null) {
            request = new ConnectionTestRequest();
        }
        return ApiResponse.success(environmentService.testConnection(id, request));
    }
}
