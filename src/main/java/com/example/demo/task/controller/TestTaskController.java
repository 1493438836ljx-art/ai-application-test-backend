package com.example.demo.task.controller;

import com.example.demo.common.enums.TestTaskStatus;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageResponse;
import com.example.demo.task.dto.*;
import com.example.demo.task.service.TestTaskService;
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

@Tag(name = "测试任务管理", description = "测试任务的创建、执行、暂停、恢复、取消等操作")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TestTaskController {

    private final TestTaskService testTaskService;

    @Operation(summary = "创建测试任务", description = "创建一个新的测试任务")
    @PostMapping
    public ApiResponse<TestTaskResponse> createTask(@Valid @RequestBody TestTaskCreateRequest request) {
        return ApiResponse.success(testTaskService.createTask(request));
    }

    @Operation(summary = "获取任务列表", description = "分页查询任务列表，支持按名称和状态筛选")
    @GetMapping
    public ApiResponse<PageResponse<TestTaskResponse>> getTasks(
            @Parameter(description = "任务名称（模糊搜索）") @RequestParam(required = false) String name,
            @Parameter(description = "任务状态") @RequestParam(required = false) TestTaskStatus status,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TestTaskResponse> result = testTaskService.getTasks(name, status, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "获取任务详情", description = "根据ID获取任务详细信息")
    @GetMapping("/{id}")
    public ApiResponse<TestTaskResponse> getTaskById(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.success(testTaskService.getTaskById(id));
    }

    @Operation(summary = "获取任务进度", description = "获取任务的执行进度信息")
    @GetMapping("/{id}/progress")
    public ApiResponse<TaskProgressResponse> getTaskProgress(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.success(testTaskService.getTaskProgress(id));
    }

    @Operation(summary = "更新任务", description = "更新任务的基本信息（仅待执行状态可修改）")
    @PutMapping("/{id}")
    public ApiResponse<TestTaskResponse> updateTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Valid @RequestBody TestTaskUpdateRequest request) {
        return ApiResponse.success(testTaskService.updateTask(id, request));
    }

    @Operation(summary = "删除任务", description = "删除指定的任务（运行中的任务不能删除）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        testTaskService.deleteTask(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启动任务", description = "启动执行测试任务")
    @PostMapping("/{id}/start")
    public ApiResponse<TestTaskResponse> startTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.success(testTaskService.startTask(id));
    }

    @Operation(summary = "暂停任务", description = "暂停正在执行的任务")
    @PostMapping("/{id}/pause")
    public ApiResponse<TestTaskResponse> pauseTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.success(testTaskService.pauseTask(id));
    }

    @Operation(summary = "恢复任务", description = "恢复已暂停的任务继续执行")
    @PostMapping("/{id}/resume")
    public ApiResponse<TestTaskResponse> resumeTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.success(testTaskService.resumeTask(id));
    }

    @Operation(summary = "取消任务", description = "取消任务执行")
    @PostMapping("/{id}/cancel")
    public ApiResponse<TestTaskResponse> cancelTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.success(testTaskService.cancelTask(id));
    }

    @Operation(summary = "获取任务执行项列表（分页）", description = "分页查询任务下的执行项")
    @GetMapping("/{id}/items")
    public ApiResponse<PageResponse<TestTaskItemResponse>> getTaskItems(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("sequence").ascending());
        Page<TestTaskItemResponse> result = testTaskService.getTaskItems(id, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "获取所有任务执行项", description = "获取任务下的所有执行项（不分页）")
    @GetMapping("/{id}/items/all")
    public ApiResponse<List<TestTaskItemResponse>> getAllTaskItems(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.success(testTaskService.getAllTaskItems(id));
    }
}
