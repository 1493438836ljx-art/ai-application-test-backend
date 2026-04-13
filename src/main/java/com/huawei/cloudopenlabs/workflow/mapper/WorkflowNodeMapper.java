/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.cloudopenlabs.workflow.entity.WorkflowNodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 工作流节点Mapper接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNodeEntity> {

    /**
     * 根据工作流ID查询所有节点
     *
     * @param workflowId 工作流ID
     * @return 节点列表
     */
    List<WorkflowNodeEntity> selectByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 根据工作流ID和节点UUID查询
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 节点
     */
    Optional<WorkflowNodeEntity> selectByWorkflowIdAndNodeUuid(
            @Param("workflowId") String workflowId,
            @Param("nodeUuid") String nodeUuid
    );

    /**
     * 根据工作流ID和节点类型查询
     *
     * @param workflowId 工作流ID
     * @param type       节点类型
     * @return 节点列表
     */
    List<WorkflowNodeEntity> selectByWorkflowIdAndType(
            @Param("workflowId") String workflowId,
            @Param("type") String type
    );

    /**
     * 查询指定节点的子节点
     *
     * @param parentNodeId 父节点ID
     * @return 子节点列表
     */
    List<WorkflowNodeEntity> selectByParentNodeId(@Param("parentNodeId") String parentNodeId);

    /**
     * 删除工作流的所有节点
     *
     * @param workflowId 工作流ID
     * @return 删除数量
     */
    int deleteByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 统计工作流的节点数量
     *
     * @param workflowId 工作流ID
     * @return 节点数量
     */
    Long countByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 根据Skill ID查询所有引用该Skill的节点
     *
     * @param skillId Skill ID
     * @return 节点列表
     */
    List<WorkflowNodeEntity> selectBySkillId(@Param("skillId") String skillId);

    /**
     * 批量更新节点兼容性状态
     *
     * @param nodeIds 节点ID列表
     * @param status  兼容性状态
     * @return 更新数量
     */
    int batchUpdateCompatibilityStatus(@Param("nodeIds") List<String> nodeIds, @Param("status") String status);
}
