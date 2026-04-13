/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowAssociationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 工作流关联Mapper接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Mapper
public interface WorkflowAssociationMapper extends BaseMapper<WorkflowAssociationEntity> {

    /**
     * 根据工作流ID查询所有关联
     *
     * @param workflowId 工作流ID
     * @return 关联列表
     */
    List<WorkflowAssociationEntity> selectByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 根据循环节点ID查询关联
     *
     * @param loopNodeId 循环节点ID
     * @return 关联
     */
    Optional<WorkflowAssociationEntity> selectByLoopNodeId(@Param("loopNodeId") String loopNodeId);

    /**
     * 删除工作流的所有关联
     *
     * @param workflowId 工作流ID
     * @return 删除数量
     */
    int deleteByWorkflowId(@Param("workflowId") String workflowId);
}
