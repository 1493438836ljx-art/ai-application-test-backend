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

@Repository
public interface TestTaskItemRepository extends JpaRepository<TestTaskItem, Long> {

    List<TestTaskItem> findByTestTaskIdOrderBySequence(Long taskId);

    Page<TestTaskItem> findByTestTaskId(Long taskId, Pageable pageable);

    List<TestTaskItem> findByTestTaskIdAndStatus(Long taskId, TestTaskItemStatus status);

    @Query("SELECT COUNT(tti) FROM TestTaskItem tti WHERE tti.testTask.id = :taskId AND tti.status = :status")
    long countByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") TestTaskItemStatus status);

    @Query("SELECT COUNT(tti) FROM TestTaskItem tti WHERE tti.testTask.id = :taskId")
    long countByTaskId(@Param("taskId") Long taskId);

    void deleteByTestTaskId(Long taskId);
}
