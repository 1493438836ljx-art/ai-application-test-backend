package com.example.demo.task.repository;

import com.example.demo.common.enums.TestTaskItemStatus;
import com.example.demo.task.entity.TestTaskItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测试任务执行项数据访问层接口
 * 提供测试任务执行项实体的数据库操作方法
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface TestTaskItemRepository extends JpaRepository<TestTaskItem, Long> {

    /**
     * 根据任务ID查询所有执行项并按序号排序
     *
     * @param taskId 任务ID
     * @return 按序号排序的执行项列表
     */
    List<TestTaskItem> findByTestTaskIdOrderBySequence(Long taskId);

    /**
     * 根据任务ID分页查询执行项
     *
     * @param taskId 任务ID
     * @param pageable 分页参数
     * @return 执行项分页结果
     */
    Page<TestTaskItem> findByTestTaskId(Long taskId, Pageable pageable);

    /**
     * 根据任务ID和状态查询执行项
     *
     * @param taskId 任务ID
     * @param status 执行项状态
     * @return 匹配的执行项列表
     */
    List<TestTaskItem> findByTestTaskIdAndStatus(Long taskId, TestTaskItemStatus status);

    /**
     * 统计指定任务下指定状态的执行项数量
     *
     * @param taskId 任务ID
     * @param status 执行项状态
     * @return 执行项数量
     */
    @Query("SELECT COUNT(tti) FROM TestTaskItem tti WHERE tti.testTask.id = :taskId AND tti.status = :status")
    long countByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") TestTaskItemStatus status);

    /**
     * 统计指定任务下的执行项总数
     *
     * @param taskId 任务ID
     * @return 执行项总数
     */
    @Query("SELECT COUNT(tti) FROM TestTaskItem tti WHERE tti.testTask.id = :taskId")
    long countByTaskId(@Param("taskId") Long taskId);

    /**
     * 删除指定任务下的所有执行项
     *
     * @param taskId 任务ID
     */
    void deleteByTestTaskId(Long taskId);
}
