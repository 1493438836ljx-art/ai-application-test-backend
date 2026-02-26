package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试用例响应DTO
 * <p>
 * 用于返回测试用例的详细信息
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测试用例响应")
public class TestCaseResponse {

    /** 测试用例ID */
    @Schema(description = "测试用例ID")
    private Long id;

    /** 所属测评集ID */
    @Schema(description = "所属测评集ID")
    private Long testSetId;

    /** 测试用例序号 */
    @Schema(description = "测试用例序号")
    private Integer sequence;

    /** 输入内容 */
    @Schema(description = "输入内容")
    private String input;

    /** 期望输出 */
    @Schema(description = "期望输出")
    private String expectedOutput;

    /** 标签 */
    @Schema(description = "标签")
    private String tags;

    /** 额外数据 */
    @Schema(description = "额外数据")
    private String extraData;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
