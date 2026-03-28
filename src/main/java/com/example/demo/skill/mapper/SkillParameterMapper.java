package com.example.demo.skill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.skill.entity.SkillParameterEntity;
import com.example.demo.skill.entity.SkillParamDirection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Skill参数Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface SkillParameterMapper extends BaseMapper<SkillParameterEntity> {

    /**
     * 根据Skill ID和参数方向查询参数列表（按顺序排序）
     *
     * @param skillId      Skill ID
     * @param direction    参数方向
     * @return 参数列表
     */
    List<SkillParameterEntity> selectBySkillIdAndDirection(
            @Param("skillId") String skillId,
            @Param("direction") SkillParamDirection direction
    );

    /**
     * 根据Skill ID查询所有参数列表
     *
     * @param skillId Skill ID
     * @return 参数列表
     */
    List<SkillParameterEntity> selectBySkillId(@Param("skillId") String skillId);

    /**
     * 根据Skill ID删除所有参数
     *
     * @param skillId Skill ID
     * @return 删除数量
     */
    int deleteBySkillId(@Param("skillId") String skillId);

    /**
     * 根据Skill ID和参数方向删除参数
     *
     * @param skillId   Skill ID
     * @param direction 参数方向
     * @return 删除数量
     */
    int deleteBySkillIdAndDirection(
            @Param("skillId") String skillId,
            @Param("direction") SkillParamDirection direction
    );

    /**
     * 根据Skill ID和参数方向统计参数数量
     *
     * @param skillId   Skill ID
     * @param direction 参数方向
     * @return 参数数量
     */
    int countBySkillIdAndDirection(
            @Param("skillId") String skillId,
            @Param("direction") SkillParamDirection direction
    );
}
