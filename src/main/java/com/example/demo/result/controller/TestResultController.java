package com.example.demo.result.controller;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageResponse;
import com.example.demo.result.dto.TestReportResponse;
import com.example.demo.result.dto.TestResultSummary;
import com.example.demo.result.service.TestReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "测试结果管理", description = "测试报告的生成、查询和导出功能")
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class TestResultController {

    private final TestReportService testReportService;

    @Operation(summary = "生成测试报告", description = "为已完成的任务生成测试报告")
    @PostMapping("/tasks/{taskId}/report")
    public ApiResponse<TestReportResponse> generateReport(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        return ApiResponse.success(testReportService.generateReport(taskId));
    }

    @Operation(summary = "获取任务报告", description = "获取指定任务的测试报告，如不存在则自动生成")
    @GetMapping("/tasks/{taskId}/report")
    public ApiResponse<TestReportResponse> getTaskReport(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        return ApiResponse.success(testReportService.getOrCreateReport(taskId));
    }

    @Operation(summary = "获取任务结果摘要", description = "获取任务的详细执行结果摘要")
    @GetMapping("/tasks/{taskId}/summary")
    public ApiResponse<TestResultSummary> getTaskResultSummary(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        return ApiResponse.success(testReportService.getResultSummary(taskId));
    }

    @Operation(summary = "获取报告列表", description = "分页查询测试报告列表")
    @GetMapping("/reports")
    public ApiResponse<PageResponse<TestReportResponse>> getReports(
            @Parameter(description = "任务名称（模糊搜索）") @RequestParam(required = false) String taskName,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TestReportResponse> result = testReportService.getReports(taskName, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "获取报告详情", description = "根据报告ID获取详细信息")
    @GetMapping("/reports/{id}")
    public ApiResponse<TestReportResponse> getReportById(
            @Parameter(description = "报告ID") @PathVariable Long id) {
        return ApiResponse.success(testReportService.getReportById(id));
    }

    @Operation(summary = "删除报告", description = "删除指定的测试报告")
    @DeleteMapping("/reports/{id}")
    public ApiResponse<Void> deleteReport(
            @Parameter(description = "报告ID") @PathVariable Long id) {
        testReportService.deleteReport(id);
        return ApiResponse.success();
    }
}
