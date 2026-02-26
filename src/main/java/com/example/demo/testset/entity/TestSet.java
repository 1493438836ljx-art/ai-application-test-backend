package com.example.demo.testset.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 测评集实体类
 * <p>
 * 测评集是测试用例的集合，用于组织和管理一组相关的测试用例。
 * 一个测评集可以包含多个测试用例（TestCase）。
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
@Table(name = "test_set")
public class TestSet {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 测评集名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 测评集描述 */
    @Column(length = 1000)
    private String description;

    /** 测试用例总数 */
    @Column(name = "total_cases")
    private Integer totalCases;

    /**
     * 包含的测试用例列表
     * 使用级联操作，删除测评集时同时删除关联的测试用例
     */
    @OneToMany(mappedBy = "testSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
