package com.example.demo.workflow.service;

import com.example.demo.workflow.dto.ExecutionResponse;
import com.example.demo.workflow.entity.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 工作流执行服务接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface WorkflowExecutionService {

    /**
     * 执行工作流
     *
     * @param workflowId 工作流ID
     * @param triggeredBy 触发人
     * @param inputData   输入数据
     * @return 执行记录ID
     */
    Long executeWorkflow(Long workflowId, String triggeredBy, String inputData);

    /**
     * 获取执行记录
     *
     * @param id 执行记录ID
     * @return 执行响应
     */
    ExecutionResponse getExecution(Long id);

    /**
     * 根据UUID获取执行记录
     *
     * @param uuid 执行UUID
     * @return 执行响应
     */
    ExecutionResponse getExecutionByUuid(String uuid);

    /**
     * 获取工作流的执行记录列表
     *
     * @param workflowId 工作流ID
     * @param pageable   分页参数
     * @return 执行记录分页列表
     */
    Page<ExecutionResponse> getExecutionsByWorkflowId(Long workflowId, Pageable pageable);

    /**
     * 获取正在执行的记录
     *
     * @return 执行记录列表
     */
    List<ExecutionResponse> getRunningExecutions();

    /**
     * 中止执行
     *
     * @param id 执行记录ID
     */
    void abortExecution(Long id);

    /**
     * 更新执行进度
     *
     * @param id       执行记录ID
     * @param progress 进度（0-100）
     */
    void updateProgress(Long id, int progress);

    /**
     * 完成执行
     *
     * @param id          执行记录ID
     * @param outputData  输出数据
     * @param nodeExecutions 节点执行详情
     */
    void completeExecution(Long id, String outputData, String nodeExecutions);

    /**
     * 执行失败
     *
     * @param id           执行记录ID
     * @param errorMessage 错误信息
     */
    void failExecution(Long id, String errorMessage);
}
