# 工作流数据库表结构设计

> 本文档基于 `Workflow.md` 中的工作流编排功能分析，设计相应的数据库表结构。

## 一、概述

工作流编排系统是一个可视化的AI应用测试编排框架，本设计涵盖以下核心数据实体：

| 实体 | 说明 | 对应表 |
|------|------|--------|
| 工作流 | 工作流的基本信息和元数据 | `workflow` |
| 节点 | 工作流中的处理单元 | `workflow_node` |
| 连线 | 节点间的数据流向 | `workflow_connection` |
| 关联线 | 循环节点与循环体的关联 | `workflow_association` |
| 执行记录 | 工作流运行历史 | `workflow_execution` |
| 节点类型 | 节点类型定义 | `workflow_node_type` |

---

## 二、ER图

```
┌─────────────────┐       ┌─────────────────────┐
│  workflow       │       │  workflow_node_type │
├─────────────────┤       ├─────────────────────┤
│ id (PK)         │       │ id (PK)             │
│ name            │       │ code (UK)           │
│ description     │       │ name                │
│ published       │       │ category            │
│ ...             │       │ description         │
└────────┬────────┘       └─────────┬───────────┘
         │                          │
         │ 1:N                      │ 1:N
         ▼                          ▼
┌─────────────────────┐    ┌─────────────────────┐
│  workflow_node      │    │  workflow_node      │
├─────────────────────┤    ├─────────────────────┤
│ id (PK)             │◄───┤ type_id (FK)        │
│ workflow_id (FK)    │    │ ...                 │
│ type                │    └─────────────────────┘
│ name                │
│ position_x          │
│ position_y          │
│ config (JSON)       │
│ ...                 │
└─────────┬───────────┘
          │
          │ 1:N (source)
          │
          ▼
┌─────────────────────┐        ┌─────────────────────┐
│workflow_connection  │        │workflow_association │
├─────────────────────┤        ├─────────────────────┤
│ id (PK)             │        │ id (PK)             │
│ workflow_id (FK)    │        │ workflow_id (FK)    │
│ source_node_id (FK) │        │ loop_node_id (FK)   │
│ target_node_id (FK) │        │ body_node_id (FK)   │
│ source_port_id      │        │ ...                 │
│ target_port_id      │        └─────────────────────┘
│ ...                 │
└─────────────────────┘

┌─────────────────┐
│ workflow        │
└────────┬────────┘
         │ 1:N
         ▼
┌─────────────────────┐
│ workflow_execution  │
├─────────────────────┤
│ id (PK)             │
│ workflow_id (FK)    │
│ status              │
│ result (JSON)       │
│ ...                 │
└─────────────────────┘
```

---

## 三、表结构详细设计

### 3.1 workflow（工作流主表）

存储工作流的基本信息和配置。

| 字段名 | 数据类型 | 可空 | 默认值 | 说明 |
|--------|----------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键ID |
| `name` | VARCHAR(100) | NO | - | 工作流名称 |
| `description` | VARCHAR(500) | YES | NULL | 工作流描述 |
| `published` | TINYINT(1) | NO | 0 | 是否已发布（0:否, 1:是） |
| `has_run` | TINYINT(1) | NO | 0 | 是否已运行（0:否, 1:是） |
| `version` | INT | NO | 1 | 版本号 |
| `status` | VARCHAR(20) | NO | 'DRAFT' | 状态（DRAFT/PUBLISHED/ARCHIVED） |
| `created_by` | VARCHAR(64) | YES | NULL | 创建人 |
| `created_at` | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| `updated_by` | VARCHAR(64) | YES | NULL | 更新人 |
| `updated_at` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | TINYINT(1) | NO | 0 | 逻辑删除标记（0:正常, 1:已删除） |

**建表SQL：**

```sql
CREATE TABLE `workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '工作流名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '工作流描述',
  `published` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已发布',
  `has_run` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已运行',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_workflow_name` (`name`),
  INDEX `idx_workflow_status` (`status`),
  INDEX `idx_workflow_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流主表';
```

---

### 3.2 workflow_node_type（节点类型表）

定义工作流中可用的节点类型。

| 字段名 | 数据类型 | 可空 | 默认值 | 说明 |
|--------|----------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键ID |
| `code` | VARCHAR(50) | NO | - | 节点类型编码（唯一） |
| `name` | VARCHAR(100) | NO | - | 节点类型名称 |
| `category` | VARCHAR(50) | NO | - | 分类（基础/逻辑/数据处理等） |
| `description` | VARCHAR(500) | YES | NULL | 描述 |
| `icon` | VARCHAR(255) | YES | NULL | 图标路径或标识 |
| `default_config` | JSON | YES | NULL | 默认配置（JSON格式） |
| `input_ports` | JSON | YES | NULL | 默认输入端口定义 |
| `output_ports` | JSON | YES | NULL | 默认输出端口定义 |
| `sort_order` | INT | NO | 0 | 排序顺序 |
| `enabled` | TINYINT(1) | NO | 1 | 是否启用（0:禁用, 1:启用） |
| `created_at` | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**建表SQL：**

```sql
CREATE TABLE `workflow_node_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '节点类型编码',
  `name` VARCHAR(100) NOT NULL COMMENT '节点类型名称',
  `category` VARCHAR(50) NOT NULL COMMENT '分类',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标',
  `default_config` JSON DEFAULT NULL COMMENT '默认配置',
  `input_ports` JSON DEFAULT NULL COMMENT '默认输入端口',
  `output_ports` JSON DEFAULT NULL COMMENT '默认输出端口',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_node_type_code` (`code`),
  INDEX `idx_node_type_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点类型表';
```

---

### 3.3 workflow_node（节点表）

存储工作流中的所有节点实例。

| 字段名 | 数据类型 | 可空 | 默认值 | 说明 |
|--------|----------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键ID |
| `workflow_id` | BIGINT | NO | - | 所属工作流ID（外键） |
| `node_uuid` | VARCHAR(36) | NO | - | 节点UUID（前端生成） |
| `type` | VARCHAR(50) | NO | - | 节点类型编码 |
| `type_id` | BIGINT | YES | NULL | 节点类型ID（外键） |
| `name` | VARCHAR(100) | NO | - | 节点名称 |
| `position_x` | INT | NO | 0 | 画布X坐标 |
| `position_y` | INT | NO | 0 | 画布Y坐标 |
| `input_ports` | JSON | YES | NULL | 输入端口定义 |
| `output_ports` | JSON | YES | NULL | 输出端口定义 |
| `input_params` | JSON | YES | NULL | 输入参数定义 |
| `output_params` | JSON | YES | NULL | 输出参数定义 |
| `config` | JSON | YES | NULL | 节点配置参数 |
| `parent_node_id` | BIGINT | YES | NULL | 父节点ID（用于循环体内节点） |
| `created_at` | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**建表SQL：**

```sql
CREATE TABLE `workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` BIGINT NOT NULL COMMENT '所属工作流ID',
  `node_uuid` VARCHAR(36) NOT NULL COMMENT '节点UUID',
  `type` VARCHAR(50) NOT NULL COMMENT '节点类型编码',
  `type_id` BIGINT DEFAULT NULL COMMENT '节点类型ID',
  `name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `position_x` INT NOT NULL DEFAULT 0 COMMENT '画布X坐标',
  `position_y` INT NOT NULL DEFAULT 0 COMMENT '画布Y坐标',
  `input_ports` JSON DEFAULT NULL COMMENT '输入端口定义',
  `output_ports` JSON DEFAULT NULL COMMENT '输出端口定义',
  `input_params` JSON DEFAULT NULL COMMENT '输入参数定义',
  `output_params` JSON DEFAULT NULL COMMENT '输出参数定义',
  `config` JSON DEFAULT NULL COMMENT '节点配置参数',
  `parent_node_id` BIGINT DEFAULT NULL COMMENT '父节点ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_node_workflow_id` (`workflow_id`),
  INDEX `idx_node_uuid` (`node_uuid`),
  INDEX `idx_node_type` (`type`),
  INDEX `idx_node_parent_id` (`parent_node_id`),
  CONSTRAINT `fk_node_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_node_type` FOREIGN KEY (`type_id`) REFERENCES `workflow_node_type` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_node_parent` FOREIGN KEY (`parent_node_id`) REFERENCES `workflow_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流节点表';
```

---

### 3.4 workflow_connection（连线表）

存储节点间的连线关系。

| 字段名 | 数据类型 | 可空 | 默认值 | 说明 |
|--------|----------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键ID |
| `workflow_id` | BIGINT | NO | - | 所属工作流ID（外键） |
| `connection_uuid` | VARCHAR(36) | NO | - | 连线UUID（前端生成） |
| `source_node_id` | BIGINT | NO | - | 源节点ID（外键） |
| `source_port_id` | VARCHAR(50) | NO | - | 源端口ID |
| `target_node_id` | BIGINT | NO | - | 目标节点ID（外键） |
| `target_port_id` | VARCHAR(50) | NO | - | 目标端口ID |
| `source_param_index` | INT | YES | NULL | 源参数索引 |
| `target_param_index` | INT | YES | NULL | 目标参数索引 |
| `label` | VARCHAR(100) | YES | NULL | 连线标签 |
| `created_at` | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |

**建表SQL：**

```sql
CREATE TABLE `workflow_connection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` BIGINT NOT NULL COMMENT '所属工作流ID',
  `connection_uuid` VARCHAR(36) NOT NULL COMMENT '连线UUID',
  `source_node_id` BIGINT NOT NULL COMMENT '源节点ID',
  `source_port_id` VARCHAR(50) NOT NULL COMMENT '源端口ID',
  `target_node_id` BIGINT NOT NULL COMMENT '目标节点ID',
  `target_port_id` VARCHAR(50) NOT NULL COMMENT '目标端口ID',
  `source_param_index` INT DEFAULT NULL COMMENT '源参数索引',
  `target_param_index` INT DEFAULT NULL COMMENT '目标参数索引',
  `label` VARCHAR(100) DEFAULT NULL COMMENT '连线标签',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_connection_workflow_id` (`workflow_id`),
  INDEX `idx_connection_uuid` (`connection_uuid`),
  INDEX `idx_connection_source` (`source_node_id`),
  INDEX `idx_connection_target` (`target_node_id`),
  CONSTRAINT `fk_conn_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conn_source` FOREIGN KEY (`source_node_id`) REFERENCES `workflow_node` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conn_target` FOREIGN KEY (`target_node_id`) REFERENCES `workflow_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流连线表';
```

---

### 3.5 workflow_association（关联表）

存储循环节点与循环体的关联关系。

| 字段名 | 数据类型 | 可空 | 默认值 | 说明 |
|--------|----------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键ID |
| `workflow_id` | BIGINT | NO | - | 所属工作流ID（外键） |
| `loop_node_id` | BIGINT | NO | - | 循环节点ID（外键） |
| `body_node_id` | BIGINT | NO | - | 循环体节点ID（外键） |
| `association_type` | VARCHAR(20) | NO | 'LOOP' | 关联类型（LOOP/CONDITION等） |
| `created_at` | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |

**建表SQL：**

```sql
CREATE TABLE `workflow_association` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` BIGINT NOT NULL COMMENT '所属工作流ID',
  `loop_node_id` BIGINT NOT NULL COMMENT '循环节点ID',
  `body_node_id` BIGINT NOT NULL COMMENT '循环体节点ID',
  `association_type` VARCHAR(20) NOT NULL DEFAULT 'LOOP' COMMENT '关联类型',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_assoc_workflow_id` (`workflow_id`),
  INDEX `idx_assoc_loop_node` (`loop_node_id`),
  INDEX `idx_assoc_body_node` (`body_node_id`),
  CONSTRAINT `fk_assoc_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assoc_loop` FOREIGN KEY (`loop_node_id`) REFERENCES `workflow_node` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assoc_body` FOREIGN KEY (`body_node_id`) REFERENCES `workflow_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流关联表';
```

---

### 3.6 workflow_execution（执行记录表）

存储工作流的执行历史记录。

| 字段名 | 数据类型 | 可空 | 默认值 | 说明 |
|--------|----------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键ID |
| `workflow_id` | BIGINT | NO | - | 工作流ID（外键） |
| `execution_uuid` | VARCHAR(36) | NO | - | 执行UUID |
| `status` | VARCHAR(20) | NO | 'PENDING' | 状态（PENDING/RUNNING/SUCCESS/FAILED/ABORTED） |
| `trigger_type` | VARCHAR(20) | NO | 'MANUAL' | 触发类型（MANUAL/SCHEDULE/API） |
| `triggered_by` | VARCHAR(64) | YES | NULL | 触发人 |
| `input_data` | JSON | YES | NULL | 输入数据 |
| `output_data` | JSON | YES | NULL | 输出数据 |
| `error_message` | TEXT | YES | NULL | 错误信息 |
| `node_executions` | JSON | YES | NULL | 节点执行详情 |
| `progress` | INT | NO | 0 | 执行进度（0-100） |
| `start_time` | DATETIME | YES | NULL | 开始时间 |
| `end_time` | DATETIME | YES | NULL | 结束时间 |
| `duration_ms` | BIGINT | YES | NULL | 执行耗时（毫秒） |
| `created_at` | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**建表SQL：**

```sql
CREATE TABLE `workflow_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` BIGINT NOT NULL COMMENT '工作流ID',
  `execution_uuid` VARCHAR(36) NOT NULL COMMENT '执行UUID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态',
  `trigger_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发类型',
  `triggered_by` VARCHAR(64) DEFAULT NULL COMMENT '触发人',
  `input_data` JSON DEFAULT NULL COMMENT '输入数据',
  `output_data` JSON DEFAULT NULL COMMENT '输出数据',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `node_executions` JSON DEFAULT NULL COMMENT '节点执行详情',
  `progress` INT NOT NULL DEFAULT 0 COMMENT '执行进度',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_exec_workflow_id` (`workflow_id`),
  INDEX `idx_exec_uuid` (`execution_uuid`),
  INDEX `idx_exec_status` (`status`),
  INDEX `idx_exec_start_time` (`start_time`),
  CONSTRAINT `fk_exec_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流执行记录表';
```

---

## 四、索引设计

### 4.1 主键索引
所有表均使用 `BIGINT` 自增主键。

### 4.2 业务索引

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|------|--------|------|------|------|
| `workflow` | `idx_workflow_name` | `name` | 普通 | 按名称查询 |
| `workflow` | `idx_workflow_status` | `status` | 普通 | 按状态筛选 |
| `workflow` | `idx_workflow_created_at` | `created_at` | 普通 | 按时间排序 |
| `workflow_node_type` | `uk_node_type_code` | `code` | 唯一 | 类型编码唯一 |
| `workflow_node_type` | `idx_node_type_category` | `category` | 普通 | 按分类筛选 |
| `workflow_node` | `idx_node_workflow_id` | `workflow_id` | 普通 | 按工作流查询 |
| `workflow_node` | `idx_node_uuid` | `node_uuid` | 普通 | 按UUID查询 |
| `workflow_node` | `idx_node_type` | `type` | 普通 | 按节点类型筛选 |
| `workflow_node` | `idx_node_parent_id` | `parent_node_id` | 普通 | 查询子节点 |
| `workflow_connection` | `idx_connection_workflow_id` | `workflow_id` | 普通 | 按工作流查询 |
| `workflow_connection` | `idx_connection_source` | `source_node_id` | 普通 | 查询出边 |
| `workflow_connection` | `idx_connection_target` | `target_node_id` | 普通 | 查询入边 |
| `workflow_association` | `idx_assoc_workflow_id` | `workflow_id` | 普通 | 按工作流查询 |
| `workflow_execution` | `idx_exec_workflow_id` | `workflow_id` | 普通 | 按工作流查询 |
| `workflow_execution` | `idx_exec_status` | `status` | 普通 | 按状态筛选 |
| `workflow_execution` | `idx_exec_start_time` | `start_time` | 普通 | 按时间排序 |

---

## 五、JPA实体类示例

### 5.1 Workflow.java

```java
package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作流实体类
 */
@Data
@Entity
@Table(name = "workflow")
@SQLDelete(sql = "UPDATE workflow SET deleted = 1 WHERE id = ?")
@Where(clause = "deleted = 0")
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(name = "has_run", nullable = false)
    private Boolean hasRun = false;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<WorkflowNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<WorkflowConnection> connections = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<WorkflowAssociation> associations = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<WorkflowExecution> executions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 5.2 WorkflowStatus.java（枚举）

```java
package com.example.demo.entity;

/**
 * 工作流状态枚举
 */
public enum WorkflowStatus {
    DRAFT,      // 草稿
    PUBLISHED,  // 已发布
    ARCHIVED    // 已归档
}
```

### 5.3 WorkflowNodeType.java

```java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点类型实体类
 */
@Data
@Entity
@Table(name = "workflow_node_type")
public class WorkflowNodeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String icon;

    @Column(name = "default_config", columnDefinition = "JSON")
    private String defaultConfig;

    @Column(name = "input_ports", columnDefinition = "JSON")
    private String inputPorts;

    @Column(name = "output_ports", columnDefinition = "JSON")
    private String outputPorts;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 5.4 WorkflowNode.java

```java
package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流节点实体类
 */
@Data
@Entity
@Table(name = "workflow_node")
public class WorkflowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnore
    private Workflow workflow;

    @Column(name = "node_uuid", nullable = false, length = 36)
    private String nodeUuid;

    @Column(nullable = false, length = 50)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    @JsonIgnore
    private WorkflowNodeType nodeType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "position_x", nullable = false)
    private Integer positionX = 0;

    @Column(name = "position_y", nullable = false)
    private Integer positionY = 0;

    @Column(name = "input_ports", columnDefinition = "JSON")
    private String inputPorts;

    @Column(name = "output_ports", columnDefinition = "JSON")
    private String outputPorts;

    @Column(name = "input_params", columnDefinition = "JSON")
    private String inputParams;

    @Column(name = "output_params", columnDefinition = "JSON")
    private String outputParams;

    @Column(columnDefinition = "JSON")
    private String config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_node_id")
    @JsonIgnore
    private WorkflowNode parentNode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 5.5 WorkflowConnection.java

```java
package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流连线实体类
 */
@Data
@Entity
@Table(name = "workflow_connection")
public class WorkflowConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnore
    private Workflow workflow;

    @Column(name = "connection_uuid", nullable = false, length = 36)
    private String connectionUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id", nullable = false)
    @JsonIgnore
    private WorkflowNode sourceNode;

    @Column(name = "source_port_id", nullable = false, length = 50)
    private String sourcePortId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    @JsonIgnore
    private WorkflowNode targetNode;

    @Column(name = "target_port_id", nullable = false, length = 50)
    private String targetPortId;

    @Column(name = "source_param_index")
    private Integer sourceParamIndex;

    @Column(name = "target_param_index")
    private Integer targetParamIndex;

    @Column(length = 100)
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### 5.6 WorkflowAssociation.java

```java
package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流关联实体类（循环体关联）
 */
@Data
@Entity
@Table(name = "workflow_association")
public class WorkflowAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnore
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loop_node_id", nullable = false)
    @JsonIgnore
    private WorkflowNode loopNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "body_node_id", nullable = false)
    @JsonIgnore
    private WorkflowNode bodyNode;

    @Column(name = "association_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AssociationType associationType = AssociationType.LOOP;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### 5.7 AssociationType.java（枚举）

```java
package com.example.demo.entity;

/**
 * 关联类型枚举
 */
public enum AssociationType {
    LOOP,       // 循环关联
    CONDITION   // 条件关联
}
```

### 5.8 WorkflowExecution.java

```java
package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流执行记录实体类
 */
@Data
@Entity
@Table(name = "workflow_execution")
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnore
    private Workflow workflow;

    @Column(name = "execution_uuid", nullable = false, length = 36)
    private String executionUuid;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "trigger_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType = TriggerType.MANUAL;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(name = "input_data", columnDefinition = "JSON")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "JSON")
    private String outputData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "node_executions", columnDefinition = "JSON")
    private String nodeExecutions;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 5.9 ExecutionStatus.java（枚举）

```java
package com.example.demo.entity;

/**
 * 执行状态枚举
 */
public enum ExecutionStatus {
    PENDING,    // 等待中
    RUNNING,    // 运行中
    SUCCESS,    // 成功
    FAILED,     // 失败
    ABORTED     // 已中止
}
```

### 5.10 TriggerType.java（枚举）

```java
package com.example.demo.entity;

/**
 * 触发类型枚举
 */
public enum TriggerType {
    MANUAL,     // 手动触发
    SCHEDULE,   // 定时触发
    API         // API触发
}
```

---

## 六、设计说明与注意事项

### 6.1 设计原则

1. **规范化设计**：遵循数据库第三范式，减少数据冗余
2. **外键约束**：使用外键保证数据一致性，级联删除简化维护
3. **JSON字段**：灵活存储动态配置，适应前端数据结构
4. **UUID支持**：保留前端生成的UUID，便于前后端数据映射
5. **软删除**：工作流主表使用逻辑删除，防止误删

### 6.2 JSON字段使用说明

| 表名 | 字段 | 说明 |
|------|------|------|
| `workflow_node_type` | `default_config` | 默认配置对象 |
| `workflow_node_type` | `input_ports` / `output_ports` | 端口定义数组 |
| `workflow_node` | `input_ports` / `output_ports` | 端口实例数组 |
| `workflow_node` | `input_params` / `output_params` | 参数定义数组 |
| `workflow_node` | `config` | 节点配置对象 |
| `workflow_execution` | `input_data` / `output_data` | 执行数据对象 |
| `workflow_execution` | `node_executions` | 节点执行详情数组 |

### 6.3 性能优化建议

1. **索引覆盖**：高频查询字段已添加索引
2. **分表策略**：执行记录表数据量大时，可按时间分表
3. **数据归档**：定期归档历史执行记录
4. **连接池**：合理配置数据库连接池参数

### 6.4 扩展性考虑

1. **节点类型扩展**：通过 `workflow_node_type` 表动态管理节点类型
2. **循环嵌套**：通过 `parent_node_id` 支持多层嵌套
3. **版本管理**：`workflow.version` 字段支持版本控制
4. **审计日志**：`created_by` / `updated_by` 字段支持审计

### 6.5 注意事项

1. **MySQL版本**：建议使用 MySQL 5.7+ 以支持 JSON 字段类型
2. **字符集**：使用 `utf8mb4` 支持完整的 Unicode 字符
3. **事务处理**：工作流保存操作应在事务中完成
4. **并发控制**：使用乐观锁（version字段）防止并发更新冲突

---

## 七、初始化数据

### 7.1 节点类型初始化SQL

```sql
-- 基础节点
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('start', '开始节点', 'BASIC', '工作流入口', 1),
('end', '结束节点', 'BASIC', '工作流出口', 2),
('loopBodyCanvas', '循环体容器', 'BASIC', '循环体容器节点', 3);

-- 逻辑控制
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('condition', '条件判断', 'LOGIC', '条件判断节点', 10),
('loop', '循环控制', 'LOGIC', '循环控制节点', 11);

-- 数据准备
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('envConnect', '环境对接', 'DATA_PREPARE', '环境对接节点', 20),
('tableExtract', '表格提取', 'DATA_PREPARE', '表格提取节点', 21);

-- 文本处理
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('textClean', '文本清洗', 'TEXT', '文本清洗', 30),
('textDedupe', '文本去重', 'TEXT', '文本去重', 31),
('textGeneralize', '文本泛化', 'TEXT', '文本泛化', 32),
('textGenerate', '文本生成', 'TEXT', '文本生成', 33);

-- 图像处理
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('imageGenerate', '图像生成', 'IMAGE', '图像生成', 40),
('imageCutout', '抠图', 'IMAGE', '抠图', 41),
('imageEnhance', '画质提升', 'IMAGE', '画质提升', 42);

-- 音视频处理
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('videoExtractAudio', '视频提取音频', 'AUDIO_VIDEO', '视频提取音频', 50),
('audioToText', '音频转文本', 'AUDIO_VIDEO', '音频转文本', 51),
('videoFrame', '视频抽帧', 'AUDIO_VIDEO', '视频抽帧', 52);

-- 测试设计
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('testPlan', '测试方案生成', 'TEST_DESIGN', '测试方案生成', 60);

-- 测试执行
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('apiAuto', 'HTTP(S)接口调用', 'TEST_EXEC', 'HTTP(S)接口调用', 70),
('aiAuto', 'AI自动化执行', 'TEST_EXEC', 'AI自动化执行', 71);

-- 结果评估
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('judgeModel', '裁判模型评估', 'EVALUATE', '裁判模型评估', 80),
('firstTokenLatency', '首Token响应时延', 'EVALUATE', '首Token响应时延', 81),
('tokenOutputTime', '每Token输出耗时', 'EVALUATE', '每Token输出耗时', 82),
('e2eLatency', '端到端时延', 'EVALUATE', '端到端时延', 83);

-- 报告生成
INSERT INTO workflow_node_type (code, name, category, description, sort_order) VALUES
('reportGenerate', '生成测试报告', 'REPORT', '生成测试报告', 90),
('reportAnalysis', '报告分析', 'REPORT', '报告分析', 91);
```

---

*文档版本：1.0*
*创建时间：2026-03-20*
*基于文档：Workflow.md*
