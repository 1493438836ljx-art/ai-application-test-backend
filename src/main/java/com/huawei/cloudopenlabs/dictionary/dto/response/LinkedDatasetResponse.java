/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.dictionary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关联的测评集响应DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedDatasetResponse {

    private String id;

    private String name;

    private Integer dataCount;

    private String testType;
}
