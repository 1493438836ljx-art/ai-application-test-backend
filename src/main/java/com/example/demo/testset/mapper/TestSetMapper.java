package com.example.demo.testset.mapper;

import com.example.demo.testset.dto.*;
import com.example.demo.testset.entity.TestCase;
import com.example.demo.testset.entity.TestSet;
import org.mapstruct.*;

import java.util.List;

/**
 * 测评集对象映射器
 * <p>
 * 使用MapStruct实现DTO与实体类之间的转换
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestSetMapper {

    /**
     * 创建请求DTO转实体
     *
     * @param request 创建请求
     * @return 测评集实体
     */
    TestSet toEntity(TestSetCreateRequest request);

    /**
     * 实体转响应DTO
     *
     * @param testSet 测评集实体
     * @return 响应DTO
     */
    TestSetResponse toResponse(TestSet testSet);

    /**
     * 实体列表转响应DTO列表
     *
     * @param testSets 测评集实体列表
     * @return 响应DTO列表
     */
    List<TestSetResponse> toResponseList(List<TestSet> testSets);

    /**
     * 更新请求DTO更新实体
     * <p>
     * 仅更新非空字段
     * </p>
     *
     * @param request 更新请求
     * @param testSet 目标实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TestSetUpdateRequest request, @MappingTarget TestSet testSet);

    /**
     * 测试用例创建请求DTO转实体
     *
     * @param request 创建请求
     * @return 测试用例实体
     */
    TestCase toEntity(TestCaseCreateRequest request);

    /**
     * 测试用例实体转响应DTO
     *
     * @param testCase 测试用例实体
     * @return 响应DTO
     */
    TestCaseResponse toResponse(TestCase testCase);

    /**
     * 测试用例实体列表转响应DTO列表
     *
     * @param testCases 测试用例实体列表
     * @return 响应DTO列表
     */
    List<TestCaseResponse> toTestCaseResponseList(List<TestCase> testCases);

    /**
     * 测试用例更新请求DTO更新实体
     *
     * @param request  更新请求
     * @param testCase 目标实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TestCaseUpdateRequest request, @MappingTarget TestCase testCase);

    /**
     * 导入项DTO转测试用例实体
     *
     * @param item 导入项
     * @return 测试用例实体
     */
    TestCase toEntity(TestCaseImportItem item);

    /**
     * 导入项DTO列表转测试用例实体列表
     *
     * @param items 导入项列表
     * @return 测试用例实体列表
     */
    List<TestCase> toTestCaseList(List<TestCaseImportItem> items);
}
