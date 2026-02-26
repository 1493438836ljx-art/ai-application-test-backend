package com.example.demo.testset.repository;

import com.example.demo.testset.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测试用例数据访问层
 * <p>
 * 提供测试用例实体的数据库操作接口
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    /**
     * 根据测评集ID查询所有测试用例（按序号排序）
     *
     * @param testSetId 测评集ID
     * @return 排序后的测试用例列表
     */
    List<TestCase> findByTestSetIdOrderBySequence(Long testSetId);

    /**
     * 根据测评集ID分页查询测试用例
     *
     * @param testSetId 测评集ID
     * @param pageable  分页参数
     * @return 测试用例分页结果
     */
    Page<TestCase> findByTestSetId(Long testSetId, Pageable pageable);

    /**
     * 查询指定测评集中测试用例的最大序号
     *
     * @param testSetId 测评集ID
     * @return 最大序号，无测试用例时返回null
     */
    @Query("SELECT MAX(tc.sequence) FROM TestCase tc WHERE tc.testSet.id = :testSetId")
    Integer findMaxSequenceByTestSetId(@Param("testSetId") Long testSetId);

    /**
     * 统计指定测评集中的测试用例数量
     *
     * @param testSetId 测评集ID
     * @return 测试用例数量
     */
    long countByTestSetId(Long testSetId);

    /**
     * 删除指定测评集中的所有测试用例
     *
     * @param testSetId 测评集ID
     */
    void deleteByTestSetId(Long testSetId);
}
