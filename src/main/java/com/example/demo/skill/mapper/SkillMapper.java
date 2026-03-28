package com.example.demo.skill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.skill.entity.SkillEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Skill Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface SkillMapper extends BaseMapper<SkillEntity> {

    /**
     * 根据名称模糊查询
     *
     * @param page 分页参数
     * @param name 名称关键字
     * @return Skill分页列表
     */
    IPage<SkillEntity> selectByNameLike(Page<SkillEntity> page, @Param("name") String name);

    /**
     * 根据分类查询
     *
     * @param page     分页参数
     * @param category 分类
     * @return Skill分页列表
     */
    IPage<SkillEntity> selectByCategory(Page<SkillEntity> page, @Param("category") String category);

    /**
     * 根据状态查询
     *
     * @param page   分页参数
     * @param status 状态
     * @return Skill分页列表
     */
    IPage<SkillEntity> selectByStatus(Page<SkillEntity> page, @Param("status") String status);

    /**
     * 根据创建人查询
     *
     * @param page      分页参数
     * @param createdBy 创建人
     * @return Skill分页列表
     */
    IPage<SkillEntity> selectByCreatedBy(Page<SkillEntity> page, @Param("createdBy") String createdBy);

    /**
     * 根据执行方式查询
     *
     * @param page          分页参数
     * @param executionType 执行方式
     * @return Skill分页列表
     */
    IPage<SkillEntity> selectByExecutionType(Page<SkillEntity> page, @Param("executionType") String executionType);

    /**
     * 检查名称是否存在
     *
     * @param name 名称
     * @return 数量
     */
    Long countByName(@Param("name") String name);

    /**
     * 查询所有未删除的Skill（用于清理孤儿文件）
     *
     * @return 未删除的Skill列表
     */
    List<SkillEntity> selectAllNonDeleted();

    /**
     * 查询已删除超过指定天数的Skill（用于老化清理）
     *
     * @param days 天数
     * @return 符合条件的Skill列表
     */
    List<SkillEntity> selectDeletedOlderThan(@Param("days") int days);

    /**
     * 物理删除Skill（绕过逻辑删除）
     *
     * @param id Skill ID
     * @return 删除的行数
     */
    int physicalDeleteById(@Param("id") String id);
}
