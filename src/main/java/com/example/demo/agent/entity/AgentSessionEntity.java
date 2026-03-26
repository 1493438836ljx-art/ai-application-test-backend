package com.example.demo.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent会话实体类
 * 用于多轮对话的会话管理，存储查询结果和操作结果
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@TableName("agent_session")
public class AgentSessionEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID（对应 chat_conversation.conversation_uuid）
     * 即 Claude CLI session ID
     */
    @TableField("conversation_id")
    private String conversationId;

    /**
     * 关联的工作流ID
     */
    @TableField("workflow_id")
    private Long workflowId;

    /**
     * 会话状态
     * ACTIVE - 进行中
     * COMPLETED - 已完成
     * ERROR - 出错
     */
    @TableField("status")
    private String status;

    /**
     * JSON格式存储的查询结果
     */
    @TableField("query_results")
    private String queryResults;

    /**
     * JSON格式存储的操作结果
     */
    @TableField("action_results")
    private String actionResults;

    /**
     * 最后一次AI推理内容
     */
    @TableField("last_reasoning")
    private String lastReasoning;

    /**
     * 当前轮次计数
     */
    @TableField("round_count")
    private Integer roundCount;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
