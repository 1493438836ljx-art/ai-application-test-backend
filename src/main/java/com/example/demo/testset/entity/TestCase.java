package com.example.demo.testset.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 测试用例实体类
 * <p>
 * 测试用例是测评的基本单元，包含输入数据、预期输出等信息。
 * 每个测试用例属于一个测评集（TestSet）。
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
@Table(name = "test_case")
public class TestCase {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属测评集 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_set_id", nullable = false)
    private TestSet testSet;

    /** 用例序号，用于确定执行顺序 */
    @Column(nullable = false)
    private Integer sequence;

    /** 输入数据，发送给AI应用的提示词或问题 */
    @Column(columnDefinition = "TEXT")
    private String input;

    /** 预期输出，用于评估AI应用的回答质量 */
    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    /** 标签，用于分类和筛选，多个标签用逗号分隔 */
    @Column(length = 500)
    private String tags;

    /** 扩展数据，JSON格式存储额外信息 */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
