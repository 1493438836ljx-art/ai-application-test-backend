package com.example.demo.workflow.execution.executor;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点验证结果
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
public class ValidationResult {

    /**
     * 是否验证通过
     */
    private boolean success;

    /**
     * 错误信息列表
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * 警告信息列表
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * 创建成功结果
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .success(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ValidationResult failure(String errorMessage) {
        List<String> errors = new ArrayList<>();
        errors.add(errorMessage);
        return ValidationResult.builder()
                .success(false)
                .errors(errors)
                .build();
    }

    /**
     * 创建失败结果（多个错误）
     */
    public static ValidationResult failure(List<String> errorMessages) {
        return ValidationResult.builder()
                .success(false)
                .errors(errorMessages)
                .build();
    }

    /**
     * 添加错误
     */
    public ValidationResult addError(String error) {
        this.errors.add(error);
        this.success = false;
        return this;
    }

    /**
     * 添加警告
     */
    public ValidationResult addWarning(String warning) {
        this.warnings.add(warning);
        return this;
    }

    /**
     * 合并验证结果
     */
    public ValidationResult merge(ValidationResult other) {
        if (other != null) {
            this.errors.addAll(other.getErrors());
            this.warnings.addAll(other.getWarnings());
            if (!other.isSuccess()) {
                this.success = false;
            }
        }
        return this;
    }
}
