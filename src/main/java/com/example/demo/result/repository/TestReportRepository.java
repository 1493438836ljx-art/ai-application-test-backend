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

@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {

    Optional<TestReport> findByTaskId(Long taskId);

    Page<TestReport> findByTaskNameContainingIgnoreCase(String taskName, Pageable pageable);

    List<TestReport> findByTestSetId(Long testSetId);

    @Query("SELECT r FROM TestReport r WHERE r.completedAt BETWEEN :startTime AND :endTime")
    List<TestReport> findByCompletedAtBetween(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    boolean existsByTaskId(Long taskId);
}
