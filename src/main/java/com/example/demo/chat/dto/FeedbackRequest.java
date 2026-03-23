package com.example.demo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 反馈请求DTO
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "反馈请求")
public class FeedbackRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    @Schema(description = "评分（1-5）", required = true, example = "5")
    private Integer rating;

    @Schema(description = "反馈类型", example = "helpful")
    private String feedbackType;

    @Schema(description = "反馈评论", example = "回答很有帮助")
    private String comment;

    @Schema(description = "用户ID")
    private String userId;
}
