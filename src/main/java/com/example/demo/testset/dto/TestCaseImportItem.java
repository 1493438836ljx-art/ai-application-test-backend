package com.example.demo.testset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试用例导入项DTO
 * <p>
 * 用于从外部文件（JSON/CSV/Excel）导入测试用例时的数据结构
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseImportItem {

    /** 测试用例序号 */
    private Integer sequence;

    /** 输入内容 */
    private String input;

    /** 期望输出 */
    private String expectedOutput;

    /** 标签 */
    private String tags;

    /** 额外数据 */
    private String extraData;
}
