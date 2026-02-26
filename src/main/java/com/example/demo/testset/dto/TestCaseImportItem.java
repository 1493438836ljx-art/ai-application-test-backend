package com.example.demo.testset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseImportItem {

    private Integer sequence;
    private String input;
    private String expectedOutput;
    private String tags;
    private String extraData;
}
