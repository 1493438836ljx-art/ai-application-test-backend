package com.example.demo.prompt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Prompt模板实体类
 * <p>
 * 存储可复用的Prompt模板，支持变量占位符（如{{variableName}}），
 * 在测试执行时动态替换为实际值。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prompt")
public class Prompt {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Prompt名称，用于标识和查找 */
    @Column(nullable = false, length = 200)
    private String name;

    /** Prompt描述 */
    @Column(length = 500)
    private String description;

    /** 模板内容，支持{{变量名}}格式的占位符 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String template;

    /** 模板中的变量名列表，逗号分隔 */
    @Column(name = "variable_names", length = 1000)
    private String variableNames;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
