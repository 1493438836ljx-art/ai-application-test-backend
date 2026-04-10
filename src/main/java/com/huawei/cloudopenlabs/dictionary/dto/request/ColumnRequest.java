/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.dictionary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 字段定义请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ColumnRequest {

    private String id;

    @NotBlank(message = "字段Key不能为空")
    @Size(min = 1, max = 50, message = "字段Key长度1-50个字符")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "字段Key必须以字母开头，只能包含字母、数字和下划线")
    private String key;

    @NotBlank(message = "字段名称不能为空")
    @Size(min = 1, max = 50, message = "字段名称长度1-50个字符")
    private String label;

    @NotBlank(message = "字段类型不能为空")
    @Pattern(regexp = "^(string|number|enum)$", message = "字段类型必须是string、number或enum")
    private String type;

    private List<String> enumOptions;

    private BigDecimal min;

    private BigDecimal max;

    private String description;
}
