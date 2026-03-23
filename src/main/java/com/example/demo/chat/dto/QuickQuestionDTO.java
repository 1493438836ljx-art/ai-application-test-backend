package com.example.demo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 快捷问题DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "快捷问题")
public class QuickQuestionDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "问题文本")
    private String text;

    @Schema(description = "分类")
    private String category;
}
