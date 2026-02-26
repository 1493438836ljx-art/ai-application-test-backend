package com.example.demo.testset.mapper;

import com.example.demo.testset.dto.*;
import com.example.demo.testset.entity.TestCase;
import com.example.demo.testset.entity.TestSet;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestSetMapper {

    TestSet toEntity(TestSetCreateRequest request);

    TestSetResponse toResponse(TestSet testSet);

    List<TestSetResponse> toResponseList(List<TestSet> testSets);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TestSetUpdateRequest request, @MappingTarget TestSet testSet);

    TestCase toEntity(TestCaseCreateRequest request);

    TestCaseResponse toResponse(TestCase testCase);

    List<TestCaseResponse> toTestCaseResponseList(List<TestCase> testCases);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TestCaseUpdateRequest request, @MappingTarget TestCase testCase);

    TestCase toEntity(TestCaseImportItem item);

    List<TestCase> toTestCaseList(List<TestCaseImportItem> items);
}
