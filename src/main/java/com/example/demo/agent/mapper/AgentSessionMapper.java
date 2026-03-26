package com.example.demo.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.agent.entity.AgentSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * Agent会话Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSessionEntity> {

    /**
     * 根据会话ID查询
     *
     * @param conversationId 会话ID
     * @return Agent会话
     */
    Optional<AgentSessionEntity> selectByConversationId(@Param("conversationId") String conversationId);

    /**
     * 根据工作流ID查询活跃会话
     *
     * @param workflowId 工作流ID
     * @return Agent会话
     */
    Optional<AgentSessionEntity> selectActiveByWorkflowId(@Param("workflowId") Long workflowId);
}
