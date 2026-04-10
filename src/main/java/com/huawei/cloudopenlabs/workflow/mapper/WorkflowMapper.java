/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流Mapper接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowEntity> {

    /**
     * 根据名称模糊查询
     *
     * @param page   分页参数
     * @param name   名称关键字
     * @return 工作流分页列表
     */
    IPage<WorkflowEntity> selectByNameLike(Page<WorkflowEntity> page, @Param("name") String name);

    /**
     * 根据状态查询
     *
     * @param page   分页参数
     * @param status 状态
     * @return 工作流分页列表
     */
    IPage<WorkflowEntity> selectByStatus(Page<WorkflowEntity> page, @Param("status") String status);

    /**
     * 根据发布状态查询
     *
     * @param published 是否发布
     * @return 工作流列表
     */
    List<WorkflowEntity> selectByPublished(@Param("published") Boolean published);

    /**
     * 根据创建人查询
     *
     * @param page     分页参数
     * @param createdBy 创建人
     * @return 工作流分页列表
     */
    IPage<WorkflowEntity> selectByCreatedBy(Page<WorkflowEntity> page, @Param("createdBy") String createdBy);

    /**
     * 统计指定状态的工作流数量
     *
     * @param status 状态
     * @return 数量
     */
    Long countByStatus(@Param("status") String status);
}
