package com.example.demo.task.repository;

import com.example.demo.common.enums.TestTaskStatus;
import com.example.demo.task.entity.TestTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测试任务数据访问层接口
 * 提供测试任务实体的数据库操作方法
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface TestTaskRepository extends JpaRepository<TestTask, Long> {

    /**
     * 根据任务名称模糊查询（分页）
     *
     * @param name 任务名称关键词
     * @param pageable 分页参数
     * @return 匹配的测试任务分页结果
     */
    Page<TestTask> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 根据任务名称模糊查询和状态查询（分页）
     *
     * @param name 任务名称关键词
     * @param status 任务状态
     * @param pageable 分页参数
     * @return 匹配的测试任务分页结果
     */
    Page<TestTask> findByNameContainingIgnoreCaseAndStatus(String name, TestTaskStatus status, Pageable pageable);

    /**
     * 根据任务状态查询（分页）
     *
     * @param status 任务状态
     * @param pageable 分页参数
     * @return 匹配的测试任务分页结果
     */
    Page<TestTask> findByStatus(TestTaskStatus status, Pageable pageable);

    /**
     * 根据多个任务状态查询
     *
     * @param statuses 任务状态列表
     * @return 匹配的测试任务列表
     */
    List<TestTask> findByStatusIn(List<TestTaskStatus> statuses);

    /**
     * 根据测评集ID查询关联的所有任务
     *
     * @param testSetId 测评集ID
     * @return 关联的测试任务列表
     */
    @Query("SELECT tt FROM TestTask tt WHERE tt.testSetId = :testSetId")
    List<TestTask> findByTestSetId(@Param("testSetId") Long testSetId);

    /**
     * 根据环境ID查询关联的所有任务
     *
     * @param environmentId 环境ID
     * @return 关联的测试任务列表
     */
    @Query("SELECT tt FROM TestTask tt WHERE tt.environmentId = :environmentId")
    List<TestTask> findByEnvironmentId(@Param("environmentId") Long environmentId);
}
