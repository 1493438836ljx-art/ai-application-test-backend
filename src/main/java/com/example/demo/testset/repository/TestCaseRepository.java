package com.example.demo.testset.repository;

import com.example.demo.testset.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByTestSetIdOrderBySequence(Long testSetId);

    Page<TestCase> findByTestSetId(Long testSetId, Pageable pageable);

    @Query("SELECT MAX(tc.sequence) FROM TestCase tc WHERE tc.testSet.id = :testSetId")
    Integer findMaxSequenceByTestSetId(@Param("testSetId") Long testSetId);

    long countByTestSetId(Long testSetId);

    void deleteByTestSetId(Long testSetId);
}
