/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.dictionary.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 数据字典创建/更新请求DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
public class DictionaryRequest {

    @NotBlank(message = "名称不能为空")
    @Size(min = 2, max = 50, message = "名称长度2-50个字符")
    private String name;

    @Size(max = 500, message = "描述最多500个字符")
    private String description;

    @NotEmpty(message = "至少需要1个字段")
    @Size(max = 10, message = "最多支持10个字段")
    @Valid
    private List<ColumnRequest> columns;
}
