package com.huawei.cloudopenlabs.dictionary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据字典简单响应DTO（用于下拉列表）
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionarySimpleResponse {

    private String id;

    private String name;

    private Integer columnCount;
}
