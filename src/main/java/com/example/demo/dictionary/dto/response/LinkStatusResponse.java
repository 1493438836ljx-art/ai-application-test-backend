package com.example.demo.dictionary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据字典关联状态响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据字典关联状态")
public class LinkStatusResponse {

    @Schema(description = "是否可以删除")
    private Boolean canDelete;

    @Schema(description = "关联的测评集数量")
    private Integer linkedDatasetCount;

    @Schema(description = "关联的测评集列表")
    private List<LinkedDatasetResponse> linkedDatasets;
}
