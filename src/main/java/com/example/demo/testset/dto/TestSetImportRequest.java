package com.example.demo.testset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSetImportRequest {

    private String name;
    private String description;
    private List<TestCaseImportItem> testCases;
}
