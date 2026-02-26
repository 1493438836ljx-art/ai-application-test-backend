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

/**
 * Prompt管理控制器
 * <p>
 * 提供Prompt模板的RESTful API接口，包括：
 * <ul>
 *   <li>Prompt的CRUD操作</li>
 *   <li>模板渲染功能</li>
 * </ul>
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Tag(name = "Prompt管理", description = "Prompt模板的CRUD操作及渲染功能")
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    /**
     * 创建Prompt模板
     *
     * @param request 创建请求
     * @return 创建后的Prompt
     */
    @Operation(summary = "创建Prompt", description = "创建一个新的Prompt模板")
    @PostMapping
    public ApiResponse<PromptResponse> createPrompt(@Valid @RequestBody PromptCreateRequest request) {
        return ApiResponse.success(promptService.createPrompt(request));
    }

    /**
     * 分页查询Prompt列表
     *
     * @param name    名称过滤（模糊搜索）
     * @param page    页码（从0开始）
     * @param size    每页大小
     * @param sortBy  排序字段
     * @param sortDir 排序方向
     * @return Prompt分页结果
     */
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

    /**
     * 根据ID获取Prompt详情
     *
     * @param id Prompt ID
     * @return Prompt详情
     */
    @Operation(summary = "获取Prompt详情", description = "根据ID获取Prompt详细信息")
    @GetMapping("/{id}")
    public ApiResponse<PromptResponse> getPromptById(
            @Parameter(description = "Prompt ID") @PathVariable Long id) {
        return ApiResponse.success(promptService.getPromptById(id));
    }

    /**
     * 根据名称获取Prompt详情
     *
     * @param name Prompt名称
     * @return Prompt详情
     */
    @Operation(summary = "根据名称获取Prompt", description = "根据名称获取Prompt详细信息")
    @GetMapping("/name/{name}")
    public ApiResponse<PromptResponse> getPromptByName(
            @Parameter(description = "Prompt名称") @PathVariable String name) {
        return ApiResponse.success(promptService.getPromptByName(name));
    }

    /**
     * 更新Prompt
     *
     * @param id      Prompt ID
     * @param request 更新请求
     * @return 更新后的Prompt
     */
    @Operation(summary = "更新Prompt", description = "更新Prompt的基本信息")
    @PutMapping("/{id}")
    public ApiResponse<PromptResponse> updatePrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long id,
            @Valid @RequestBody PromptUpdateRequest request) {
        return ApiResponse.success(promptService.updatePrompt(id, request));
    }

    /**
     * 删除Prompt
     *
     * @param id Prompt ID
     * @return 成功响应
     */
    @Operation(summary = "删除Prompt", description = "删除指定的Prompt")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long id) {
        promptService.deletePrompt(id);
        return ApiResponse.success();
    }

    /**
     * 渲染Prompt模板
     *
     * @param id      Prompt ID
     * @param request 渲染请求（包含变量值）
     * @return 渲染结果
     */
    @Operation(summary = "渲染Prompt", description = "使用提供的变量值渲染Prompt模板")
    @PostMapping("/{id}/render")
    public ApiResponse<PromptRenderResponse> renderPrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long id,
            @RequestBody PromptRenderRequest request) {
        return ApiResponse.success(promptService.renderPrompt(id, request));
    }

    /**
     * 直接渲染模板字符串
     *
     * @param request  渲染请求（包含变量值）
     * @param template 模板内容
     * @return 渲染结果
     */
    @Operation(summary = "渲染模板", description = "直接渲染提供的模板字符串")
    @PostMapping("/render")
    public ApiResponse<PromptRenderResponse> renderTemplate(
            @RequestBody PromptRenderRequest request,
            @Parameter(description = "模板内容") @RequestParam String template) {
        return ApiResponse.success(promptService.renderTemplate(template, request.getVariables()));
    }
}
