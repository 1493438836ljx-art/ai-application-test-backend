package com.example.demo.testset.controller;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.response.PageResponse;
import com.example.demo.testset.dto.*;
import com.example.demo.testset.service.TestSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "测评集管理", description = "测评集和测试用例的CRUD操作及导入功能")
@RestController
@RequestMapping("/api/v1/test-sets")
@RequiredArgsConstructor
public class TestSetController {

    private final TestSetService testSetService;

    @Operation(summary = "创建测评集", description = "创建一个新的测评集")
    @PostMapping
    public ApiResponse<TestSetResponse> createTestSet(@Valid @RequestBody TestSetCreateRequest request) {
        return ApiResponse.success(testSetService.createTestSet(request));
    }

    @Operation(summary = "获取测评集列表", description = "分页查询测评集列表，支持按名称模糊搜索")
    @GetMapping
    public ApiResponse<PageResponse<TestSetResponse>> getTestSets(
            @Parameter(description = "测评集名称（模糊搜索）") @RequestParam(required = false) String name,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TestSetResponse> result = testSetService.getTestSets(name, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "获取测评集详情", description = "根据ID获取测评集详细信息")
    @GetMapping("/{id}")
    public ApiResponse<TestSetResponse> getTestSetById(
            @Parameter(description = "测评集ID") @PathVariable Long id) {
        return ApiResponse.success(testSetService.getTestSetById(id));
    }

    @Operation(summary = "更新测评集", description = "更新测评集的基本信息")
    @PutMapping("/{id}")
    public ApiResponse<TestSetResponse> updateTestSet(
            @Parameter(description = "测评集ID") @PathVariable Long id,
            @Valid @RequestBody TestSetUpdateRequest request) {
        return ApiResponse.success(testSetService.updateTestSet(id, request));
    }

    @Operation(summary = "删除测评集", description = "删除测评集及其所有测试用例")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTestSet(
            @Parameter(description = "测评集ID") @PathVariable Long id) {
        testSetService.deleteTestSet(id);
        return ApiResponse.success();
    }

    @Operation(summary = "添加测试用例", description = "向测评集添加单个测试用例")
    @PostMapping("/{id}/cases")
    public ApiResponse<TestCaseResponse> addTestCase(
            @Parameter(description = "测评集ID") @PathVariable Long id,
            @Valid @RequestBody TestCaseCreateRequest request) {
        return ApiResponse.success(testSetService.addTestCase(id, request));
    }

    @Operation(summary = "批量添加测试用例", description = "向测评集批量添加测试用例")
    @PostMapping("/{id}/cases/batch")
    public ApiResponse<List<TestCaseResponse>> addTestCases(
            @Parameter(description = "测评集ID") @PathVariable Long id,
            @Valid @RequestBody List<TestCaseCreateRequest> requests) {
        return ApiResponse.success(testSetService.addTestCases(id, requests));
    }

    @Operation(summary = "获取测试用例列表（分页）", description = "分页查询测评集下的测试用例")
    @GetMapping("/{id}/cases")
    public ApiResponse<PageResponse<TestCaseResponse>> getTestCases(
            @Parameter(description = "测评集ID") @PathVariable Long id,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("sequence").ascending());
        Page<TestCaseResponse> result = testSetService.getTestCases(id, pageable);
        return ApiResponse.success(PageResponse.of(result));
    }

    @Operation(summary = "获取所有测试用例", description = "获取测评集下的所有测试用例（不分页）")
    @GetMapping("/{id}/cases/all")
    public ApiResponse<List<TestCaseResponse>> getAllTestCases(
            @Parameter(description = "测评集ID") @PathVariable Long id) {
        return ApiResponse.success(testSetService.getAllTestCases(id));
    }

    @Operation(summary = "更新测试用例", description = "更新指定测试用例的信息")
    @PutMapping("/{testSetId}/cases/{caseId}")
    public ApiResponse<TestCaseResponse> updateTestCase(
            @Parameter(description = "测评集ID") @PathVariable Long testSetId,
            @Parameter(description = "测试用例ID") @PathVariable Long caseId,
            @Valid @RequestBody TestCaseUpdateRequest request) {
        return ApiResponse.success(testSetService.updateTestCase(testSetId, caseId, request));
    }

    @Operation(summary = "删除测试用例", description = "删除指定的测试用例")
    @DeleteMapping("/{testSetId}/cases/{caseId}")
    public ApiResponse<Void> deleteTestCase(
            @Parameter(description = "测评集ID") @PathVariable Long testSetId,
            @Parameter(description = "测试用例ID") @PathVariable Long caseId) {
        testSetService.deleteTestCase(testSetId, caseId);
        return ApiResponse.success();
    }

    @Operation(summary = "导入测试用例", description = "从JSON、CSV或Excel文件导入测试用例到测评集")
    @PostMapping(value = "/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TestSetResponse> importTestCases(
            @Parameter(description = "测评集ID") @PathVariable Long id,
            @Parameter(description = "导入文件") @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(testSetService.importTestCases(id, file));
    }
}
