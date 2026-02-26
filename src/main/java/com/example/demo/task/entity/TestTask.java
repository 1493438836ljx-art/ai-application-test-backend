package com.example.demo.task.entity;

import com.example.demo.common.enums.TestTaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 测试任务实体类
 * 用于存储测试任务的基本信息和执行状态
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "test_task")
public class TestTask {

    /** 任务唯一标识ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 任务描述 */
    @Column(length = 1000)
    private String description;

    /** 关联的测评集ID */
    @Column(name = "test_set_id", nullable = false)
    private Long testSetId;

    /** 关联的测试环境ID */
    @Column(name = "environment_id", nullable = false)
    private Long environmentId;

    /** 执行插件ID */
    @Column(name = "execution_plugin_id", nullable = false)
    private Long executionPluginId;

    /** 评估插件ID */
    @Column(name = "evaluation_plugin_id", nullable = false)
    private Long evaluationPluginId;

    /** 任务状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TestTaskStatus status = TestTaskStatus.PENDING;

    /** 总执行项数 */
    @Column(name = "total_items")
    private Integer totalItems;

    /** 已完成项数 */
    @Column(name = "completed_items")
    @Builder.Default
    private Integer completedItems = 0;

    /** 执行成功的项数 */
    @Column(name = "success_items")
    @Builder.Default
    private Integer successItems = 0;

    /** 执行失败的项数 */
    @Column(name = "failed_items")
    @Builder.Default
    private Integer failedItems = 0;

    /** 执行插件配置(JSON格式) */
    @Column(name = "execution_config", columnDefinition = "TEXT")
    private String executionConfig;

    /** 评估插件配置(JSON格式) */
    @Column(name = "evaluation_config", columnDefinition = "TEXT")
    private String evaluationConfig;

    /** 任务开始执行时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 任务完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
