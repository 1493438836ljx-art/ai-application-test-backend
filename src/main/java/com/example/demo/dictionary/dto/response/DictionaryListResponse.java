package com.example.demo.dictionary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据字典列表响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据字典列表项")
public class DictionaryListResponse {

    @Schema(description = "数据字典ID")
    private Long id;

    @Schema(description = "数据字典名称")
    private String name;

    @Schema(description = "字典描述")
    private String description;

    @Schema(description = "字段数量")
    private Integer columnCount;

    @Schema(description = "关联的测评集数量")
    private Integer linkedDatasetCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
