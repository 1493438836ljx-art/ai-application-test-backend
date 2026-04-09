# 外键约束移除与应用层校验方案

## 变更背景

当前 `schema_open_gauss.sql` 中定义了 6 条外键约束。为保证数据库操作灵活性、提升批量写入性能、简化数据迁移流程，决定移除数据库层面的外键约束，改为在应用层（Service）通过代码逻辑保证数据一致性。

## 需要移除的外键约束

| # | 子表 | 外键字段 | 父表 | 父表字段 | 级联删除 |
|---|------|---------|------|---------|---------|
| 1 | chat_feedback | message_id | chat_message | id | 否 |
| 2 | chat_message | conversation_id | chat_conversation | id | 否 |
| 3 | dictionary_column | dictionary_id | data_dictionary | id | 是 |
| 4 | skill_access_control | skill_id | skill | id | 是 |
| 5 | execution_log | execution_id | workflow_execution | id | 是 |
| 6 | workflow_node_execution | execution_id | workflow_execution | id | 是 |

## 变更内容

### 1. 修改 schema_open_gauss.sql

移除文件末尾的 6 条 `ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY` 语句，保留 `COMMENT ON COLUMN` 中对关联关系的文字说明。

需要移除的 SQL（文件第 729-734 行）：

```sql
ALTER TABLE "chat_feedback" ADD CONSTRAINT "fk_chat_feedback_message" FOREIGN KEY ("message_id") REFERENCES "chat_message" ("id");
ALTER TABLE "chat_message" ADD CONSTRAINT "fk_chat_message_conversation" FOREIGN KEY ("conversation_id") REFERENCES "chat_conversation" ("id");
ALTER TABLE "dictionary_column" ADD CONSTRAINT "fk_dictionary_column_dictionary" FOREIGN KEY ("dictionary_id") REFERENCES "data_dictionary" ("id") ON DELETE CASCADE CONSTRAINTS;
ALTER TABLE "skill_access_control" ADD CONSTRAINT "fk_skill_access_control_skill" FOREIGN KEY ("skill_id") REFERENCES "skill" ("id") ON DELETE CASCADE CONSTRAINTS;
ALTER TABLE "execution_log" ADD CONSTRAINT "fk_execution_log_workflow_execution" FOREIGN KEY ("execution_id") REFERENCES "workflow_execution" ("id") ON DELETE CASCADE CONSTRAINTS;
ALTER TABLE "workflow_node_execution" ADD CONSTRAINT "fk_workflow_node_execution_workflow_execution" FOREIGN KEY ("execution_id") REFERENCES "workflow_execution" ("id") ON DELETE CASCADE CONSTRAINTS;
```

### 2. 应用层校验设计

以下为每条外键对应的应用层校验逻辑，需在各 Service 类中实现：

#### 2.1 chat_feedback.message_id → chat_message.id

- **校验位置**: `ChatFeedbackService` 创建反馈时
- **校验逻辑**: 插入前通过 `chatMessageMapper.selectById(messageId)` 检查消息是否存在，不存在则抛出业务异常
- **删除联动**: 删除 chat_message 时，需同步删除关联的 chat_feedback 记录

#### 2.2 chat_message.conversation_id → chat_conversation.id

- **校验位置**: `ChatMessageService` 发送消息时
- **校验逻辑**: 插入前通过 `chatConversationMapper.selectById(conversationId)` 检查会话是否存在，不存在则抛出业务异常
- **删除联动**: 删除 chat_conversation 时，需同步删除关联的 chat_message 及其 chat_feedback 记录

#### 2.3 dictionary_column.dictionary_id → data_dictionary.id

- **校验位置**: `DictionaryColumnService` 创建/更新字段时
- **校验逻辑**: 插入前通过 `dataDictionaryMapper.selectById(dictionaryId)` 检查字典是否存在，不存在则抛出业务异常
- **删除联动**: 删除 data_dictionary 时，需同步删除关联的 dictionary_column 记录（原 CASCADE 语义）

#### 2.4 skill_access_control.skill_id → skill.id

- **校验位置**: `SkillAccessControlService` 创建访问控制时
- **校验逻辑**: 插入前通过 `skillMapper.selectById(skillId)` 检查 Skill 是否存在，不存在则抛出业务异常
- **删除联动**: 删除 skill 时，需同步删除关联的 skill_access_control 记录（原 CASCADE 语义）

#### 2.5 execution_log.execution_id → workflow_execution.id

- **校验位置**: `ExecutionLogService` 记录日志时
- **校验逻辑**: 插入前通过 `workflowExecutionMapper.selectById(executionId)` 检查执行记录是否存在，不存在则抛出业务异常
- **删除联动**: 删除 workflow_execution 时，需同步删除关联的 execution_log 记录（原 CASCADE 语义）

#### 2.6 workflow_node_execution.execution_id → workflow_execution.id

- **校验位置**: `WorkflowNodeExecutionService` 记录节点执行时
- **校验逻辑**: 插入前通过 `workflowExecutionMapper.selectById(executionId)` 检查执行记录是否存在，不存在则抛出业务异常
- **删除联动**: 删除 workflow_execution 时，需同步删除关联的 workflow_node_execution 记录（原 CASCADE 语义）

## 变更影响

- **schema_open_gauss.sql**: 移除 6 条外键约束 SQL
- **Java Service 层**: 在涉及上述表的新增和删除操作中增加校验和级联删除逻辑
- **数据一致性**: 由数据库强制保证变为应用层保证，需确保 Service 层逻辑覆盖所有入口（包括批量操作）
