package com.example.demo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.workflow.entity.WorkflowNodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 工作流节点Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNodeEntity> {

    /**
     * 根据工作流ID查询所有节点
     *
     * @param workflowId 工作流ID
     * @return 节点列表
     */
    List<WorkflowNodeEntity> selectByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 根据工作流ID和节点UUID查询
     *
     * @param workflowId 工作流ID
     * @param nodeUuid   节点UUID
     * @return 节点
     */
    Optional<WorkflowNodeEntity> selectByWorkflowIdAndNodeUuid(
            @Param("workflowId") Long workflowId,
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
            @Param("workflowId") Long workflowId,
            @Param("type") String type
    );

    /**
     * 查询指定节点的子节点
     *
     * @param parentNodeId 父节点ID
     * @return 子节点列表
     */
    List<WorkflowNodeEntity> selectByParentNodeId(@Param("parentNodeId") Long parentNodeId);

    /**
     * 删除工作流的所有节点
     *
     * @param workflowId 工作流ID
     * @return 删除数量
     */
    int deleteByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 统计工作流的节点数量
     *
     * @param workflowId 工作流ID
     * @return 节点数量
     */
    Long countByWorkflowId(@Param("workflowId") Long workflowId);
}
