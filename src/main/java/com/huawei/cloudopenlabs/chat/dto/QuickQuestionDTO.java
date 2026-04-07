package com.huawei.cloudopenlabs.chat.dto;

import lombok.Data;

/**
 * 快捷问题DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class QuickQuestionDTO {

    private Long id;

    private String icon;

    private String text;

    private String category;
}
