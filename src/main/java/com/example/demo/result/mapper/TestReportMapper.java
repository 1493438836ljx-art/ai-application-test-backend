package com.example.demo.result.mapper;

import com.example.demo.result.dto.TestReportResponse;
import com.example.demo.result.entity.TestReport;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestReportMapper {

    TestReportResponse toResponse(TestReport report);

    List<TestReportResponse> toResponseList(List<TestReport> reports);
}
