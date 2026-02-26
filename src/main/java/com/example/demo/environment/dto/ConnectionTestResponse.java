package com.example.demo.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连接测试响应DTO
 * <p>
 * 用于返回环境连接测试的结果信息
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "连接测试响应")
public class ConnectionTestResponse {

    /** 是否连接成功 */
    @Schema(description = "是否连接成功")
    private Boolean success;

    /** 响应消息 */
    @Schema(description = "响应消息")
    private String message;

    /** 响应时间（毫秒） */
    @Schema(description = "响应时间（毫秒）")
    private Long responseTimeMs;

    /** 测试输出（如果有） */
    @Schema(description = "测试输出（如果有）")
    private String testOutput;
}
