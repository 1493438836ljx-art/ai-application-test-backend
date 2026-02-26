package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "更新测试用例请求")
public class TestCaseUpdateRequest {

    @Positive(message = "序号必须为正数")
    @Schema(description = "测试用例序号")
    private Integer sequence;

    @Schema(description = "输入内容")
    private String input;

    @Schema(description = "期望输出")
    private String expectedOutput;

    @Schema(description = "标签，多个用逗号分隔")
    private String tags;

    @Schema(description = "额外数据(JSON格式)")
    private String extraData;
}
