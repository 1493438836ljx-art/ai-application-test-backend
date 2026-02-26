package com.example.demo.testset.repository;

import com.example.demo.testset.entity.TestSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 测评集数据访问层
 * <p>
 * 提供测评集实体的数据库操作接口
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface TestSetRepository extends JpaRepository<TestSet, Long> {

    /**
     * 根据名称模糊查询测评集（不区分大小写）
     *
     * @param name     名称关键词
     * @param pageable 分页参数
     * @return 匹配的测评集分页结果
     */
    Page<TestSet> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 检查指定名称的测评集是否存在
     *
     * @param name 测评集名称
     * @return 存在返回true，否则返回false
     */
    boolean existsByName(String name);

    /**
     * 根据ID查询测评集并关联加载测试用例
     * <p>
     * 使用LEFT JOIN FETCH避免N+1查询问题
     * </p>
     *
     * @param id 测评集ID
     * @return 包含测试用例的测评集
     */
    @Query("SELECT ts FROM TestSet ts LEFT JOIN FETCH ts.testCases WHERE ts.id = :id")
    Optional<TestSet> findByIdWithTestCases(@Param("id") Long id);
}
