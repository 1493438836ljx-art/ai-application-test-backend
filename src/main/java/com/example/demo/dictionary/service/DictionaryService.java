package com.example.demo.dictionary.service;

import com.example.demo.dictionary.dto.request.DictionaryRequest;
import com.example.demo.dictionary.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 数据字典服务接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface DictionaryService {

    /**
     * 分页查询数据字典列表
     *
     * @param keyword  搜索关键词（匹配名称、字段key、字段label）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<DictionaryListResponse> getDictionaryList(String keyword, Pageable pageable);

    /**
     * 获取数据字典详情
     *
     * @param id 数据字典ID
     * @return 详情响应
     */
    DictionaryDetailResponse getDictionaryDetail(Long id);

    /**
     * 创建数据字典
     *
     * @param request 创建请求
     * @return 创建结果
     */
    DictionaryDetailResponse createDictionary(DictionaryRequest request);

    /**
     * 更新数据字典
     *
     * @param id      数据字典ID
     * @param request 更新请求
     * @return 更新结果
     */
    DictionaryDetailResponse updateDictionary(Long id, DictionaryRequest request);

    /**
     * 删除数据字典
     *
     * @param id 数据字典ID
     */
    void deleteDictionary(Long id);

    /**
     * 检查数据字典关联状态
     *
     * @param id 数据字典ID
     * @return 关联状态
     */
    LinkStatusResponse getLinkStatus(Long id);

    /**
     * 获取所有数据字典（下拉选择用）
     *
     * @return 简单响应列表
     */
    List<DictionarySimpleResponse> getAllDictionaries();

    /**
     * 根据名称获取数据字典的columns
     *
     * @param name 数据字典名称
     * @return columns列表
     */
    List<ColumnResponse> getColumnsByDictionaryName(String name);
}
