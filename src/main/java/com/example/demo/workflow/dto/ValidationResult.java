package com.example.demo.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "工作流验证结果")
public class ValidationResult {

    @Schema(description = "是否验证通过")
    private boolean valid;

    @Schema(description = "错误列表")
    private List<ValidationError> errors = new ArrayList<>();

    @Schema(description = "警告列表")
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
    @Schema(description = "验证错误")
    public static class ValidationError {
        @Schema(description = "错误代码", example = "MISSING_START_NODE")
        private String code;

        @Schema(description = "错误消息", example = "工作流必须包含一个开始节点")
        private String message;

        @Schema(description = "关联的节点UUID")
        private String nodeUuid;

        @Schema(description = "关联的字段")
        private String field;
    }

    /**
     * 验证警告
     */
    @Data
    @Schema(description = "验证警告")
    public static class ValidationWarning {
        @Schema(description = "警告代码", example = "ORPHAN_NODE")
        private String code;

        @Schema(description = "警告消息", example = "节点没有连接到任何其他节点")
        private String message;

        @Schema(description = "关联的节点UUID")
        private String nodeUuid;
    }
}
