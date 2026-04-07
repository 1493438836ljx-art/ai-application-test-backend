package com.huawei.cloudopenlabs.dictionary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据字典列表响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryListResponse {

    private Long id;

    private String name;

    private String description;

    private Integer columnCount;

    private Integer linkedDatasetCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
