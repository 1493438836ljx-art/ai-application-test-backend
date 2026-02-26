package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 更新测试用例请求DTO
 * <p>
 * 用于接收更新测试用例的请求数据，所有字段均为可选
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "更新测试用例请求")
public class TestCaseUpdateRequest {

    /** 测试用例序号 */
    @Positive(message = "序号必须为正数")
    @Schema(description = "测试用例序号")
    private Integer sequence;

    /** 输入内容 */
    @Schema(description = "输入内容")
    private String input;

    /** 期望输出 */
    @Schema(description = "期望输出")
    private String expectedOutput;

    /** 标签 */
    @Schema(description = "标签，多个用逗号分隔")
    private String tags;

    /** 额外数据 */
    @Schema(description = "额外数据(JSON格式)")
    private String extraData;
}
