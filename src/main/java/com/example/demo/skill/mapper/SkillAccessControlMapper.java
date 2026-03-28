package com.example.demo.skill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.skill.entity.SkillAccessControlEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Skill访问控制Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface SkillAccessControlMapper extends BaseMapper<SkillAccessControlEntity> {

    /**
     * 根据Skill ID查询访问控制列表
     *
     * @param skillId Skill ID
     * @return 访问控制列表
     */
    List<SkillAccessControlEntity> selectBySkillId(@Param("skillId") String skillId);

    /**
     * 根据Skill ID删除访问控制
     *
     * @param skillId Skill ID
     * @return 删除数量
     */
    int deleteBySkillId(@Param("skillId") String skillId);
}
