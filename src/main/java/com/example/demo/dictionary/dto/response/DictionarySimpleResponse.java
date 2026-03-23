package com.example.demo.dictionary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据字典简单响应DTO（用于下拉列表）
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据字典简单信息（下拉列表用）")
public class DictionarySimpleResponse {

    @Schema(description = "数据字典ID")
    private Long id;

    @Schema(description = "数据字典名称")
    private String name;

    @Schema(description = "字段数量")
    private Integer columnCount;
}
