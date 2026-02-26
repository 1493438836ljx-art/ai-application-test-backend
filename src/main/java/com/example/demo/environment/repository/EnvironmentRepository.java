package com.example.demo.environment.repository;

import com.example.demo.environment.entity.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 环境数据访问层接口
 * <p>
 * 提供环境实体的数据库操作方法，继承自JpaRepository
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    /**
     * 根据名称模糊查询环境列表（分页）
     *
     * @param name 环境名称关键字
     * @param pageable 分页参数
     * @return 匹配的环境分页列表
     */
    Page<Environment> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 查询所有激活状态的环境
     *
     * @return 激活状态的环境列表
     */
    List<Environment> findByIsActiveTrue();

    /**
     * 根据名称精确查询环境
     *
     * @param name 环境名称
     * @return 环境实体（可选）
     */
    Optional<Environment> findByName(String name);

    /**
     * 检查指定名称的环境是否存在
     *
     * @param name 环境名称
     * @return 存在返回true，否则返回false
     */
    boolean existsByName(String name);
}
