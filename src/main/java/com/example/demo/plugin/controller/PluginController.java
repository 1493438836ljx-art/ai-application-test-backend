package com.example.demo.plugin.controller;

import com.example.demo.common.enums.PluginType;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageResponse;
import com.example.demo.plugin.dto.*;
import com.example.demo.plugin.service.PluginService;
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

import java.util.List;

@Tag(name = "插件管理", description = "执行插件和评估插件的CRUD操作")
@RestController
@RequestMapping("/api/v1/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginService pluginService;

    @Operation(summary = "创建插件", description = "创建一个新的插件配置")
    @PostMapping
    public ApiResponse<PluginResponse> createPlugin(@Valid @RequestBody PluginCreateRequest request) {
        return ApiResponse.success(pluginService.createPlugin(request));
    }

    @Operation(summary = "获取插件列表", description = "分页查询插件列表，支持按类型筛选")
    @GetMapping
    public ApiResponse<PageResponse<PluginResponse>> getPlugins(
            @Parameter(description = "插件类型") @RequestParam(required = false) PluginType type,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PluginResponse> result = pluginService.getPlugins(type, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "按类型获取插件", description = "获取指定类型的所有激活插件")
    @GetMapping("/type/{type}")
    public ApiResponse<List<PluginResponse>> getPluginsByType(
            @Parameter(description = "插件类型") @PathVariable PluginType type) {
        return ApiResponse.success(pluginService.getPluginsByType(type));
    }

    @Operation(summary = "获取插件详情", description = "根据ID获取插件详细信息")
    @GetMapping("/{id}")
    public ApiResponse<PluginResponse> getPluginById(
            @Parameter(description = "插件ID") @PathVariable Long id) {
        return ApiResponse.success(pluginService.getPluginById(id));
    }

    @Operation(summary = "更新插件", description = "更新插件配置信息")
    @PutMapping("/{id}")
    public ApiResponse<PluginResponse> updatePlugin(
            @Parameter(description = "插件ID") @PathVariable Long id,
            @Valid @RequestBody PluginUpdateRequest request) {
        return ApiResponse.success(pluginService.updatePlugin(id, request));
    }

    @Operation(summary = "删除插件", description = "删除指定的插件（内置插件不可删除）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlugin(
            @Parameter(description = "插件ID") @PathVariable Long id) {
        pluginService.deletePlugin(id);
        return ApiResponse.success();
    }
}
