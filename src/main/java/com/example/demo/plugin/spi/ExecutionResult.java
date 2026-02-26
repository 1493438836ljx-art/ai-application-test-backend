package com.example.demo.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行结果类，封装执行插件执行后的返回结果
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /** 执行是否成功 */
    private boolean success;

    /** 输出内容，AI推理返回的结果 */
    private String output;

    /** 错误信息，执行失败时的错误描述 */
    private String errorMessage;

    /** 执行耗时（毫秒） */
    private Long executionTimeMs;

    /** Token使用量 */
    private Integer tokenUsage;
}
