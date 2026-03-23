package com.example.demo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.workflow.entity.WorkflowNodeTypeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 节点类型Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface WorkflowNodeTypeMapper extends BaseMapper<WorkflowNodeTypeEntity> {

    /**
     * 根据编码查询
     *
     * @param code 节点类型编码
     * @return 节点类型
     */
    Optional<WorkflowNodeTypeEntity> selectByCode(@Param("code") String code);

    /**
     * 根据分类查询
     *
     * @param category 分类
     * @return 节点类型列表
     */
    List<WorkflowNodeTypeEntity> selectByCategory(@Param("category") String category);

    /**
     * 查询所有启用的节点类型
     *
     * @return 节点类型列表
     */
    List<WorkflowNodeTypeEntity> selectEnabled();

    /**
     * 检查编码是否存在
     *
     * @param code 节点类型编码
     * @return 是否存在
     */
    boolean existsByCode(@Param("code") String code);
}
