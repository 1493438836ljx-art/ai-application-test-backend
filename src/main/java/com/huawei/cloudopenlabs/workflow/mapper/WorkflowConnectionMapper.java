/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowConnectionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流连线Mapper接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Mapper
public interface WorkflowConnectionMapper extends BaseMapper<WorkflowConnectionEntity> {

    /**
     * 根据工作流ID查询所有连线
     *
     * @param workflowId 工作流ID
     * @return 连线列表
     */
    List<WorkflowConnectionEntity> selectByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 查询以指定节点为源的所有连线
     *
     * @param sourceNodeId 源节点ID
     * @return 连线列表
     */
    List<WorkflowConnectionEntity> selectBySourceNodeId(@Param("sourceNodeId") String sourceNodeId);

    /**
     * 查询以指定节点为目标的所有连线
     *
     * @param targetNodeId 目标节点ID
     * @return 连线列表
     */
    List<WorkflowConnectionEntity> selectByTargetNodeId(@Param("targetNodeId") String targetNodeId);

    /**
     * 删除工作流的所有连线
     *
     * @param workflowId 工作流ID
     * @return 删除数量
     */
    int deleteByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 删除以指定节点为源的所有连线
     *
     * @param sourceNodeId 源节点ID
     * @return 删除数量
     */
    int deleteBySourceNodeId(@Param("sourceNodeId") String sourceNodeId);

    /**
     * 删除以指定节点为目标的所有连线
     *
     * @param targetNodeId 目标节点ID
     * @return 删除数量
     */
    int deleteByTargetNodeId(@Param("targetNodeId") String targetNodeId);

    /**
     * 统计工作流的连线数量
     *
     * @param workflowId 工作流ID
     * @return 连线数量
     */
    Long countByWorkflowId(@Param("workflowId") String workflowId);
}
