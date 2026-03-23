package com.example.demo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.workflow.entity.VariableTypeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 变量类型Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface VariableTypeMapper extends BaseMapper<VariableTypeEntity> {

    /**
     * 根据编码查询
     *
     * @param code 变量类型编码
     * @return 变量类型
     */
    VariableTypeEntity selectByCode(@Param("code") String code);

    /**
     * 查询所有启用的变量类型
     *
     * @return 变量类型列表
     */
    List<VariableTypeEntity> selectEnabled();

    /**
     * 根据分类查询启用的变量类型
     *
     * @param category 分类
     * @return 变量类型列表
     */
    List<VariableTypeEntity> selectEnabledByCategory(@Param("category") String category);

    /**
     * 查询所有分类
     *
     * @return 分类列表
     */
    List<String> selectAllCategories();

    /**
     * 检查编码是否存在
     *
     * @param code 变量类型编码
     * @return 是否存在
     */
    boolean existsByCode(@Param("code") String code);
}
