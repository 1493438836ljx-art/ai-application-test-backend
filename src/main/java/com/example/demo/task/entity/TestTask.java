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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "test_task")
public class TestTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "test_set_id", nullable = false)
    private Long testSetId;

    @Column(name = "environment_id", nullable = false)
    private Long environmentId;

    @Column(name = "execution_plugin_id", nullable = false)
    private Long executionPluginId;

    @Column(name = "evaluation_plugin_id", nullable = false)
    private Long evaluationPluginId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TestTaskStatus status = TestTaskStatus.PENDING;

    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "completed_items")
    @Builder.Default
    private Integer completedItems = 0;

    @Column(name = "success_items")
    @Builder.Default
    private Integer successItems = 0;

    @Column(name = "failed_items")
    @Builder.Default
    private Integer failedItems = 0;

    @Column(name = "execution_config", columnDefinition = "TEXT")
    private String executionConfig;

    @Column(name = "evaluation_config", columnDefinition = "TEXT")
    private String evaluationConfig;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
