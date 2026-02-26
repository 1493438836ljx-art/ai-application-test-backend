package com.example.demo.prompt.repository;

import com.example.demo.prompt.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Prompt数据访问层
 * <p>
 * 提供Prompt实体的数据库操作接口
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {

    /**
     * 根据名称模糊查询Prompt（不区分大小写）
     *
     * @param name     名称关键词
     * @param pageable 分页参数
     * @return 匹配的Prompt分页结果
     */
    Page<Prompt> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 根据名称精确查询Prompt
     *
     * @param name Prompt名称
     * @return Prompt对象
     */
    Optional<Prompt> findByName(String name);

    /**
     * 检查指定名称的Prompt是否存在
     *
     * @param name Prompt名称
     * @return 存在返回true，否则返回false
     */
    boolean existsByName(String name);
}
