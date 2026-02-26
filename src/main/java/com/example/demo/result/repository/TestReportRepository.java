package com.example.demo.result.repository;

import com.example.demo.result.entity.TestReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 测试报告数据访问层接口
 *
 * 提供测试报告实体的数据库操作，包括基本的CRUD操作和自定义查询方法
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {

    /**
     * 根据任务ID查询测试报告
     *
     * @param taskId 任务ID
     * @return 测试报告（可选）
     */
    Optional<TestReport> findByTaskId(Long taskId);

    /**
     * 根据任务名称模糊查询测试报告（分页）
     *
     * @param taskName 任务名称
     * @param pageable 分页参数
     * @return 测试报告分页结果
     */
    Page<TestReport> findByTaskNameContainingIgnoreCase(String taskName, Pageable pageable);

    /**
     * 根据测评集ID查询所有测试报告
     *
     * @param testSetId 测评集ID
     * @return 测试报告列表
     */
    List<TestReport> findByTestSetId(Long testSetId);

    /**
     * 根据完成时间范围查询测试报告
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 测试报告列表
     */
    @Query("SELECT r FROM TestReport r WHERE r.completedAt BETWEEN :startTime AND :endTime")
    List<TestReport> findByCompletedAtBetween(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 检查指定任务是否已存在报告
     *
     * @param taskId 任务ID
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByTaskId(Long taskId);
}
