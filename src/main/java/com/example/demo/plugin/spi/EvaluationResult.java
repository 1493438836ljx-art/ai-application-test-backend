package com.example.demo.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评估结果类，封装评估插件评估后的返回结果
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    /** 评估是否成功 */
    private boolean success;

    /** 评估得分，0到1之间的分数值 */
    private double score;

    /** 评估原因，描述评估结果的详细说明 */
    private String reason;

    /** 错误信息，评估失败时的错误描述 */
    private String errorMessage;
}
