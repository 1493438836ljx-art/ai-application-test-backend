package com.example.demo.dictionary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 字段定义响应DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字段定义信息")
public class ColumnResponse {

    @Schema(description = "字段ID")
    private Long id;

    @Schema(description = "字段Key（英文标识）")
    private String key;

    @Schema(description = "字段名称（中文显示名）")
    private String label;

    @Schema(description = "字段类型")
    private String type;

    @Schema(description = "枚举选项（仅enum类型）")
    private List<String> enumOptions;

    @Schema(description = "最小值（仅number类型）")
    private BigDecimal min;

    @Schema(description = "最大值（仅number类型）")
    private BigDecimal max;

    @Schema(description = "字段描述")
    private String description;
}
