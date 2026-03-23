package com.example.demo.dictionary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dictionary.dto.request.ColumnRequest;
import com.example.demo.dictionary.dto.request.DictionaryRequest;
import com.example.demo.dictionary.dto.response.*;
import com.example.demo.dictionary.entity.DataDictionary;
import com.example.demo.dictionary.entity.DictionaryColumn;
import com.example.demo.dictionary.mapper.DataDictionaryMapper;
import com.example.demo.dictionary.mapper.DictionaryColumnMapper;
import com.example.demo.dictionary.service.DictionaryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据字典服务实现类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final DataDictionaryMapper dictionaryMapper;
    private final DictionaryColumnMapper columnMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DictionaryListResponse> getDictionaryList(String keyword, Pageable pageable) {
        log.info("查询数据字典列表, keyword: {}, page: {}, size: {}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<DataDictionary> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        IPage<DataDictionary> result = dictionaryMapper.searchByKeyword(page, keyword);

        List<DictionaryListResponse> content = result.getRecords().stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, result.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public DictionaryDetailResponse getDictionaryDetail(Long id) {
        log.info("获取数据字典详情: {}", id);

        DataDictionary dictionary = dictionaryMapper.selectById(id);
        if (dictionary == null || dictionary.getIsDeleted() == 1) {
            throw new RuntimeException("数据字典不存在: " + id);
        }

        return convertToDetailResponse(dictionary);
    }

    @Override
    @Transactional
    public DictionaryDetailResponse createDictionary(DictionaryRequest request) {
        log.info("创建数据字典: {}", request.getName());

        // 检查名称是否重复
        LambdaQueryWrapper<DataDictionary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataDictionary::getName, request.getName())
               .eq(DataDictionary::getIsDeleted, 0);
        if (dictionaryMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("数据字典名称已存在: " + request.getName());
        }

        // 创建数据字典
        DataDictionary dictionary = new DataDictionary();
        dictionary.setName(request.getName());
        dictionary.setDescription(request.getDescription());
        dictionary.setIsDeleted(0);
        dictionaryMapper.insert(dictionary);

        // 创建字段定义
        List<DictionaryColumn> columns = createColumns(dictionary.getId(), request.getColumns());
        for (DictionaryColumn column : columns) {
            columnMapper.insert(column);
        }

        return getDictionaryDetail(dictionary.getId());
    }

    @Override
    @Transactional
    public DictionaryDetailResponse updateDictionary(Long id, DictionaryRequest request) {
        log.info("更新数据字典: {}", id);

        DataDictionary dictionary = dictionaryMapper.selectById(id);
        if (dictionary == null || dictionary.getIsDeleted() == 1) {
            throw new RuntimeException("数据字典不存在: " + id);
        }

        // 检查名称是否重复（排除自身）
        LambdaQueryWrapper<DataDictionary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataDictionary::getName, request.getName())
               .eq(DataDictionary::getIsDeleted, 0)
               .ne(DataDictionary::getId, id);
        if (dictionaryMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("数据字典名称已存在: " + request.getName());
        }

        // 更新基本信息
        dictionary.setName(request.getName());
        dictionary.setDescription(request.getDescription());
        dictionaryMapper.updateById(dictionary);

        // 删除旧字段，插入新字段
        columnMapper.deleteByDictionaryId(id);
        List<DictionaryColumn> columns = createColumns(id, request.getColumns());
        for (DictionaryColumn column : columns) {
            columnMapper.insert(column);
        }

        return getDictionaryDetail(id);
    }

    @Override
    @Transactional
    public void deleteDictionary(Long id) {
        log.info("删除数据字典: {}", id);

        DataDictionary dictionary = dictionaryMapper.selectById(id);
        if (dictionary == null || dictionary.getIsDeleted() == 1) {
            throw new RuntimeException("数据字典不存在: " + id);
        }

        // 检查是否有关联的测评集
        LinkStatusResponse linkStatus = getLinkStatus(id);
        if (!linkStatus.getCanDelete()) {
            throw new RuntimeException("该字典已被" + linkStatus.getLinkedDatasetCount() + "个测评集关联，无法删除");
        }

        // 删除关联字段
        columnMapper.deleteByDictionaryId(id);

        // 逻辑删除字典
        dictionaryMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public LinkStatusResponse getLinkStatus(Long id) {
        log.info("检查数据字典关联状态: {}", id);

        // TODO: 实际实现时需要查询测评集表
        // 目前返回模拟数据，表示没有关联
        // 实际应该类似：SELECT COUNT(*) FROM dataset WHERE dictionary_id = #{dictionaryId}

        List<LinkedDatasetResponse> linkedDatasets = getLinkedDatasets(id);
        int count = linkedDatasets.size();

        return LinkStatusResponse.builder()
                .canDelete(count == 0)
                .linkedDatasetCount(count)
                .linkedDatasets(linkedDatasets)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DictionarySimpleResponse> getAllDictionaries() {
        log.info("获取所有数据字典（下拉用）");

        List<DataDictionary> dictionaries = dictionaryMapper.selectAllForDropdown();
        return dictionaries.stream()
                .map(this::convertToSimpleResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建字段定义列表
     */
    private List<DictionaryColumn> createColumns(Long dictionaryId, List<ColumnRequest> requests) {
        List<DictionaryColumn> columns = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            ColumnRequest request = requests.get(i);
            DictionaryColumn column = new DictionaryColumn();
            column.setDictionaryId(dictionaryId);
            column.setColumnKey(request.getKey());
            column.setColumnLabel(request.getLabel());
            column.setColumnType(request.getType());
            column.setSortOrder(i);

            // 处理枚举选项
            if ("enum".equals(request.getType()) && request.getEnumOptions() != null) {
                try {
                    column.setEnumOptions(objectMapper.writeValueAsString(request.getEnumOptions()));
                } catch (JsonProcessingException e) {
                    log.error("序列化枚举选项失败", e);
                }
            }

            // 处理数值范围
            if ("number".equals(request.getType())) {
                column.setMinValue(request.getMin());
                column.setMaxValue(request.getMax());
            }

            columns.add(column);
        }
        return columns;
    }

    /**
     * 获取关联的测评集列表
     * TODO: 实际实现时需要关联测评集表查询
     */
    private List<LinkedDatasetResponse> getLinkedDatasets(Long dictionaryId) {
        // 模拟返回空列表
        // 实际应该查询 dataset 表：SELECT * FROM dataset WHERE dictionary_id = #{dictionaryId}
        return Collections.emptyList();
    }

    /**
     * 转换为列表响应
     */
    private DictionaryListResponse convertToListResponse(DataDictionary dictionary) {
        int columnCount = columnMapper.countByDictionaryId(dictionary.getId());
        List<LinkedDatasetResponse> linkedDatasets = getLinkedDatasets(dictionary.getId());

        return DictionaryListResponse.builder()
                .id(dictionary.getId())
                .name(dictionary.getName())
                .description(dictionary.getDescription())
                .columnCount(columnCount)
                .linkedDatasetCount(linkedDatasets.size())
                .createdAt(dictionary.getCreatedAt())
                .updatedAt(dictionary.getUpdatedAt())
                .build();
    }

    /**
     * 转换为详情响应
     */
    private DictionaryDetailResponse convertToDetailResponse(DataDictionary dictionary) {
        List<DictionaryColumn> columns = columnMapper.selectByDictionaryId(dictionary.getId());
        List<ColumnResponse> columnResponses = columns.stream()
                .map(this::convertToColumnResponse)
                .collect(Collectors.toList());

        List<LinkedDatasetResponse> linkedDatasets = getLinkedDatasets(dictionary.getId());

        return DictionaryDetailResponse.builder()
                .id(dictionary.getId())
                .name(dictionary.getName())
                .description(dictionary.getDescription())
                .columns(columnResponses)
                .linkedDatasets(linkedDatasets)
                .createdAt(dictionary.getCreatedAt())
                .updatedAt(dictionary.getUpdatedAt())
                .build();
    }

    /**
     * 转换为字段响应
     */
    private ColumnResponse convertToColumnResponse(DictionaryColumn column) {
        List<String> enumOptions = null;
        if (column.getEnumOptions() != null && !column.getEnumOptions().isEmpty()) {
            try {
                enumOptions = objectMapper.readValue(column.getEnumOptions(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.error("反序列化枚举选项失败", e);
            }
        }

        return ColumnResponse.builder()
                .id(column.getId())
                .key(column.getColumnKey())
                .label(column.getColumnLabel())
                .type(column.getColumnType())
                .enumOptions(enumOptions)
                .min(column.getMinValue())
                .max(column.getMaxValue())
                .build();
    }

    /**
     * 转换为简单响应
     */
    private DictionarySimpleResponse convertToSimpleResponse(DataDictionary dictionary) {
        int columnCount = columnMapper.countByDictionaryId(dictionary.getId());

        return DictionarySimpleResponse.builder()
                .id(dictionary.getId())
                .name(dictionary.getName())
                .columnCount(columnCount)
                .build();
    }
}
