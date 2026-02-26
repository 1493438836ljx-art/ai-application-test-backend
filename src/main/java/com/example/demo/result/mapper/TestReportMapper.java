package com.example.demo.result.mapper;

import com.example.demo.result.dto.TestReportResponse;
import com.example.demo.result.entity.TestReport;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 测试报告对象转换器接口
 *
 * 提供TestReport实体与TestReportResponse DTO之间的转换功能
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestReportMapper {

    /**
     * 将测试报告实体转换为响应DTO
     *
     * @param report 测试报告实体
     * @return 测试报告响应DTO
     */
    TestReportResponse toResponse(TestReport report);

    /**
     * 将测试报告实体列表转换为响应DTO列表
     *
     * @param reports 测试报告实体列表
     * @return 测试报告响应DTO列表
     */
    List<TestReportResponse> toResponseList(List<TestReport> reports);
}
