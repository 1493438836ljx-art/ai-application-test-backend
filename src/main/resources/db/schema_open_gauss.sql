-- ai_test_platform Schema for OpenGauss
-- Converted from MySQL schema
-- Generated: 2026-04-09

-- =============================================
-- Table: agent_session
-- =============================================
DROP TABLE IF EXISTS "agent_session" CASCADE CONSTRAINTS;
CREATE TABLE "agent_session" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "conversation_id" VARCHAR(36) NOT NULL,
    "workflow_id" BIGINT DEFAULT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    "query_results" VARCHAR(8000),
    "action_results" VARCHAR(8000),
    "last_reasoning" VARCHAR(2000),
    "round_count" INT NOT NULL DEFAULT 0,
    "parse_error_count" INT NOT NULL DEFAULT 0,
    "start_time" BIGINT DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX "idx_agent_session_conversation_id" ON "agent_session" ("conversation_id");
CREATE INDEX "idx_agent_session_workflow_id" ON "agent_session" ("workflow_id");
CREATE INDEX "idx_agent_session_status" ON "agent_session" ("status");
CREATE INDEX "idx_agent_session_created_at" ON "agent_session" ("created_at");
CREATE TRIGGER "trg_agent_session_updated_at" BEFORE UPDATE ON "agent_session" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "agent_session" IS 'Agent session table';
COMMENT ON COLUMN "agent_session"."id" IS 'Primary key ID';
COMMENT ON COLUMN "agent_session"."conversation_id" IS 'Conversation ID';
COMMENT ON COLUMN "agent_session"."workflow_id" IS 'Associated workflow ID';
COMMENT ON COLUMN "agent_session"."status" IS 'Session status';
COMMENT ON COLUMN "agent_session"."query_results" IS 'Query results in JSON format';
COMMENT ON COLUMN "agent_session"."action_results" IS 'Action results in JSON format';
COMMENT ON COLUMN "agent_session"."last_reasoning" IS 'Last AI reasoning content';
COMMENT ON COLUMN "agent_session"."round_count" IS 'Current round count';
COMMENT ON COLUMN "agent_session"."parse_error_count" IS 'Parse error count for limiting retry attempts';
COMMENT ON COLUMN "agent_session"."start_time" IS 'Execution start timestamp in milliseconds';
COMMENT ON COLUMN "agent_session"."created_at" IS 'Created at';
COMMENT ON COLUMN "agent_session"."updated_at" IS 'Updated at';

-- =============================================
-- Table: async_task
-- =============================================
DROP TABLE IF EXISTS "async_task" CASCADE CONSTRAINTS;
CREATE TABLE "async_task" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "task_id" VARCHAR(64) NOT NULL,
    "task_content" VARCHAR(2000),
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "progress" INT DEFAULT 0,
    "result" VARCHAR(5000),
    "error_message" VARCHAR(2000),
    "workflow_id" BIGINT DEFAULT NULL,
    "session_id" VARCHAR(64) DEFAULT NULL,
    "start_time" TIMESTAMP DEFAULT NULL,
    "end_time" TIMESTAMP DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX "uk_async_task_task_id" ON "async_task" ("task_id");
CREATE INDEX "idx_async_task_status" ON "async_task" ("status");
CREATE INDEX "idx_async_task_workflow" ON "async_task" ("workflow_id");
CREATE INDEX "idx_async_task_session" ON "async_task" ("session_id");
CREATE INDEX "idx_async_task_created" ON "async_task" ("created_at");
CREATE TRIGGER "trg_async_task_updated_at" BEFORE UPDATE ON "async_task" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "async_task" IS '异步任务表';
COMMENT ON COLUMN "async_task"."id" IS '主键ID';
COMMENT ON COLUMN "async_task"."task_id" IS '任务唯一标识';
COMMENT ON COLUMN "async_task"."task_content" IS '任务内容';
COMMENT ON COLUMN "async_task"."status" IS '任务状态';
COMMENT ON COLUMN "async_task"."progress" IS '执行进度(0-100)';
COMMENT ON COLUMN "async_task"."result" IS '执行结果';
COMMENT ON COLUMN "async_task"."error_message" IS '错误信息';
COMMENT ON COLUMN "async_task"."workflow_id" IS '关联的工作流ID';
COMMENT ON COLUMN "async_task"."session_id" IS '会话ID';
COMMENT ON COLUMN "async_task"."start_time" IS '开始时间';
COMMENT ON COLUMN "async_task"."end_time" IS '结束时间';
COMMENT ON COLUMN "async_task"."created_at" IS '创建时间';
COMMENT ON COLUMN "async_task"."updated_at" IS '更新时间';

-- =============================================
-- Table: chat_conversation
-- =============================================
DROP TABLE IF EXISTS "chat_conversation" CASCADE CONSTRAINTS;
CREATE TABLE "chat_conversation" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "conversation_uuid" VARCHAR(36) NOT NULL,
    "created_at" TIMESTAMP NOT NULL,
    "last_message_at" TIMESTAMP DEFAULT NULL,
    "message_count" INT NOT NULL,
    "metadata" VARCHAR(4000) DEFAULT NULL,
    "status" VARCHAR(20) NOT NULL,
    "title" VARCHAR(200) DEFAULT NULL,
    "updated_at" TIMESTAMP NOT NULL,
    "user_id" VARCHAR(64));
CREATE UNIQUE INDEX "uk_chat_conversation_uuid" ON "chat_conversation" ("conversation_uuid");

-- =============================================
-- Table: chat_feedback
-- =============================================
DROP TABLE IF EXISTS "chat_feedback" CASCADE CONSTRAINTS;
CREATE TABLE "chat_feedback" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "comment" VARCHAR(500) DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL,
    "feedback_type" VARCHAR(20) DEFAULT NULL,
    "rating" INT NOT NULL,
    "user_id" VARCHAR(64) DEFAULT NULL,
    "message_id" BIGINT NOT NULL
);
CREATE INDEX "idx_chat_feedback_message_id" ON "chat_feedback" ("message_id");

-- =============================================
-- Table: chat_message
-- =============================================
DROP TABLE IF EXISTS "chat_message" CASCADE CONSTRAINTS;
CREATE TABLE "chat_message" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "content" VARCHAR(5000) NOT NULL,
    "content_type" VARCHAR(20) NOT NULL,
    "created_at" TIMESTAMP NOT NULL,
    "latency_ms" BIGINT DEFAULT NULL,
    "message_uuid" VARCHAR(36) NOT NULL,
    "metadata" VARCHAR(4000) DEFAULT NULL,
    "model" VARCHAR(50) DEFAULT NULL,
    "role" VARCHAR(20) NOT NULL,
    "tokens" INT DEFAULT NULL,
    "conversation_id" BIGINT NOT NULL
);
CREATE UNIQUE INDEX "uk_chat_message_uuid" ON "chat_message" ("message_uuid");
CREATE INDEX "idx_chat_message_conversation_id" ON "chat_message" ("conversation_id");

-- =============================================
-- Table: chat_quick_question
-- =============================================
DROP TABLE IF EXISTS "chat_quick_question" CASCADE CONSTRAINTS;
CREATE TABLE "chat_quick_question" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "category" VARCHAR(50) DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL,
    "enabled" NUMBER(1) NOT NULL,
    "icon" VARCHAR(10) DEFAULT NULL,
    "sort_order" INT NOT NULL,
    "text" VARCHAR(200) NOT NULL,
    "updated_at" TIMESTAMP NOT NULL
);

-- =============================================
-- Table: client_registry
-- =============================================
DROP TABLE IF EXISTS "client_registry" CASCADE CONSTRAINTS;
CREATE TABLE "client_registry" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "client_id" VARCHAR(100) NOT NULL,
    "client_name" VARCHAR(200) DEFAULT NULL,
    "status" VARCHAR(20) DEFAULT 'IDLE',
    "running_tasks" INT DEFAULT 0,
    "max_concurrency" INT DEFAULT 5,
    "cpu_usage" NUMBER(20,6) DEFAULT NULL,
    "memory_usage" NUMBER(20,6) DEFAULT NULL,
    "supported_execution_types" VARCHAR(4000) DEFAULT NULL,
    "version" VARCHAR(50) DEFAULT NULL,
    "labels" VARCHAR(4000) DEFAULT NULL,
    "last_heartbeat" TIMESTAMP DEFAULT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX "uk_client_registry_client_id" ON "client_registry" ("client_id");
CREATE INDEX "idx_client_registry_status" ON "client_registry" ("status");
CREATE INDEX "idx_client_registry_last_heartbeat" ON "client_registry" ("last_heartbeat");
CREATE TRIGGER "trg_client_registry_updated_at" BEFORE UPDATE ON "client_registry" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "client_registry" IS 'Client执行机注册表';
COMMENT ON COLUMN "client_registry"."client_id" IS 'Client ID';
COMMENT ON COLUMN "client_registry"."client_name" IS 'Client名称';
COMMENT ON COLUMN "client_registry"."status" IS '状态: IDLE/BUSY/OFFLINE';
COMMENT ON COLUMN "client_registry"."running_tasks" IS '正在执行的任务数';
COMMENT ON COLUMN "client_registry"."max_concurrency" IS '最大并发数';
COMMENT ON COLUMN "client_registry"."cpu_usage" IS 'CPU使用率';
COMMENT ON COLUMN "client_registry"."memory_usage" IS '内存使用率';
COMMENT ON COLUMN "client_registry"."supported_execution_types" IS '支持的执行类型';
COMMENT ON COLUMN "client_registry"."version" IS 'Client版本';
COMMENT ON COLUMN "client_registry"."labels" IS '标签(用于任务匹配)';
COMMENT ON COLUMN "client_registry"."last_heartbeat" IS '最后心跳时间';

-- =============================================
-- Table: data_dictionary
-- =============================================
DROP TABLE IF EXISTS "data_dictionary" CASCADE CONSTRAINTS;
CREATE TABLE "data_dictionary" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "name" VARCHAR(50) NOT NULL,
    "description" VARCHAR(500) DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "is_deleted" NUMBER(1) NOT NULL DEFAULT 0
);
CREATE INDEX "idx_data_dictionary_name" ON "data_dictionary" ("name");
CREATE INDEX "idx_data_dictionary_created_at" ON "data_dictionary" ("created_at");
CREATE TRIGGER "trg_data_dictionary_updated_at" BEFORE UPDATE ON "data_dictionary" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "data_dictionary" IS '数据字典表';
COMMENT ON COLUMN "data_dictionary"."id" IS '主键ID';
COMMENT ON COLUMN "data_dictionary"."name" IS '数据字典名称';
COMMENT ON COLUMN "data_dictionary"."description" IS '字典描述';
COMMENT ON COLUMN "data_dictionary"."created_at" IS '创建时间';
COMMENT ON COLUMN "data_dictionary"."updated_at" IS '更新时间';
COMMENT ON COLUMN "data_dictionary"."is_deleted" IS '逻辑删除标记';

-- =============================================
-- Table: dictionary_column
-- =============================================
DROP TABLE IF EXISTS "dictionary_column" CASCADE CONSTRAINTS;
CREATE TABLE "dictionary_column" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "dictionary_id" BIGINT NOT NULL,
    "column_key" VARCHAR(50) NOT NULL,
    "column_label" VARCHAR(50) NOT NULL,
    "column_type" VARCHAR(20) NOT NULL,
    "enum_options" VARCHAR(1000) DEFAULT NULL,
    "min_value" NUMBER(20,6) DEFAULT NULL,
    "max_value" NUMBER(20,6) DEFAULT NULL,
    "sort_order" INT NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "idx_dictionary_column_dictionary_id" ON "dictionary_column" ("dictionary_id");
CREATE INDEX "idx_dictionary_column_sort_order" ON "dictionary_column" ("sort_order");
CREATE TRIGGER "trg_dictionary_column_updated_at" BEFORE UPDATE ON "dictionary_column" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "dictionary_column" IS '字段定义表';
COMMENT ON COLUMN "dictionary_column"."id" IS '主键ID';
COMMENT ON COLUMN "dictionary_column"."dictionary_id" IS '关联的数据字典ID';
COMMENT ON COLUMN "dictionary_column"."column_key" IS '字段Key';
COMMENT ON COLUMN "dictionary_column"."column_label" IS '字段名称';
COMMENT ON COLUMN "dictionary_column"."column_type" IS '字段类型';
COMMENT ON COLUMN "dictionary_column"."enum_options" IS '枚举选项JSON数组';
COMMENT ON COLUMN "dictionary_column"."min_value" IS '最小值';
COMMENT ON COLUMN "dictionary_column"."max_value" IS '最大值';
COMMENT ON COLUMN "dictionary_column"."sort_order" IS '排序序号';
COMMENT ON COLUMN "dictionary_column"."created_at" IS '创建时间';
COMMENT ON COLUMN "dictionary_column"."updated_at" IS '更新时间';

-- =============================================
-- Table: execution_log
-- =============================================
DROP TABLE IF EXISTS "execution_log" CASCADE CONSTRAINTS;
CREATE TABLE "execution_log" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "execution_id" BIGINT NOT NULL,
    "node_uuid" VARCHAR(50) DEFAULT NULL,
    "log_level" VARCHAR(20) NOT NULL,
    "message" VARCHAR(2000) NOT NULL,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "idx_execution_log_execution_id" ON "execution_log" ("execution_id");
CREATE INDEX "idx_execution_log_timestamp" ON "execution_log" ("timestamp");
COMMENT ON TABLE "execution_log" IS '执行日志表';
COMMENT ON COLUMN "execution_log"."execution_id" IS '执行记录ID';
COMMENT ON COLUMN "execution_log"."node_uuid" IS '节点UUID';
COMMENT ON COLUMN "execution_log"."log_level" IS '日志级别: DEBUG/INFO/WARN/ERROR';
COMMENT ON COLUMN "execution_log"."message" IS '日志消息';
COMMENT ON COLUMN "execution_log"."timestamp" IS '时间戳';

-- =============================================
-- Table: skill
-- =============================================
DROP TABLE IF EXISTS "skill" CASCADE CONSTRAINTS;
CREATE TABLE "skill" (
    "id" VARCHAR(36) NOT NULL PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,
    "description" VARCHAR(2000) DEFAULT NULL,
    "suite_path" VARCHAR(500) DEFAULT NULL,
    "suite_filename" VARCHAR(255) DEFAULT NULL,
    "execution_type" VARCHAR(20) NOT NULL,
    "category" VARCHAR(20) NOT NULL,
    "access_type" VARCHAR(20) NOT NULL,
    "is_container" NUMBER(1) NOT NULL DEFAULT 0,
    "status" VARCHAR(20) NOT NULL,
    "created_by" VARCHAR(100) NOT NULL,
    "updated_by" VARCHAR(100) NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" NUMBER(1) NOT NULL DEFAULT 0,
    "deleted_at" TIMESTAMP DEFAULT NULL,
    "allow_add_input_params" NUMBER(1) DEFAULT 0,
    "allow_add_output_params" NUMBER(1) DEFAULT 0
);
CREATE UNIQUE INDEX "uk_skill_name" ON "skill" ("name");
CREATE INDEX "idx_skill_category" ON "skill" ("category");
CREATE INDEX "idx_skill_status" ON "skill" ("status");
CREATE INDEX "idx_skill_created_by" ON "skill" ("created_by");
CREATE TRIGGER "trg_skill_updated_at" BEFORE UPDATE ON "skill" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "skill" IS 'Skill main table';
COMMENT ON COLUMN "skill"."id" IS 'Primary key, UUID';
COMMENT ON COLUMN "skill"."name" IS 'Skill name, globally unique';
COMMENT ON COLUMN "skill"."description" IS 'Skill description';
COMMENT ON COLUMN "skill"."suite_path" IS 'Executable suite file path';
COMMENT ON COLUMN "skill"."suite_filename" IS '执行套件文件名（带后缀）';
COMMENT ON COLUMN "skill"."execution_type" IS 'Execution type: AUTOMATED/AI';
COMMENT ON COLUMN "skill"."category" IS 'Category: SYSTEM/USER';
COMMENT ON COLUMN "skill"."access_type" IS 'Access control: PUBLIC/PRIVATE/WHITELIST/PROJECT';
COMMENT ON COLUMN "skill"."is_container" IS 'Is container: false/true';
COMMENT ON COLUMN "skill"."status" IS 'Status: PUBLISHED/DRAFT';
COMMENT ON COLUMN "skill"."created_by" IS 'Creator';
COMMENT ON COLUMN "skill"."updated_by" IS 'Updater';
COMMENT ON COLUMN "skill"."deleted" IS 'Logical deletion flag: false/true';
COMMENT ON COLUMN "skill"."deleted_at" IS '删除时间';
COMMENT ON COLUMN "skill"."allow_add_input_params" IS '是否支持增加入参';
COMMENT ON COLUMN "skill"."allow_add_output_params" IS '是否支持增加出参';

-- =============================================
-- Table: skill_access_control
-- =============================================
DROP TABLE IF EXISTS "skill_access_control" CASCADE CONSTRAINTS;
CREATE TABLE "skill_access_control" (
    "id" VARCHAR(36) NOT NULL PRIMARY KEY,
    "skill_id" VARCHAR(36) NOT NULL,
    "target_type" VARCHAR(20) NOT NULL,
    "target_id" VARCHAR(36) NOT NULL
);
CREATE UNIQUE INDEX "uk_skill_access_control_target" ON "skill_access_control" ("skill_id", "target_type", "target_id");
CREATE INDEX "idx_skill_access_control_skill_id" ON "skill_access_control" ("skill_id");
CREATE INDEX "idx_skill_access_control_target" ON "skill_access_control" ("target_type", "target_id");
COMMENT ON TABLE "skill_access_control" IS 'Skill access control table';
COMMENT ON COLUMN "skill_access_control"."id" IS 'Primary key, UUID';
COMMENT ON COLUMN "skill_access_control"."skill_id" IS 'Associated Skill ID';
COMMENT ON COLUMN "skill_access_control"."target_type" IS 'Target type: USER/PROJECT';
COMMENT ON COLUMN "skill_access_control"."target_id" IS 'Target ID';

-- =============================================
-- Table: skill_parameter
-- =============================================
DROP TABLE IF EXISTS "skill_parameter" CASCADE CONSTRAINTS;
CREATE TABLE "skill_parameter" (
    "id" VARCHAR(36) NOT NULL PRIMARY KEY,
    "skill_id" VARCHAR(36) NOT NULL,
    "param_direction" VARCHAR(10) NOT NULL,
    "param_order" INT NOT NULL,
    "param_type" VARCHAR(50) DEFAULT NULL,
    "param_name" VARCHAR(100) DEFAULT NULL,
    "default_value" VARCHAR(1000) DEFAULT NULL,
    "description" VARCHAR(500) DEFAULT NULL,
    "required" NUMBER(1) DEFAULT 0
);
CREATE INDEX "idx_skill_parameter_skill_id" ON "skill_parameter" ("skill_id");

-- =============================================
-- Table: variable_type
-- =============================================
DROP TABLE IF EXISTS "variable_type" CASCADE CONSTRAINTS;
CREATE TABLE "variable_type" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "category" VARCHAR(50) NOT NULL,
    "code" VARCHAR(50) NOT NULL,
    "created_at" TIMESTAMP NOT NULL,
    "description" VARCHAR(500) DEFAULT NULL,
    "element_type" VARCHAR(50) DEFAULT NULL,
    "enabled" NUMBER(1) NOT NULL,
    "file_type" VARCHAR(50) DEFAULT NULL,
    "name" VARCHAR(100) NOT NULL,
    "sort_order" INT NOT NULL,
    "updated_at" TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX "uk_variable_type_code" ON "variable_type" ("code");

-- =============================================
-- Table: workflow
-- =============================================
DROP TABLE IF EXISTS "workflow" CASCADE CONSTRAINTS;
CREATE TABLE "workflow" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,
    "description" VARCHAR(500) DEFAULT NULL,
    "published" NUMBER(1) NOT NULL DEFAULT 0,
    "has_run" NUMBER(1) NOT NULL DEFAULT 0,
    "version" INT NOT NULL DEFAULT 1,
    "status" VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    "trigger_type" VARCHAR(20) DEFAULT 'MANUAL',
    "trigger_config" VARCHAR(4000) DEFAULT NULL,
    "created_by" VARCHAR(64) DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_by" VARCHAR(64) DEFAULT NULL,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted" NUMBER(1) NOT NULL DEFAULT 0,
    "published_at" TIMESTAMP DEFAULT NULL,
    "published_by" VARCHAR(100));
CREATE INDEX "idx_workflow_name" ON "workflow" ("name");
CREATE INDEX "idx_workflow_status" ON "workflow" ("status");
CREATE INDEX "idx_workflow_created_at" ON "workflow" ("created_at");
CREATE TRIGGER "trg_workflow_updated_at" BEFORE UPDATE ON "workflow" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "workflow" IS '工作流主表';
COMMENT ON COLUMN "workflow"."id" IS '主键ID';
COMMENT ON COLUMN "workflow"."name" IS '工作流名称';
COMMENT ON COLUMN "workflow"."description" IS '工作流描述';
COMMENT ON COLUMN "workflow"."published" IS '是否已发布';
COMMENT ON COLUMN "workflow"."has_run" IS '是否已运行';
COMMENT ON COLUMN "workflow"."version" IS '版本号';
COMMENT ON COLUMN "workflow"."status" IS '状态';
COMMENT ON COLUMN "workflow"."trigger_type" IS '触发类型: MANUAL/SCHEDULE/API';
COMMENT ON COLUMN "workflow"."trigger_config" IS '触发配置(如定时规则)';
COMMENT ON COLUMN "workflow"."created_by" IS '创建人';
COMMENT ON COLUMN "workflow"."created_at" IS '创建时间';
COMMENT ON COLUMN "workflow"."updated_by" IS '更新人';
COMMENT ON COLUMN "workflow"."updated_at" IS '更新时间';
COMMENT ON COLUMN "workflow"."deleted" IS '逻辑删除标记';
COMMENT ON COLUMN "workflow"."published_at" IS 'publish time';
COMMENT ON COLUMN "workflow"."published_by" IS 'publisher';

-- =============================================
-- Table: workflow_association
-- =============================================
DROP TABLE IF EXISTS "workflow_association" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_association" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "workflow_id" BIGINT NOT NULL,
    "loop_node_id" BIGINT DEFAULT NULL,
    "body_node_id" BIGINT NOT NULL,
    "association_type" VARCHAR(50) NOT NULL DEFAULT 'LOOP',
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "container_node_id" BIGINT DEFAULT NULL,
    "container_node_uuid" VARCHAR(36) DEFAULT NULL,
    "body_node_uuid" VARCHAR(36));
CREATE INDEX "idx_workflow_association_workflow_id" ON "workflow_association" ("workflow_id");
CREATE INDEX "idx_workflow_association_loop_node" ON "workflow_association" ("loop_node_id");
CREATE INDEX "idx_workflow_association_body_node" ON "workflow_association" ("body_node_id");
COMMENT ON TABLE "workflow_association" IS '工作流关联表';
COMMENT ON COLUMN "workflow_association"."id" IS '主键ID';
COMMENT ON COLUMN "workflow_association"."workflow_id" IS '所属工作流ID';
COMMENT ON COLUMN "workflow_association"."association_type" IS '关联类型';
COMMENT ON COLUMN "workflow_association"."container_node_uuid" IS 'container node UUID';
COMMENT ON COLUMN "workflow_association"."body_node_uuid" IS 'body node UUID';

-- =============================================
-- Table: workflow_connection
-- =============================================
DROP TABLE IF EXISTS "workflow_connection" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_connection" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "workflow_id" BIGINT NOT NULL,
    "connection_uuid" VARCHAR(36) NOT NULL,
    "source_node_id" BIGINT NOT NULL,
    "source_port_id" VARCHAR(50) NOT NULL,
    "target_node_id" BIGINT NOT NULL,
    "target_port_id" VARCHAR(50) NOT NULL,
    "branch_label" VARCHAR(50) DEFAULT NULL,
    "branch_priority" INT DEFAULT NULL,
    "source_param_index" INT DEFAULT NULL,
    "target_param_index" INT DEFAULT NULL,
    "label" VARCHAR(100) DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "idx_workflow_connection_workflow_id" ON "workflow_connection" ("workflow_id");
CREATE INDEX "idx_workflow_connection_uuid" ON "workflow_connection" ("connection_uuid");
CREATE INDEX "idx_workflow_connection_source" ON "workflow_connection" ("source_node_id");
CREATE INDEX "idx_workflow_connection_target" ON "workflow_connection" ("target_node_id");
COMMENT ON TABLE "workflow_connection" IS '工作流连线表';
COMMENT ON COLUMN "workflow_connection"."id" IS '主键ID';
COMMENT ON COLUMN "workflow_connection"."workflow_id" IS '所属工作流ID';
COMMENT ON COLUMN "workflow_connection"."connection_uuid" IS '连线UUID';
COMMENT ON COLUMN "workflow_connection"."source_node_id" IS '源节点ID';
COMMENT ON COLUMN "workflow_connection"."source_port_id" IS '源端口ID';
COMMENT ON COLUMN "workflow_connection"."target_node_id" IS '目标节点ID';
COMMENT ON COLUMN "workflow_connection"."target_port_id" IS '目标端口ID';
COMMENT ON COLUMN "workflow_connection"."branch_label" IS '分支标签';
COMMENT ON COLUMN "workflow_connection"."branch_priority" IS '分支优先级';
COMMENT ON COLUMN "workflow_connection"."source_param_index" IS '源参数索引';
COMMENT ON COLUMN "workflow_connection"."target_param_index" IS '目标参数索引';
COMMENT ON COLUMN "workflow_connection"."label" IS '连线标签';
COMMENT ON COLUMN "workflow_connection"."created_at" IS '创建时间';

-- =============================================
-- Table: workflow_error_log
-- =============================================
DROP TABLE IF EXISTS "workflow_error_log" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_error_log" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "execution_id" BIGINT DEFAULT NULL,
    "workflow_id" BIGINT DEFAULT NULL,
    "node_uuid" VARCHAR(50) DEFAULT NULL,
    "node_name" VARCHAR(100) DEFAULT NULL,
    "error_type" VARCHAR(50) DEFAULT NULL,
    "error_code" INT DEFAULT NULL,
    "error_message" VARCHAR(2000) DEFAULT NULL,
    "error_stack" VARCHAR(5000),
    "context_json" VARCHAR(4000),
    "retry_count" INT DEFAULT NULL,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "idx_workflow_error_log_execution_id" ON "workflow_error_log" ("execution_id");
CREATE INDEX "idx_workflow_error_log_workflow_id" ON "workflow_error_log" ("workflow_id");
CREATE INDEX "idx_workflow_error_log_error_type" ON "workflow_error_log" ("error_type");
CREATE INDEX "idx_workflow_error_log_timestamp" ON "workflow_error_log" ("timestamp");
COMMENT ON TABLE "workflow_error_log" IS '工作流错误日志表';
COMMENT ON COLUMN "workflow_error_log"."execution_id" IS '执行记录ID';
COMMENT ON COLUMN "workflow_error_log"."workflow_id" IS '工作流ID';
COMMENT ON COLUMN "workflow_error_log"."node_uuid" IS '节点UUID';
COMMENT ON COLUMN "workflow_error_log"."node_name" IS '节点名称';
COMMENT ON COLUMN "workflow_error_log"."error_type" IS '错误类型: RECOVERABLE/BUSINESS/SYSTEM';
COMMENT ON COLUMN "workflow_error_log"."error_code" IS '错误代码';
COMMENT ON COLUMN "workflow_error_log"."error_message" IS '错误消息';
COMMENT ON COLUMN "workflow_error_log"."error_stack" IS '错误堆栈';
COMMENT ON COLUMN "workflow_error_log"."context_json" IS '上下文信息';
COMMENT ON COLUMN "workflow_error_log"."retry_count" IS '重试次数';
COMMENT ON COLUMN "workflow_error_log"."timestamp" IS '发生时间';

-- =============================================
-- Table: workflow_execution
-- =============================================
DROP TABLE IF EXISTS "workflow_execution" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_execution" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "workflow_id" BIGINT NOT NULL,
    "execution_uuid" VARCHAR(36) NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "trigger_type" VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    "triggered_by" VARCHAR(64) DEFAULT NULL,
    "input_data" VARCHAR(8000),
    "output_data" VARCHAR(8000),
    "error_message" VARCHAR(2000),
    "node_executions" VARCHAR(8000),
    "progress" INT NOT NULL DEFAULT 0,
    "start_time" TIMESTAMP DEFAULT NULL,
    "end_time" TIMESTAMP DEFAULT NULL,
    "duration_ms" BIGINT DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "idx_workflow_execution_workflow_id" ON "workflow_execution" ("workflow_id");
CREATE INDEX "idx_workflow_execution_uuid" ON "workflow_execution" ("execution_uuid");
CREATE INDEX "idx_workflow_execution_status" ON "workflow_execution" ("status");
CREATE INDEX "idx_workflow_execution_start_time" ON "workflow_execution" ("start_time");
CREATE TRIGGER "trg_workflow_execution_updated_at" BEFORE UPDATE ON "workflow_execution" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "workflow_execution" IS '工作流执行记录表';
COMMENT ON COLUMN "workflow_execution"."id" IS '主键ID';
COMMENT ON COLUMN "workflow_execution"."workflow_id" IS '工作流ID';
COMMENT ON COLUMN "workflow_execution"."execution_uuid" IS '执行UUID';
COMMENT ON COLUMN "workflow_execution"."status" IS '执行状态';
COMMENT ON COLUMN "workflow_execution"."trigger_type" IS '触发类型';
COMMENT ON COLUMN "workflow_execution"."triggered_by" IS '触发人';
COMMENT ON COLUMN "workflow_execution"."input_data" IS '输入数据';
COMMENT ON COLUMN "workflow_execution"."output_data" IS '输出数据';
COMMENT ON COLUMN "workflow_execution"."error_message" IS '错误信息';
COMMENT ON COLUMN "workflow_execution"."node_executions" IS '节点执行详情';
COMMENT ON COLUMN "workflow_execution"."progress" IS '执行进度';
COMMENT ON COLUMN "workflow_execution"."start_time" IS '开始时间';
COMMENT ON COLUMN "workflow_execution"."end_time" IS '结束时间';
COMMENT ON COLUMN "workflow_execution"."duration_ms" IS '执行耗时(毫秒)';
COMMENT ON COLUMN "workflow_execution"."created_at" IS '创建时间';
COMMENT ON COLUMN "workflow_execution"."updated_at" IS '更新时间';

-- =============================================
-- Table: workflow_node
-- =============================================
DROP TABLE IF EXISTS "workflow_node" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_node" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "workflow_id" BIGINT NOT NULL,
    "node_uuid" VARCHAR(36) NOT NULL,
    "type" VARCHAR(50) NOT NULL,
    "type_id" BIGINT DEFAULT NULL,
    "name" VARCHAR(100) NOT NULL,
    "skill_id" VARCHAR(50) DEFAULT NULL,
    "skill_snapshot" VARCHAR(4000) DEFAULT NULL,
    "position_x" INT NOT NULL DEFAULT 0,
    "position_y" INT NOT NULL DEFAULT 0,
    "input_ports" VARCHAR(4000),
    "output_ports" VARCHAR(4000),
    "input_params" VARCHAR(4000),
    "output_params" VARCHAR(4000),
    "config" VARCHAR(4000),
    "execution_location" VARCHAR(20) DEFAULT NULL,
    "error_strategy" VARCHAR(20) DEFAULT 'STOP',
    "retry_count" INT DEFAULT 3,
    "retry_interval" INT DEFAULT 1000,
    "error_branch_id" BIGINT DEFAULT NULL,
    "condition_type" VARCHAR(20) DEFAULT NULL,
    "conditions" VARCHAR(4000) DEFAULT NULL,
    "loop_type" VARCHAR(20) DEFAULT NULL,
    "loop_config" VARCHAR(4000) DEFAULT NULL,
    "batch_config" VARCHAR(4000) DEFAULT NULL,
    "async_config" VARCHAR(4000) DEFAULT NULL,
    "collect_config" VARCHAR(4000) DEFAULT NULL,
    "compatibility_status" VARCHAR(20) DEFAULT 'COMPATIBLE',
    "parent_node_id" BIGINT DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "node_category" VARCHAR(20) DEFAULT 'BASIC'
);
CREATE INDEX "idx_workflow_node_workflow_id" ON "workflow_node" ("workflow_id");
CREATE INDEX "idx_workflow_node_uuid" ON "workflow_node" ("node_uuid");
CREATE INDEX "idx_workflow_node_type" ON "workflow_node" ("type");
CREATE INDEX "idx_workflow_node_parent_id" ON "workflow_node" ("parent_node_id");
CREATE INDEX "idx_workflow_node_skill_id" ON "workflow_node" ("skill_id");
CREATE TRIGGER "trg_workflow_node_updated_at" BEFORE UPDATE ON "workflow_node" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "workflow_node" IS '工作流节点表';
COMMENT ON COLUMN "workflow_node"."id" IS '主键ID';
COMMENT ON COLUMN "workflow_node"."workflow_id" IS '所属工作流ID';
COMMENT ON COLUMN "workflow_node"."node_uuid" IS '节点UUID';
COMMENT ON COLUMN "workflow_node"."type" IS '节点类型编码';
COMMENT ON COLUMN "workflow_node"."type_id" IS '节点类型ID';
COMMENT ON COLUMN "workflow_node"."name" IS '节点名称';
COMMENT ON COLUMN "workflow_node"."skill_id" IS '引用的Skill ID';
COMMENT ON COLUMN "workflow_node"."skill_snapshot" IS 'Skill快照(创建时的参数定义)';
COMMENT ON COLUMN "workflow_node"."position_x" IS '画布X坐标';
COMMENT ON COLUMN "workflow_node"."position_y" IS '画布Y坐标';
COMMENT ON COLUMN "workflow_node"."input_ports" IS '输入端口定义';
COMMENT ON COLUMN "workflow_node"."output_ports" IS '输出端口定义';
COMMENT ON COLUMN "workflow_node"."input_params" IS '输入参数定义';
COMMENT ON COLUMN "workflow_node"."output_params" IS '输出参数定义';
COMMENT ON COLUMN "workflow_node"."config" IS '节点配置参数';
COMMENT ON COLUMN "workflow_node"."execution_location" IS '执行位置: CLIENT/SERVICE';
COMMENT ON COLUMN "workflow_node"."error_strategy" IS '错误策略: STOP/SKIP/RETRY/ERROR_BRANCH';
COMMENT ON COLUMN "workflow_node"."retry_count" IS '重试次数';
COMMENT ON COLUMN "workflow_node"."retry_interval" IS '重试间隔(毫秒)';
COMMENT ON COLUMN "workflow_node"."error_branch_id" IS '错误处理分支节点ID';
COMMENT ON COLUMN "workflow_node"."condition_type" IS '条件类型: SIMPLE/MULTI';
COMMENT ON COLUMN "workflow_node"."conditions" IS '条件表达式配置';
COMMENT ON COLUMN "workflow_node"."loop_type" IS '循环类型: COUNT/ARRAY/CONDITION';
COMMENT ON COLUMN "workflow_node"."loop_config" IS '循环配置';
COMMENT ON COLUMN "workflow_node"."batch_config" IS '批处理配置';
COMMENT ON COLUMN "workflow_node"."async_config" IS '异步处理配置';
COMMENT ON COLUMN "workflow_node"."collect_config" IS '结果收集配置';
COMMENT ON COLUMN "workflow_node"."compatibility_status" IS '兼容性状态: COMPATIBLE/NEEDS_UPDATE/INCOMPATIBLE/INVALID';
COMMENT ON COLUMN "workflow_node"."parent_node_id" IS '父节点ID';
COMMENT ON COLUMN "workflow_node"."node_category" IS 'node category: BASIC/LOGIC/EXECUTION';

-- =============================================
-- Table: workflow_node_execution
-- =============================================
DROP TABLE IF EXISTS "workflow_node_execution" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_node_execution" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "execution_id" BIGINT NOT NULL,
    "workflow_id" BIGINT NOT NULL,
    "node_uuid" VARCHAR(50) NOT NULL,
    "node_name" VARCHAR(100) DEFAULT NULL,
    "node_type" VARCHAR(50) DEFAULT NULL,
    "status" VARCHAR(20) NOT NULL,
    "input_data" VARCHAR(4000) DEFAULT NULL,
    "output_data" VARCHAR(4000) DEFAULT NULL,
    "error_message" VARCHAR(2000),
    "error_stack" VARCHAR(5000),
    "retry_count" INT DEFAULT 0,
    "start_time" TIMESTAMP DEFAULT NULL,
    "end_time" TIMESTAMP DEFAULT NULL,
    "duration_ms" BIGINT DEFAULT NULL,
    "client_id" VARCHAR(100) DEFAULT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "idx_workflow_node_execution_execution_id" ON "workflow_node_execution" ("execution_id");
CREATE INDEX "idx_workflow_node_execution_workflow_id" ON "workflow_node_execution" ("workflow_id");
CREATE INDEX "idx_workflow_node_execution_node_uuid" ON "workflow_node_execution" ("node_uuid");
CREATE INDEX "idx_workflow_node_execution_status" ON "workflow_node_execution" ("status");
CREATE TRIGGER "trg_workflow_node_execution_updated_at" BEFORE UPDATE ON "workflow_node_execution" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "workflow_node_execution" IS '节点执行记录表';
COMMENT ON COLUMN "workflow_node_execution"."execution_id" IS '执行记录ID';
COMMENT ON COLUMN "workflow_node_execution"."workflow_id" IS '工作流ID';
COMMENT ON COLUMN "workflow_node_execution"."node_uuid" IS '节点UUID';
COMMENT ON COLUMN "workflow_node_execution"."node_name" IS '节点名称';
COMMENT ON COLUMN "workflow_node_execution"."node_type" IS '节点类型';
COMMENT ON COLUMN "workflow_node_execution"."status" IS '状态: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED/TIMEOUT';
COMMENT ON COLUMN "workflow_node_execution"."input_data" IS '输入参数';
COMMENT ON COLUMN "workflow_node_execution"."output_data" IS '输出参数';
COMMENT ON COLUMN "workflow_node_execution"."error_message" IS '错误信息';
COMMENT ON COLUMN "workflow_node_execution"."error_stack" IS '错误堆栈';
COMMENT ON COLUMN "workflow_node_execution"."retry_count" IS '已重试次数';
COMMENT ON COLUMN "workflow_node_execution"."start_time" IS '开始时间';
COMMENT ON COLUMN "workflow_node_execution"."end_time" IS '结束时间';
COMMENT ON COLUMN "workflow_node_execution"."duration_ms" IS '执行耗时(毫秒)';
COMMENT ON COLUMN "workflow_node_execution"."client_id" IS '执行的Client ID';

-- =============================================
-- Table: workflow_node_type
-- =============================================
DROP TABLE IF EXISTS "workflow_node_type" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_node_type" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "category" VARCHAR(50) NOT NULL,
    "code" VARCHAR(50) NOT NULL,
    "created_at" TIMESTAMP NOT NULL,
    "default_config" VARCHAR(4000) DEFAULT NULL,
    "description" VARCHAR(500) DEFAULT NULL,
    "enabled" NUMBER(1) NOT NULL,
    "icon" VARCHAR(255) DEFAULT NULL,
    "input_ports" VARCHAR(4000) DEFAULT NULL,
    "name" VARCHAR(100) NOT NULL,
    "output_ports" VARCHAR(4000) DEFAULT NULL,
    "sort_order" INT NOT NULL,
    "updated_at" TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX "uk_workflow_node_type_code" ON "workflow_node_type" ("code");

-- =============================================
-- Table: workflow_variable_type
-- =============================================
DROP TABLE IF EXISTS "workflow_variable_type" CASCADE CONSTRAINTS;
CREATE TABLE "workflow_variable_type" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "code" VARCHAR(100) NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    "category" VARCHAR(50) NOT NULL,
    "element_type" VARCHAR(50) DEFAULT NULL,
    "file_type" VARCHAR(50) DEFAULT NULL,
    "dictionary_type" VARCHAR(100) DEFAULT NULL,
    "description" VARCHAR(500) DEFAULT NULL,
    "sort_order" INT DEFAULT 0,
    "enabled" NUMBER(1) DEFAULT 1,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX "uk_workflow_variable_type_code" ON "workflow_variable_type" ("code");
CREATE TRIGGER "trg_workflow_variable_type_updated_at" BEFORE UPDATE ON "workflow_variable_type" FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
COMMENT ON TABLE "workflow_variable_type" IS '工作流变量类型表';
COMMENT ON COLUMN "workflow_variable_type"."code" IS '变量类型编码';
COMMENT ON COLUMN "workflow_variable_type"."name" IS '变量类型名称';
COMMENT ON COLUMN "workflow_variable_type"."category" IS '分类 (BASIC/COMPOSITE)';
COMMENT ON COLUMN "workflow_variable_type"."element_type" IS '元素类型（用于数组类型）';
COMMENT ON COLUMN "workflow_variable_type"."file_type" IS '文件类型（用于文件类型）';
COMMENT ON COLUMN "workflow_variable_type"."dictionary_type" IS '数据字典类型（用于Dictionary类型）';
COMMENT ON COLUMN "workflow_variable_type"."description" IS '描述';
COMMENT ON COLUMN "workflow_variable_type"."sort_order" IS '排序顺序';
COMMENT ON COLUMN "workflow_variable_type"."enabled" IS '是否启用';
COMMENT ON COLUMN "workflow_variable_type"."created_at" IS '创建时间';
COMMENT ON COLUMN "workflow_variable_type"."updated_at" IS '更新时间';

-- =============================================
-- Foreign Key Constraints (created after all tables)
-- =============================================
ALTER TABLE "chat_feedback" ADD CONSTRAINT "fk_chat_feedback_message" FOREIGN KEY ("message_id") REFERENCES "chat_message" ("id");
ALTER TABLE "chat_message" ADD CONSTRAINT "fk_chat_message_conversation" FOREIGN KEY ("conversation_id") REFERENCES "chat_conversation" ("id");
ALTER TABLE "dictionary_column" ADD CONSTRAINT "fk_dictionary_column_dictionary" FOREIGN KEY ("dictionary_id") REFERENCES "data_dictionary" ("id") ON DELETE CASCADE CONSTRAINTS;
ALTER TABLE "skill_access_control" ADD CONSTRAINT "fk_skill_access_control_skill" FOREIGN KEY ("skill_id") REFERENCES "skill" ("id") ON DELETE CASCADE CONSTRAINTS;
ALTER TABLE "execution_log" ADD CONSTRAINT "fk_execution_log_workflow_execution" FOREIGN KEY ("execution_id") REFERENCES "workflow_execution" ("id") ON DELETE CASCADE CONSTRAINTS;
ALTER TABLE "workflow_node_execution" ADD CONSTRAINT "fk_workflow_node_execution_workflow_execution" FOREIGN KEY ("execution_id") REFERENCES "workflow_execution" ("id") ON DELETE CASCADE CONSTRAINTS;
