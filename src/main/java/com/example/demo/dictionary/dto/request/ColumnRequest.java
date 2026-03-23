package com.example.demo.dictionary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 字段定义请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "字段定义请求")
public class ColumnRequest {

    @Schema(description = "字段ID（更新时传入）")
    private Long id;

    @NotBlank(message = "字段Key不能为空")
    @Size(min = 1, max = 50, message = "字段Key长度1-50个字符")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "字段Key必须以字母开头，只能包含字母、数字和下划线")
    @Schema(description = "字段Key（英文标识）", example = "input")
    private String key;

    @NotBlank(message = "字段名称不能为空")
    @Size(min = 1, max = 50, message = "字段名称长度1-50个字符")
    @Schema(description = "字段名称（中文显示名）", example = "输入")
    private String label;

    @NotBlank(message = "字段类型不能为空")
    @Pattern(regexp = "^(string|number|enum)$", message = "字段类型必须是string、number或enum")
    @Schema(description = "字段类型", example = "string", allowableValues = {"string", "number", "enum"})
    private String type;

    @Schema(description = "枚举选项（仅enum类型使用）", example = "[\"选项1\", \"选项2\"]")
    private List<String> enumOptions;

    @Schema(description = "最小值（仅number类型使用）", example = "0")
    private BigDecimal min;

    @Schema(description = "最大值（仅number类型使用）", example = "100")
    private BigDecimal max;

    @Schema(description = "字段描述（用于详情展示）", example = "用户输入内容")
    private String description;
}
