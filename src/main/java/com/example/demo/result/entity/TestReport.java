package com.example.demo.result.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 测试报告实体类
 *
 * 用于存储测试任务的执行结果报告，包含测试统计数据、评分信息等
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "test_report")
public class TestReport {

    /** 报告唯一标识ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的测试任务ID */
    @Column(name = "task_id", nullable = false, unique = true)
    private Long taskId;

    /** 测试任务名称 */
    @Column(nullable = false, length = 200)
    private String taskName;

    /** 关联的测评集ID */
    @Column(name = "test_set_id")
    private Long testSetId;

    /** 测评集名称 */
    @Column(name = "test_set_name", length = 200)
    private String testSetName;

    /** 执行环境名称 */
    @Column(name = "environment_name", length = 200)
    private String environmentName;

    /** 执行插件名称 */
    @Column(name = "execution_plugin_name", length = 200)
    private String executionPluginName;

    /** 评估插件名称 */
    @Column(name = "evaluation_plugin_name", length = 200)
    private String evaluationPluginName;

    /** 总测试项数 */
    @Column(name = "total_items")
    private Integer totalItems;

    /** 成功执行的测试项数 */
    @Column(name = "success_items")
    private Integer successItems;

    /** 失败的测试项数 */
    @Column(name = "failed_items")
    private Integer failedItems;

    /** 成功率（百分比） */
    @Column(name = "success_rate", precision = 5, scale = 2)
    private Double successRate;

    /** 平均评分 */
    @Column(name = "average_score", precision = 5, scale = 2)
    private Double averageScore;

    /** 总执行时间（毫秒） */
    @Column(name = "total_execution_time_ms")
    private Long totalExecutionTimeMs;

    /** 测试开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 测试完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 报告摘要 */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 报告详情（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** 记录创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 记录更新时间 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
