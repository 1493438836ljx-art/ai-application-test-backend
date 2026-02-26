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
@Schema(description = "连接测试响应")
public class ConnectionTestResponse {

    @Schema(description = "是否连接成功")
    private Boolean success;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应时间（毫秒）")
    private Long responseTimeMs;

    @Schema(description = "测试输出（如果有）")
    private String testOutput;
}
