package com.example.demo.plugin.entity;

import com.example.demo.common.enums.PluginType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 插件实体类，表示系统中的执行插件或评估插件配置
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plugin")
public class Plugin {

    /** 插件ID，主键自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 插件名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 插件描述 */
    @Column(length = 500)
    private String description;

    /** 插件类型（执行插件/评估插件） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PluginType type;

    /** 插件实现类的全限定名 */
    @Column(name = "class_name", length = 500)
    private String className;

    /** 默认配置，JSON格式 */
    @Column(columnDefinition = "TEXT")
    private String defaultConfig;

    /** 是否为内置插件 */
    @Column(name = "is_builtin")
    @Builder.Default
    private Boolean isBuiltin = false;

    /** 是否激活 */
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
