package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "创建测试用例请求")
public class TestCaseCreateRequest {

    @NotNull(message = "测试用例序号不能为空")
    @Positive(message = "序号必须为正数")
    @Schema(description = "测试用例序号", requiredMode = Schema.RequiredMode.REQUIRED)
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
