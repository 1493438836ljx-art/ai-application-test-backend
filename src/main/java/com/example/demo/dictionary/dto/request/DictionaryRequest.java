package com.example.demo.dictionary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 数据字典创建/更新请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "数据字典请求")
public class DictionaryRequest {

    @NotBlank(message = "名称不能为空")
    @Size(min = 2, max = 50, message = "名称长度2-50个字符")
    @Schema(description = "数据字典名称", example = "通用对话测试")
    private String name;

    @Size(max = 500, message = "描述最多500个字符")
    @Schema(description = "字典描述", example = "用于测试模型的基础对话能力")
    private String description;

    @NotEmpty(message = "至少需要1个字段")
    @Size(max = 10, message = "最多支持10个字段")
    @Valid
    @Schema(description = "字段定义列表")
    private List<ColumnRequest> columns;
}
