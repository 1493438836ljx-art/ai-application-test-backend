package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 创建测试用例请求DTO
 * <p>
 * 用于接收创建新测试用例的请求数据
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "创建测试用例请求")
public class TestCaseCreateRequest {

    /** 测试用例序号，用于确定执行顺序 */
    @NotNull(message = "测试用例序号不能为空")
    @Positive(message = "序号必须为正数")
    @Schema(description = "测试用例序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sequence;

    /** 输入内容，发送给AI应用的提示词或问题 */
    @Schema(description = "输入内容")
    private String input;

    /** 期望输出，用于评估AI应用的回答质量 */
    @Schema(description = "期望输出")
    private String expectedOutput;

    /** 标签，多个用逗号分隔 */
    @Schema(description = "标签，多个用逗号分隔")
    private String tags;

    /** 额外数据，JSON格式存储扩展信息 */
    @Schema(description = "额外数据(JSON格式)")
    private String extraData;
}
