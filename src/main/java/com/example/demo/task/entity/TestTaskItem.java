package com.example.demo.task.entity;

import com.example.demo.common.enums.TestTaskItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 测试任务执行项实体类
 * 用于存储测试任务中每个测试用例的执行详情和结果
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "test_task_item")
public class TestTaskItem {

    /** 执行项唯一标识ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属测试任务 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TestTask testTask;

    /** 关联的测试用例ID */
    @Column(name = "test_case_id", nullable = false)
    private Long testCaseId;

    /** 执行序号 */
    @Column(nullable = false)
    private Integer sequence;

    /** 执行项状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TestTaskItemStatus status = TestTaskItemStatus.PENDING;

    /** 输入内容 */
    @Column(columnDefinition = "TEXT")
    private String input;

    /** 期望输出 */
    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    /** 实际输出 */
    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    /** 评估得分 */
    @Column(precision = 5, scale = 2)
    private Double score;

    /** 评估原因 */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 执行耗时(毫秒) */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 开始执行时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 完成执行时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
