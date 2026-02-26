package com.example.demo.testset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 测评集导入请求DTO
 * <p>
 * 用于JSON格式导入测试用例时的数据结构
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSetImportRequest {

    /** 测评集名称 */
    private String name;

    /** 测评集描述 */
    private String description;

    /** 要导入的测试用例列表 */
    private List<TestCaseImportItem> testCases;
}
