package com.example.demo.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.workflow.entity.WorkflowExecutionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 工作流执行记录Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecutionEntity> {

    /**
     * 根据执行UUID查询
     *
     * @param executionUuid 执行UUID
     * @return 执行记录
     */
    Optional<WorkflowExecutionEntity> selectByExecutionUuid(@Param("executionUuid") String executionUuid);

    /**
     * 根据工作流ID查询执行记录列表
     *
     * @param page       分页参数
     * @param workflowId 工作流ID
     * @return 执行记录分页列表
     */
    IPage<WorkflowExecutionEntity> selectByWorkflowId(Page<WorkflowExecutionEntity> page, @Param("workflowId") Long workflowId);

    /**
     * 根据状态查询
     *
     * @param page   分页参数
     * @param status 状态
     * @return 执行记录分页列表
     */
    IPage<WorkflowExecutionEntity> selectByStatus(Page<WorkflowExecutionEntity> page, @Param("status") String status);

    /**
     * 查询正在执行的记录
     *
     * @return 正在执行的记录列表
     */
    List<WorkflowExecutionEntity> selectRunningExecutions();

    /**
     * 获取工作流的最新执行记录
     *
     * @param workflowId 工作流ID
     * @return 最新执行记录
     */
    Optional<WorkflowExecutionEntity> selectLatestByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * 统计工作流的执行次数
     *
     * @param workflowId 工作流ID
     * @return 执行次数
     */
    Long countByWorkflowId(@Param("workflowId") Long workflowId);
}
