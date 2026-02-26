package com.example.demo.testset.repository;

import com.example.demo.testset.entity.TestSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestSetRepository extends JpaRepository<TestSet, Long> {

    Page<TestSet> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByName(String name);

    @Query("SELECT ts FROM TestSet ts LEFT JOIN FETCH ts.testCases WHERE ts.id = :id")
    Optional<TestSet> findByIdWithTestCases(@Param("id") Long id);
}
