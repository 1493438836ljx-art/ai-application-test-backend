package com.huawei.cloudopenlabs.dictionary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据字典详情响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryDetailResponse {

    private Long id;

    private String name;

    private String description;

    private List<ColumnResponse> columns;

    private List<LinkedDatasetResponse> linkedDatasets;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
