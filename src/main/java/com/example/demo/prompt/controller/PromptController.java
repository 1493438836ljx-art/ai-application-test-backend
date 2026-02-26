package com.example.demo.prompt.controller;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageResponse;
import com.example.demo.prompt.dto.*;
import com.example.demo.prompt.service.PromptService;
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

@Tag(name = "Prompt管理", description = "Prompt模板的CRUD操作及渲染功能")
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @Operation(summary = "创建Prompt", description = "创建一个新的Prompt模板")
    @PostMapping
    public ApiResponse<PromptResponse> createPrompt(@Valid @RequestBody PromptCreateRequest request) {
        return ApiResponse.success(promptService.createPrompt(request));
    }

    @Operation(summary = "获取Prompt列表", description = "分页查询Prompt列表，支持按名称模糊搜索")
    @GetMapping
    public ApiResponse<PageResponse<PromptResponse>> getPrompts(
            @Parameter(description = "Prompt名称（模糊搜索）") @RequestParam(required = false) String name,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PromptResponse> result = promptService.getPrompts(name, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "获取Prompt详情", description = "根据ID获取Prompt详细信息")
    @GetMapping("/{id}")
    public ApiResponse<PromptResponse> getPromptById(
            @Parameter(description = "Prompt ID") @PathVariable Long id) {
        return ApiResponse.success(promptService.getPromptById(id));
    }

    @Operation(summary = "根据名称获取Prompt", description = "根据名称获取Prompt详细信息")
    @GetMapping("/name/{name}")
    public ApiResponse<PromptResponse> getPromptByName(
            @Parameter(description = "Prompt名称") @PathVariable String name) {
        return ApiResponse.success(promptService.getPromptByName(name));
    }

    @Operation(summary = "更新Prompt", description = "更新Prompt的基本信息")
    @PutMapping("/{id}")
    public ApiResponse<PromptResponse> updatePrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long id,
            @Valid @RequestBody PromptUpdateRequest request) {
        return ApiResponse.success(promptService.updatePrompt(id, request));
    }

    @Operation(summary = "删除Prompt", description = "删除指定的Prompt")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long id) {
        promptService.deletePrompt(id);
        return ApiResponse.success();
    }

    @Operation(summary = "渲染Prompt", description = "使用提供的变量值渲染Prompt模板")
    @PostMapping("/{id}/render")
    public ApiResponse<PromptRenderResponse> renderPrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long id,
            @RequestBody PromptRenderRequest request) {
        return ApiResponse.success(promptService.renderPrompt(id, request));
    }

    @Operation(summary = "渲染模板", description = "直接渲染提供的模板字符串")
    @PostMapping("/render")
    public ApiResponse<PromptRenderResponse> renderTemplate(
            @RequestBody PromptRenderRequest request,
            @Parameter(description = "模板内容") @RequestParam String template) {
        return ApiResponse.success(promptService.renderTemplate(template, request.getVariables()));
    }
}
