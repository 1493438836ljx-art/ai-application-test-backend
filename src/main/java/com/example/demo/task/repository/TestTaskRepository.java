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

@Repository
public interface TestTaskRepository extends JpaRepository<TestTask, Long> {

    Page<TestTask> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<TestTask> findByStatus(TestTaskStatus status, Pageable pageable);

    List<TestTask> findByStatusIn(List<TestTaskStatus> statuses);

    @Query("SELECT tt FROM TestTask tt WHERE tt.testSetId = :testSetId")
    List<TestTask> findByTestSetId(@Param("testSetId") Long testSetId);

    @Query("SELECT tt FROM TestTask tt WHERE tt.environmentId = :environmentId")
    List<TestTask> findByEnvironmentId(@Param("environmentId") Long environmentId);
}
