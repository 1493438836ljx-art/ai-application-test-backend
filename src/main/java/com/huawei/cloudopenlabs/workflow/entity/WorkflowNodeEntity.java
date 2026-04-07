package com.huawei.cloudopenlabs.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流节点实体类
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Data
@TableName("workflow_node")
public class WorkflowNodeEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属工作流ID
     */
    @TableField("workflow_id")
    private Long workflowId;

    /**
     * 节点UUID
     */
    @TableField("node_uuid")
    private String nodeUuid;

    /**
     * 节点类型编码
     */
    @TableField("type")
    private String type;

    /**
     * 节点类型ID
     */
    @TableField("type_id")
    private Long typeId;

    /**
     * 节点名称
     */
    @TableField("name")
    private String name;

    /**
     * 画布X坐标
     */
    @TableField("position_x")
    private Integer positionX;

    /**
     * 画布Y坐标
     */
    @TableField("position_y")
    private Integer positionY;

    /**
     * 输入端口定义
     */
    @TableField("input_ports")
    private String inputPorts;

    /**
     * 输出端口定义
     */
    @TableField("output_ports")
    private String outputPorts;

    /**
     * 输入参数定义
     */
    @TableField("input_params")
    private String inputParams;

    /**
     * 输出参数定义
     */
    @TableField("output_params")
    private String outputParams;

    /**
     * 节点配置参数
     */
    @TableField("config")
    private String config;

    /**
     * 父节点ID
     */
    @TableField("parent_node_id")
    private Long parentNodeId;

    // ========== Skill引用 ==========

    /**
     * 引用的Skill ID
     */
    @TableField("skill_id")
    private String skillId;

    /**
     * Skill快照（JSON格式）
     */
    @TableField("skill_snapshot")
    private String skillSnapshot;

    // ========== 执行配置 ==========

    /**
     * 执行位置：CLIENT/SERVICE
     */
    @TableField("execution_location")
    private String executionLocation;

    /**
     * 错误策略：STOP/SKIP/RETRY/ERROR_BRANCH
     */
    @TableField("error_strategy")
    private String errorStrategy;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 重试间隔（毫秒）
     */
    @TableField("retry_interval")
    private Integer retryInterval;

    /**
     * 错误处理分支节点ID
     */
    @TableField("error_branch_id")
    private Long errorBranchId;

    // ========== 条件节点配置 ==========

    /**
     * 条件类型：SIMPLE/MULTI
     */
    @TableField("condition_type")
    private String conditionType;

    /**
     * 条件配置（JSON格式）
     */
    @TableField("conditions")
    private String conditions;

    // ========== 循环节点配置 ==========

    /**
     * 循环类型：COUNT/ARRAY/CONDITION
     */
    @TableField("loop_type")
    private String loopType;

    /**
     * 循环配置（JSON格式）
     */
    @TableField("loop_config")
    private String loopConfig;

    // ========== 批处理/异步/收集配置 ==========

    /**
     * 批处理配置（JSON格式）
     */
    @TableField("batch_config")
    private String batchConfig;

    /**
     * 异步处理配置（JSON格式）
     */
    @TableField("async_config")
    private String asyncConfig;

    /**
     * 结果收集配置（JSON格式）
     */
    @TableField("collect_config")
    private String collectConfig;

    // ========== 兼容性状态 ==========

    /**
     * 兼容性状态：COMPATIBLE/NEEDS_UPDATE/INCOMPATIBLE/INVALID
     */
    @TableField("compatibility_status")
    private String compatibilityStatus;

    /**
     * 节点分类：BASIC/LOGIC/EXECUTION
     */
    @TableField("node_category")
    private String nodeCategory;

    // ========== 时间字段 ==========

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updatedAt;
}
