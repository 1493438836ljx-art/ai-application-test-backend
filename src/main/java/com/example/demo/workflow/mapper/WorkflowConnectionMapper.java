package com.example.demo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.workflow.entity.WorkflowConnectionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 工作流连线Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface WorkflowConnectionMapper extends BaseMapper<WorkflowConnectionEntity> {

    /**
     * 根据工作流ID查询所有连线
     *
     * @param workflowId 工作流ID
     * @return 连线列表
     */
    List<WorkflowConnectionEntity> selectByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 查询以指定节点为源的所有连线
     *
     * @param sourceNodeId 源节点ID
     * @return 连线列表
     */
    List<WorkflowConnectionEntity> selectBySourceNodeId(@Param("sourceNodeId") Long sourceNodeId);

    /**
     * 查询以指定节点为目标的所有连线
     *
     * @param targetNodeId 目标节点ID
     * @return 连线列表
     */
    List<WorkflowConnectionEntity> selectByTargetNodeId(@Param("targetNodeId") Long targetNodeId);

    /**
     * 删除工作流的所有连线
     *
     * @param workflowId 工作流ID
     * @return 删除数量
     */
    int deleteByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 统计工作流的连线数量
     *
     * @param workflowId 工作流ID
     * @return 连线数量
     */
    Long countByWorkflowId(@Param("workflowId") Long workflowId);
}
