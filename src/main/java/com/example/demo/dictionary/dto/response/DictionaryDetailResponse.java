package com.example.demo.dictionary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据字典详情响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据字典详情")
public class DictionaryDetailResponse {

    @Schema(description = "数据字典ID")
    private Long id;

    @Schema(description = "数据字典名称")
    private String name;

    @Schema(description = "字典描述")
    private String description;

    @Schema(description = "字段定义列表")
    private List<ColumnResponse> columns;

    @Schema(description = "关联的测评集列表")
    private List<LinkedDatasetResponse> linkedDatasets;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
