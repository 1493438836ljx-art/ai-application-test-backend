/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.dictionary.controller;

import com.huawei.cloudopenlabs.common.dto.ApiResponse;
import com.huawei.cloudopenlabs.common.dto.PageResponse;
import com.huawei.cloudopenlabs.dictionary.dto.request.DictionaryRequest;
import com.huawei.cloudopenlabs.dictionary.dto.response.*;
import com.huawei.cloudopenlabs.dictionary.dto.response.*;
import com.huawei.cloudopenlabs.dictionary.service.DictionaryService;
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
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Slf4j
@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
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
    public ResponseEntity<ApiResponse<PageResponse<DictionaryListResponse>>> getDictionaryList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Querying data dictionary list, keyword: {}, page: {}, size: {}", keyword, page, size);
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
    public ResponseEntity<ApiResponse<DictionaryDetailResponse>> getDictionaryDetail(
            @PathVariable String id) {

        log.info("Getting data dictionary details: {}", id);
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
    public ResponseEntity<ApiResponse<DictionaryDetailResponse>> createDictionary(
            @Valid @RequestBody DictionaryRequest request) {

        log.info("Creating data dictionary: {}", request.getName());
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
    public ResponseEntity<ApiResponse<DictionaryDetailResponse>> updateDictionary(
            @PathVariable String id,
            @Valid @RequestBody DictionaryRequest request) {

        log.info("Updating data dictionary: {}", id);
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
    public ResponseEntity<ApiResponse<Void>> deleteDictionary(
            @PathVariable String id) {

        log.info("Deleting data dictionary: {}", id);
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
    public ResponseEntity<ApiResponse<LinkStatusResponse>> getLinkStatus(
            @PathVariable String id) {

        log.info("Checking data dictionary association status: {}", id);
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
    public ResponseEntity<ApiResponse<List<DictionarySimpleResponse>>> getAllDictionaries() {

        log.info("Getting all data dictionaries (for dropdown)");
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
    public ResponseEntity<ApiResponse<List<ColumnResponse>>> getColumnsByName(
            @PathVariable String name) {

        log.info("Getting data dictionary columns by name: {}", name);
        List<ColumnResponse> response = dictionaryService.getColumnsByDictionaryName(name);
        return ResponseEntity.ok(ApiResponse.<List<ColumnResponse>>builder()
                .code(200)
                .message("success")
                .data(response)
                .build());
    }
}
