package com.example.demo.plugin.repository;

import com.example.demo.common.enums.PluginType;
import com.example.demo.plugin.entity.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 插件数据访问层接口，提供插件实体的数据库操作
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Repository
public interface PluginRepository extends JpaRepository<Plugin, Long>, JpaSpecificationExecutor<Plugin> {

    /**
     * 根据插件类型查询插件列表
     *
     * @param type 插件类型
     * @return 该类型的所有插件列表
     */
    List<Plugin> findByType(PluginType type);

    /**
     * 查询所有激活状态的插件
     *
     * @return 激活状态的插件列表
     */
    List<Plugin> findByIsActiveTrue();

    /**
     * 根据插件类型查询激活状态的插件列表
     *
     * @param type 插件类型
     * @return 该类型且激活状态的插件列表
     */
    List<Plugin> findByTypeAndIsActiveTrue(PluginType type);

    /**
     * 根据插件名称查询插件
     *
     * @param name 插件名称
     * @return 插件对象（可能为空）
     */
    Optional<Plugin> findByName(String name);

    /**
     * 检查指定名称的插件是否存在
     *
     * @param name 插件名称
     * @return 存在返回true，否则返回false
     */
    boolean existsByName(String name);
}
