package com.huawei.cloudopenlabs.dictionary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 字段定义响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnResponse {

    private Long id;

    private String key;

    private String label;

    private String type;

    private List<String> enumOptions;

    private BigDecimal min;

    private BigDecimal max;

    private String description;
}
