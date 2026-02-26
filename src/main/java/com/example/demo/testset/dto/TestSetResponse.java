package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测评集响应DTO
 * <p>
 * 用于返回测评集的详细信息
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测评集响应")
public class TestSetResponse {

    /** 测评集ID */
    @Schema(description = "测评集ID")
    private Long id;

    /** 测评集名称 */
    @Schema(description = "测评集名称")
    private String name;

    /** 测评集描述 */
    @Schema(description = "测评集描述")
    private String description;

    /** 测试用例总数 */
    @Schema(description = "测试用例总数")
    private Integer totalCases;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
