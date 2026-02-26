package com.example.demo.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "连接测试请求")
public class ConnectionTestRequest {

    @Schema(description = "测试输入内容（可选）")
    private String testInput;
}
