package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测试用例响应")
public class TestCaseResponse {

    @Schema(description = "测试用例ID")
    private Long id;

    @Schema(description = "所属测评集ID")
    private Long testSetId;

    @Schema(description = "测试用例序号")
    private Integer sequence;

    @Schema(description = "输入内容")
    private String input;

    @Schema(description = "期望输出")
    private String expectedOutput;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "额外数据")
    private String extraData;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
