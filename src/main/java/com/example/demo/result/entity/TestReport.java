package com.example.demo.result.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "test_report")
public class TestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, unique = true)
    private Long taskId;

    @Column(nullable = false, length = 200)
    private String taskName;

    @Column(name = "test_set_id")
    private Long testSetId;

    @Column(name = "test_set_name", length = 200)
    private String testSetName;

    @Column(name = "environment_name", length = 200)
    private String environmentName;

    @Column(name = "execution_plugin_name", length = 200)
    private String executionPluginName;

    @Column(name = "evaluation_plugin_name", length = 200)
    private String evaluationPluginName;

    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "success_items")
    private Integer successItems;

    @Column(name = "failed_items")
    private Integer failedItems;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private Double successRate;

    @Column(name = "average_score", precision = 5, scale = 2)
    private Double averageScore;

    @Column(name = "total_execution_time_ms")
    private Long totalExecutionTimeMs;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
