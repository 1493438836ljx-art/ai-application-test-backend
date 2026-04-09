-- ai_test_platform Schema Backup
-- Generated: 2026-04-09T04:06:25.091Z

CREATE DATABASE IF NOT EXISTS `ai_test_platform`;
USE `ai_test_platform`;

-- Table: agent_session
DROP TABLE IF EXISTS `agent_session`;
CREATE TABLE `agent_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key ID',
  `conversation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Conversation ID',
  `workflow_id` bigint DEFAULT NULL COMMENT 'Associated workflow ID',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'Session status',
  `query_results` text COLLATE utf8mb4_unicode_ci COMMENT 'Query results in JSON format',
  `action_results` text COLLATE utf8mb4_unicode_ci COMMENT 'Action results in JSON format',
  `last_reasoning` text COLLATE utf8mb4_unicode_ci COMMENT 'Last AI reasoning content',
  `round_count` int NOT NULL DEFAULT '0' COMMENT 'Current round count',
  `parse_error_count` int NOT NULL DEFAULT '0' COMMENT 'Parse error count for limiting retry attempts',
  `start_time` bigint DEFAULT NULL COMMENT 'Execution start timestamp in milliseconds',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=412 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent session table';

-- Table: async_task
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务唯一标识',
  `task_content` text COLLATE utf8mb4_unicode_ci COMMENT '任务内容',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
  `progress` int DEFAULT '0' COMMENT '执行进度(0-100)',
  `result` text COLLATE utf8mb4_unicode_ci COMMENT '执行结果',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `workflow_id` bigint DEFAULT NULL COMMENT '关联的工作流ID',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_async_status` (`status`),
  KEY `idx_async_workflow` (`workflow_id`),
  KEY `idx_async_session` (`session_id`),
  KEY `idx_async_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步任务表';

-- Table: chat_conversation
DROP TABLE IF EXISTS `chat_conversation`;
CREATE TABLE `chat_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `last_message_at` datetime(6) DEFAULT NULL,
  `message_count` int NOT NULL,
  `metadata` json DEFAULT NULL,
  `status` enum('ACTIVE','ARCHIVED','DELETED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_dfvbi5o7dea9wrv91pxy4mo5f` (`conversation_uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=156 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: chat_feedback
DROP TABLE IF EXISTS `chat_feedback`;
CREATE TABLE `chat_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `feedback_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rating` int NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1xkycipx5h7pjij9b60318poq` (`message_id`),
  CONSTRAINT `FK1xkycipx5h7pjij9b60318poq` FOREIGN KEY (`message_id`) REFERENCES `chat_message` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: chat_message
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` enum('TEXT','MARKDOWN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `latency_ms` bigint DEFAULT NULL,
  `message_uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `metadata` json DEFAULT NULL,
  `model` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('USER','ASSISTANT','SYSTEM') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tokens` int DEFAULT NULL,
  `conversation_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_dtu0fce9sctur4wctjp5madds` (`message_uuid`),
  KEY `FK2ojdav5p4lsot6u8fllhp3h7k` (`conversation_id`),
  CONSTRAINT `FK2ojdav5p4lsot6u8fllhp3h7k` FOREIGN KEY (`conversation_id`) REFERENCES `chat_conversation` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=291 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: chat_quick_question
DROP TABLE IF EXISTS `chat_quick_question`;
CREATE TABLE `chat_quick_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `icon` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL,
  `text` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: client_registry
DROP TABLE IF EXISTS `client_registry`;
CREATE TABLE `client_registry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Client ID',
  `client_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Client名称',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'IDLE' COMMENT '状态: IDLE/BUSY/OFFLINE',
  `running_tasks` int DEFAULT '0' COMMENT '正在执行的任务数',
  `max_concurrency` int DEFAULT '5' COMMENT '最大并发数',
  `cpu_usage` double DEFAULT NULL COMMENT 'CPU使用率',
  `memory_usage` double DEFAULT NULL COMMENT '内存使用率',
  `supported_execution_types` json DEFAULT NULL COMMENT '支持的执行类型',
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Client版本',
  `labels` json DEFAULT NULL COMMENT '标签(用于任务匹配)',
  `last_heartbeat` datetime DEFAULT NULL COMMENT '最后心跳时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `client_id` (`client_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_heartbeat` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Client执行机注册表';

-- Table: data_dictionary
DROP TABLE IF EXISTS `data_dictionary`;
CREATE TABLE `data_dictionary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据字典名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '字典描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- Table: dictionary_column
DROP TABLE IF EXISTS `dictionary_column`;
CREATE TABLE `dictionary_column` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dictionary_id` bigint NOT NULL COMMENT '关联的数据字典ID',
  `column_key` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字段Key',
  `column_label` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字段名称',
  `column_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字段类型',
  `enum_options` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '枚举选项JSON数组',
  `min_value` decimal(20,6) DEFAULT NULL COMMENT '最小值',
  `max_value` decimal(20,6) DEFAULT NULL COMMENT '最大值',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dictionary_id` (`dictionary_id`),
  KEY `idx_sort_order` (`sort_order`),
  CONSTRAINT `fk_column_dictionary` FOREIGN KEY (`dictionary_id`) REFERENCES `data_dictionary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段定义表';

-- Table: execution_log
DROP TABLE IF EXISTS `execution_log`;
CREATE TABLE `execution_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_id` bigint NOT NULL COMMENT '执行记录ID',
  `node_uuid` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点UUID',
  `log_level` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日志级别: DEBUG/INFO/WARN/ERROR',
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日志消息',
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_timestamp` (`timestamp`),
  CONSTRAINT `fk_exec_log_workflow` FOREIGN KEY (`execution_id`) REFERENCES `workflow_execution` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行日志表';

-- Table: skill
DROP TABLE IF EXISTS `skill`;
CREATE TABLE `skill` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Primary key, UUID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Skill name, globally unique',
  `description` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Skill description',
  `suite_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Executable suite file path',
  `suite_filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行套件文件名（带后缀）',
  `execution_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Execution type: AUTOMATED/AI',
  `category` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Category: SYSTEM/USER',
  `access_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Access control: PUBLIC/PRIVATE/WHITELIST/PROJECT',
  `is_container` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Is container: 0=false, 1=true',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Status: PUBLISHED/DRAFT',
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Creator',
  `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Updater',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Logical deletion flag: 0=false, 1=true',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `allow_add_input_params` tinyint(1) DEFAULT '0' COMMENT '是否支持增加入参',
  `allow_add_output_params` tinyint(1) DEFAULT '0' COMMENT '是否支持增加出参',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_name` (`name`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill main table';

-- Table: skill_access_control
DROP TABLE IF EXISTS `skill_access_control`;
CREATE TABLE `skill_access_control` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Primary key, UUID',
  `skill_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Associated Skill ID',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Target type: USER/PROJECT',
  `target_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Target ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_skill_target` (`skill_id`,`target_type`,`target_id`),
  KEY `idx_skill_id` (`skill_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  CONSTRAINT `fk_access_control_skill` FOREIGN KEY (`skill_id`) REFERENCES `skill` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill access control table';

-- Table: skill_parameter
DROP TABLE IF EXISTS `skill_parameter`;
CREATE TABLE `skill_parameter` (
  `id` varchar(36) NOT NULL,
  `skill_id` varchar(36) NOT NULL,
  `param_direction` varchar(10) NOT NULL,
  `param_order` int NOT NULL,
  `param_type` varchar(50) DEFAULT NULL,
  `param_name` varchar(100) DEFAULT NULL,
  `default_value` varchar(1000) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `required` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table: variable_type
DROP TABLE IF EXISTS `variable_type`;
CREATE TABLE `variable_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `element_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `file_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_9pqpu0gutd63fssl138vhou43` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: workflow
DROP TABLE IF EXISTS `workflow`;
CREATE TABLE `workflow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作流名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作流描述',
  `published` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已发布',
  `has_run` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已运行',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `trigger_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'MANUAL' COMMENT '触发类型: MANUAL/SCHEDULE/API',
  `trigger_config` json DEFAULT NULL COMMENT '触发配置(如定时规则)',
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `published_at` datetime DEFAULT NULL COMMENT 'publish time',
  `published_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'publisher',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_name` (`name`),
  KEY `idx_workflow_status` (`status`),
  KEY `idx_workflow_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流主表';

-- Table: workflow_association
DROP TABLE IF EXISTS `workflow_association`;
CREATE TABLE `workflow_association` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '所属工作流ID',
  `loop_node_id` bigint DEFAULT NULL,
  `body_node_id` bigint NOT NULL COMMENT '循环体节点ID',
  `association_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOOP' COMMENT '关联类型',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `container_node_id` bigint DEFAULT NULL COMMENT 'container node ID',
  `container_node_uuid` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'container node UUID',
  `body_node_uuid` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'body node UUID',
  PRIMARY KEY (`id`),
  KEY `idx_assoc_workflow_id` (`workflow_id`),
  KEY `idx_assoc_loop_node` (`loop_node_id`),
  KEY `idx_assoc_body_node` (`body_node_id`)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流关联表';

-- Table: workflow_connection
DROP TABLE IF EXISTS `workflow_connection`;
CREATE TABLE `workflow_connection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '所属工作流ID',
  `connection_uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '连线UUID',
  `source_node_id` bigint NOT NULL COMMENT '源节点ID',
  `source_port_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '源端口ID',
  `target_node_id` bigint NOT NULL COMMENT '目标节点ID',
  `target_port_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标端口ID',
  `branch_label` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分支标签',
  `branch_priority` int DEFAULT NULL COMMENT '分支优先级',
  `source_param_index` int DEFAULT NULL COMMENT '源参数索引',
  `target_param_index` int DEFAULT NULL COMMENT '目标参数索引',
  `label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '连线标签',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_connection_workflow_id` (`workflow_id`),
  KEY `idx_connection_uuid` (`connection_uuid`),
  KEY `idx_connection_source` (`source_node_id`),
  KEY `idx_connection_target` (`target_node_id`)
) ENGINE=InnoDB AUTO_INCREMENT=744 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流连线表';

-- Table: workflow_error_log
DROP TABLE IF EXISTS `workflow_error_log`;
CREATE TABLE `workflow_error_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_id` bigint DEFAULT NULL COMMENT '执行记录ID',
  `workflow_id` bigint DEFAULT NULL COMMENT '工作流ID',
  `node_uuid` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点UUID',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点名称',
  `error_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误类型: RECOVERABLE/BUSINESS/SYSTEM',
  `error_code` int DEFAULT NULL COMMENT '错误代码',
  `error_message` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误消息',
  `error_stack` text COLLATE utf8mb4_unicode_ci COMMENT '错误堆栈',
  `context_json` json DEFAULT NULL COMMENT '上下文信息',
  `retry_count` int DEFAULT NULL COMMENT '重试次数',
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_error_type` (`error_type`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流错误日志表';

-- Table: workflow_execution
DROP TABLE IF EXISTS `workflow_execution`;
CREATE TABLE `workflow_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `execution_uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '执行UUID',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '执行状态',
  `trigger_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL' COMMENT '触发类型',
  `triggered_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '触发人',
  `input_data` text COLLATE utf8mb4_unicode_ci COMMENT '输入数据',
  `output_data` text COLLATE utf8mb4_unicode_ci COMMENT '输出数据',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `node_executions` text COLLATE utf8mb4_unicode_ci COMMENT '节点执行详情',
  `progress` int NOT NULL DEFAULT '0' COMMENT '执行进度',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_exec_workflow_id` (`workflow_id`),
  KEY `idx_exec_uuid` (`execution_uuid`),
  KEY `idx_exec_status` (`status`),
  KEY `idx_exec_start_time` (`start_time`)
) ENGINE=InnoDB AUTO_INCREMENT=128 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流执行记录表';

-- Table: workflow_node
DROP TABLE IF EXISTS `workflow_node`;
CREATE TABLE `workflow_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '所属工作流ID',
  `node_uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点UUID',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点类型编码',
  `type_id` bigint DEFAULT NULL COMMENT '节点类型ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点名称',
  `skill_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '引用的Skill ID',
  `skill_snapshot` json DEFAULT NULL COMMENT 'Skill快照(创建时的参数定义)',
  `position_x` int NOT NULL DEFAULT '0' COMMENT '画布X坐标',
  `position_y` int NOT NULL DEFAULT '0' COMMENT '画布Y坐标',
  `input_ports` text COLLATE utf8mb4_unicode_ci COMMENT '输入端口定义',
  `output_ports` text COLLATE utf8mb4_unicode_ci COMMENT '输出端口定义',
  `input_params` text COLLATE utf8mb4_unicode_ci COMMENT '输入参数定义',
  `output_params` text COLLATE utf8mb4_unicode_ci COMMENT '输出参数定义',
  `config` text COLLATE utf8mb4_unicode_ci COMMENT '节点配置参数',
  `execution_location` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行位置: CLIENT/SERVICE',
  `error_strategy` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'STOP' COMMENT '错误策略: STOP/SKIP/RETRY/ERROR_BRANCH',
  `retry_count` int DEFAULT '3' COMMENT '重试次数',
  `retry_interval` int DEFAULT '1000' COMMENT '重试间隔(毫秒)',
  `error_branch_id` bigint DEFAULT NULL COMMENT '错误处理分支节点ID',
  `condition_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '条件类型: SIMPLE/MULTI',
  `conditions` json DEFAULT NULL COMMENT '条件表达式配置',
  `loop_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '循环类型: COUNT/ARRAY/CONDITION',
  `loop_config` json DEFAULT NULL COMMENT '循环配置',
  `batch_config` json DEFAULT NULL COMMENT '批处理配置',
  `async_config` json DEFAULT NULL COMMENT '异步处理配置',
  `collect_config` json DEFAULT NULL COMMENT '结果收集配置',
  `compatibility_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'COMPATIBLE' COMMENT '兼容性状态: COMPATIBLE/NEEDS_UPDATE/INCOMPATIBLE/INVALID',
  `parent_node_id` bigint DEFAULT NULL COMMENT '父节点ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `node_category` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'BASIC' COMMENT 'node category: BASIC/LOGIC/EXECUTION',
  PRIMARY KEY (`id`),
  KEY `idx_node_workflow_id` (`workflow_id`),
  KEY `idx_node_uuid` (`node_uuid`),
  KEY `idx_node_type` (`type`),
  KEY `idx_node_parent_id` (`parent_node_id`),
  KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1086 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流节点表';

-- Table: workflow_node_execution
DROP TABLE IF EXISTS `workflow_node_execution`;
CREATE TABLE `workflow_node_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_id` bigint NOT NULL COMMENT '执行记录ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `node_uuid` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点UUID',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点类型',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED/TIMEOUT',
  `input_data` json DEFAULT NULL COMMENT '输入参数',
  `output_data` json DEFAULT NULL COMMENT '输出参数',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `error_stack` text COLLATE utf8mb4_unicode_ci COMMENT '错误堆栈',
  `retry_count` int DEFAULT '0' COMMENT '已重试次数',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `client_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行的Client ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_node_uuid` (`node_uuid`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_node_exec_workflow` FOREIGN KEY (`execution_id`) REFERENCES `workflow_execution` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点执行记录表';

-- Table: workflow_node_type
DROP TABLE IF EXISTS `workflow_node_type`;
CREATE TABLE `workflow_node_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `default_config` json DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `input_ports` json DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `output_ports` json DEFAULT NULL,
  `sort_order` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_exidxclradwnmbja1rha99bb1` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=1231 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: workflow_variable_type
DROP TABLE IF EXISTS `workflow_variable_type`;
CREATE TABLE `workflow_variable_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(100) NOT NULL COMMENT '变量类型编码',
  `name` varchar(100) NOT NULL COMMENT '变量类型名称',
  `category` varchar(50) NOT NULL COMMENT '分类 (BASIC/COMPOSITE)',
  `element_type` varchar(50) DEFAULT NULL COMMENT '元素类型（用于数组类型）',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型（用于文件类型）',
  `dictionary_type` varchar(100) DEFAULT NULL COMMENT '数据字典类型（用于Dictionary类型）',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流变量类型表';

