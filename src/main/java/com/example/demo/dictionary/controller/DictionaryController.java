package com.example.demo.dictionary.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.PageResponse;
import com.example.demo.dictionary.dto.request.DictionaryRequest;
import com.example.demo.dictionary.dto.response.*;
import com.example.demo.dictionary.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典管理控制器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
@Tag(name = "数据字典管理", description = "数据字典的增删改查接口")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    /**
     * 分页查询数据字典列表
     *
     * @param keyword 搜索关键词
     * @param page    页码
     * @param size    每页大小
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询数据字典列表", description = "支持按名称、字段key、字段label搜索")
    public ResponseEntity<ApiResponse<PageResponse<DictionaryListResponse>>> getDictionaryList(
            @Parameter(description = "搜索关键词（匹配名称、字段key、字段label）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        log.info("查询数据字典列表, keyword: {}, page: {}, size: {}", keyword, page, size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DictionaryListResponse> result = dictionaryService.getDictionaryList(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<DictionaryListResponse>>builder()
                .code(200)
                .message("success")
                .data(PageResponse.from(result))
                .build());
    }

    /**
     * 获取数据字典详情
     *
     * @param id 数据字典ID
     * @return 详情响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取数据字典详情", description = "根据ID获取数据字典的详细信息，包括字段定义和关联的测评集")
    public ResponseEntity<ApiResponse<DictionaryDetailResponse>> getDictionaryDetail(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id) {

        log.info("获取数据字典详情: {}", id);
        DictionaryDetailResponse response = dictionaryService.getDictionaryDetail(id);
        return ResponseEntity.ok(ApiResponse.<DictionaryDetailResponse>builder()
                .code(200)
                .message("success")
                .data(response)
                .build());
    }

    /**
     * 创建数据字典
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建数据字典", description = "创建一个新的数据字典，包含字段定义")
    public ResponseEntity<ApiResponse<DictionaryDetailResponse>> createDictionary(
            @Valid @RequestBody DictionaryRequest request) {

        log.info("创建数据字典: {}", request.getName());
        DictionaryDetailResponse response = dictionaryService.createDictionary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DictionaryDetailResponse>builder()
                        .code(200)
                        .message("创建成功")
                        .data(response)
                        .build());
    }

    /**
     * 更新数据字典
     *
     * @param id      数据字典ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新数据字典", description = "更新数据字典的基本信息和字段定义")
    public ResponseEntity<ApiResponse<DictionaryDetailResponse>> updateDictionary(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DictionaryRequest request) {

        log.info("更新数据字典: {}", id);
        DictionaryDetailResponse response = dictionaryService.updateDictionary(id, request);
        return ResponseEntity.ok(ApiResponse.<DictionaryDetailResponse>builder()
                .code(200)
                .message("更新成功")
                .data(response)
                .build());
    }

    /**
     * 删除数据字典
     *
     * @param id 数据字典ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据字典", description = "删除指定的数据字典（逻辑删除），需确保没有关联的测评集")
    public ResponseEntity<ApiResponse<Void>> deleteDictionary(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id) {

        log.info("删除数据字典: {}", id);
        dictionaryService.deleteDictionary(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("删除成功")
                .build());
    }

    /**
     * 检查数据字典关联状态
     *
     * @param id 数据字典ID
     * @return 关联状态
     */
    @GetMapping("/{id}/link-status")
    @Operation(summary = "检查数据字典关联状态", description = "检查数据字典是否被测评集关联，用于判断是否可以删除")
    public ResponseEntity<ApiResponse<LinkStatusResponse>> getLinkStatus(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id) {

        log.info("检查数据字典关联状态: {}", id);
        LinkStatusResponse response = dictionaryService.getLinkStatus(id);
        return ResponseEntity.ok(ApiResponse.<LinkStatusResponse>builder()
                .code(200)
                .message("success")
                .data(response)
                .build());
    }

    /**
     * 获取所有数据字典（下拉选择用）
     *
     * @return 简单响应列表
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有数据字典", description = "获取所有数据字典的简单信息，用于下拉选择")
    public ResponseEntity<ApiResponse<List<DictionarySimpleResponse>>> getAllDictionaries() {

        log.info("获取所有数据字典（下拉用）");
        List<DictionarySimpleResponse> response = dictionaryService.getAllDictionaries();
        return ResponseEntity.ok(ApiResponse.<List<DictionarySimpleResponse>>builder()
                .code(200)
                .message("success")
                .data(response)
                .build());
    }

    /**
     * 根据名称获取数据字典的columns
     *
     * @param name 数据字典名称
     * @return columns列表
     */
    @GetMapping("/by-name/{name}/columns")
    @Operation(summary = "根据名称获取columns", description = "根据数据字典名称获取字段定义列表")
    public ResponseEntity<ApiResponse<List<ColumnResponse>>> getColumnsByName(
            @Parameter(description = "数据字典名称", required = true)
            @PathVariable String name) {

        log.info("根据名称获取数据字典columns: {}", name);
        List<ColumnResponse> response = dictionaryService.getColumnsByDictionaryName(name);
        return ResponseEntity.ok(ApiResponse.<List<ColumnResponse>>builder()
                .code(200)
                .message("success")
                .data(response)
                .build());
    }
}
