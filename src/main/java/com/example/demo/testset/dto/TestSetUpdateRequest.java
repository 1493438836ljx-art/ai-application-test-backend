package com.example.demo.testset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新测评集请求")
public class TestSetUpdateRequest {

    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "测评集名称")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    @Schema(description = "测评集描述")
    private String description;
}
