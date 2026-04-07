package com.huawei.cloudopenlabs.workflow.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流验证结果DTO
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
public class ValidationResult {

    private boolean valid;

    private List<ValidationError> errors = new ArrayList<>();

    private List<ValidationWarning> warnings = new ArrayList<>();

    /**
     * 添加错误
     *
     * @param code     错误代码
     * @param message  错误消息
     * @param nodeUuid 关联的节点UUID
     * @param field    关联的字段
     */
    public void addError(String code, String message, String nodeUuid, String field) {
        ValidationError error = new ValidationError();
        error.setCode(code);
        error.setMessage(message);
        error.setNodeUuid(nodeUuid);
        error.setField(field);
        errors.add(error);
        this.valid = false;
    }

    /**
     * 添加警告
     *
     * @param code     警告代码
     * @param message  警告消息
     * @param nodeUuid 关联的节点UUID
     */
    public void addWarning(String code, String message, String nodeUuid) {
        ValidationWarning warning = new ValidationWarning();
        warning.setCode(code);
        warning.setMessage(message);
        warning.setNodeUuid(nodeUuid);
        warnings.add(warning);
    }

    /**
     * 合并另一个验证结果
     *
     * @param other 其他验证结果
     */
    public void merge(ValidationResult other) {
        if (other == null) {
            return;
        }
        if (!other.isValid()) {
            this.valid = false;
        }
        this.errors.addAll(other.getErrors());
        this.warnings.addAll(other.getWarnings());
    }

    /**
     * 验证错误
     */
    @Data
    public static class ValidationError {
        private String code;

        private String message;

        private String nodeUuid;

        private String field;
    }

    /**
     * 验证警告
     */
    @Data
    public static class ValidationWarning {
        private String code;

        private String message;

        private String nodeUuid;
    }
}
