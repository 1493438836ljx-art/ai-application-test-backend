package com.example.demo.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连接测试请求DTO
 * <p>
 * 用于接收环境连接测试的请求参数
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "连接测试请求")
public class ConnectionTestRequest {

    /** 测试输入内容（可选） */
    @Schema(description = "测试输入内容（可选）")
    private String testInput;
}
