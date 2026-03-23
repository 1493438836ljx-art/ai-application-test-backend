package com.example.demo.dictionary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关联的测评集响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关联的测评集信息")
public class LinkedDatasetResponse {

    @Schema(description = "测评集ID")
    private String id;

    @Schema(description = "测评集名称")
    private String name;

    @Schema(description = "数据条数")
    private Integer dataCount;

    @Schema(description = "测试类型")
    private String testType;
}
