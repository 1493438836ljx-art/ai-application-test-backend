package com.example.demo.environment.entity;

import com.example.demo.common.enums.EnvironmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 环境实体类
 * <p>
 * 用于存储AI应用/软件环境配置信息，包括API端点、认证配置等
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
@Table(name = "environment")
public class Environment {

    /** 环境唯一标识ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 环境名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 环境描述 */
    @Column(length = 500)
    private String description;

    /** 环境类型（HTTP_API/SDK） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EnvironmentType type;

    /** 环境配置信息（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String config;

    /** API端点URL */
    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    /** 认证类型（NONE/API_KEY/BEARER/BASIC等） */
    @Column(name = "auth_type", length = 50)
    private String authType;

    /** 认证配置信息（JSON格式） */
    @Column(name = "auth_config", columnDefinition = "TEXT")
    private String authConfig;

    /** 是否激活状态 */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
