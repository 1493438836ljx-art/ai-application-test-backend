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
@Schema(description = "测评集响应")
public class TestSetResponse {

    @Schema(description = "测评集ID")
    private Long id;

    @Schema(description = "测评集名称")
    private String name;

    @Schema(description = "测评集描述")
    private String description;

    @Schema(description = "测试用例总数")
    private Integer totalCases;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
