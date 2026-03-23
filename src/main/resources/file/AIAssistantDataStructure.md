# AI智能助手数据库表结构设计

> 本文档定义AI智能助手功能的数据库表结构和API接口设计。

## 一、数据库表设计

### 1.1 chat_conversation（对话表）

存储用户与AI的对话会话。

```sql
CREATE TABLE `chat_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_uuid` VARCHAR(36) NOT NULL COMMENT '对话UUID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '对话标题（自动生成或用户设置）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-活跃, ARCHIVED-已归档, DELETED-已删除',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
  `metadata` JSON DEFAULT NULL COMMENT '元数据（上下文信息等）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_conversation_uuid` (`conversation_uuid`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_last_message_at` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话表';
```

### 1.2 chat_message（消息表）

存储对话中的每条消息。

```sql
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` BIGINT NOT NULL COMMENT '对话ID',
  `message_uuid` VARCHAR(36) NOT NULL COMMENT '消息UUID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户, assistant-AI助手, system-系统',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `content_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '内容类型：text-文本, markdown-Markdown',
  `tokens` INT DEFAULT 0 COMMENT 'Token数量',
  `model` VARCHAR(50) DEFAULT NULL COMMENT '使用的模型名称',
  `latency_ms` BIGINT DEFAULT NULL COMMENT '响应延迟（毫秒）',
  `metadata` JSON DEFAULT NULL COMMENT '元数据',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_message_uuid` (`message_uuid`),
  INDEX `idx_conversation_id` (`conversation_id`),
  INDEX `idx_role` (`role`),
  INDEX `idx_created_at` (`created_at`),
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`)
    REFERENCES `chat_conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';
```

### 1.3 chat_feedback（反馈表）

存储用户对AI回复的反馈。

```sql
CREATE TABLE `chat_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` BIGINT NOT NULL COMMENT '消息ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
  `rating` TINYINT NOT NULL COMMENT '评分：1-差, 2-一般, 3-好, 4-很好, 5-非常好',
  `feedback_type` VARCHAR(20) DEFAULT NULL COMMENT '反馈类型：helpful-有帮助, inaccurate-不准确, inappropriate-不当',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '反馈评论',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_message_id` (`message_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_rating` (`rating`),
  CONSTRAINT `fk_feedback_message` FOREIGN KEY (`message_id`)
    REFERENCES `chat_message` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI反馈表';
```

### 1.4 chat_quick_question（快捷问题表）

存储预设的快捷问题。

```sql
CREATE TABLE `chat_quick_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `icon` VARCHAR(10) DEFAULT NULL COMMENT '图标（emoji）',
  `text` VARCHAR(200) NOT NULL COMMENT '问题文本',
  `category` VARCHAR(50) DEFAULT 'general' COMMENT '分类',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_category` (`category`),
  INDEX `idx_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快捷问题表';
```

---

## 二、ER图

```
┌─────────────────────┐
│ chat_conversation   │
├─────────────────────┤
│ id (PK)             │
│ conversation_uuid   │
│ user_id             │
│ title               │
│ status              │
│ message_count       │
│ last_message_at     │
│ metadata (JSON)     │
└─────────┬───────────┘
          │ 1:N
          ▼
┌─────────────────────┐       ┌─────────────────────┐
│ chat_message        │       │ chat_feedback       │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ conversation_id(FK) │       │ message_id (FK)     │
│ message_uuid        │◄──────│ user_id             │
│ role                │  1:N  │ rating              │
│ content             │       │ feedback_type       │
│ content_type        │       │ comment             │
│ tokens              │       └─────────────────────┘
│ model               │
│ latency_ms          │
└─────────────────────┘

┌─────────────────────┐
│ chat_quick_question │
├─────────────────────┤
│ id (PK)             │
│ icon                │
│ text                │
│ category            │
│ sort_order          │
│ enabled             │
└─────────────────────┘
```

---

## 三、API接口设计

### 3.1 对话管理API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/conversations` | 创建新对话 |
| GET | `/api/chat/conversations` | 获取对话列表 |
| GET | `/api/chat/conversations/{uuid}` | 获取对话详情（含消息） |
| PUT | `/api/chat/conversations/{uuid}` | 更新对话信息 |
| DELETE | `/api/chat/conversations/{uuid}` | 删除对话 |
| POST | `/api/chat/conversations/{uuid}/archive` | 归档对话 |

### 3.2 消息API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/send` | 发送消息并获取AI回复 |
| GET | `/api/chat/conversations/{uuid}/messages` | 获取对话消息列表 |
| POST | `/api/chat/stream` | 流式发送消息（SSE） |

### 3.3 反馈API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/messages/{uuid}/feedback` | 提交消息反馈 |
| GET | `/api/chat/messages/{uuid}/feedback` | 获取消息反馈 |

### 3.4 快捷问题API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/quick-questions` | 获取快捷问题列表 |

---

## 四、请求/响应示例

### 4.1 发送消息

**请求**：
```json
POST /api/chat/send
{
  "conversationId": "conv-uuid-123",
  "message": "如何创建测评集？",
  "context": {
    "currentPage": "/evaluation"
  }
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "conv-uuid-123",
    "userMessage": {
      "id": 1,
      "messageUuid": "msg-uuid-user",
      "role": "user",
      "content": "如何创建测评集？",
      "createdAt": "2026-03-20T18:00:00"
    },
    "assistantMessage": {
      "id": 2,
      "messageUuid": "msg-uuid-assistant",
      "role": "assistant",
      "content": "创建测评集的步骤如下：\n1. 进入测评集管理页面\n2. 点击"新建测评集"按钮...",
      "contentType": "markdown",
      "createdAt": "2026-03-20T18:00:02"
    }
  }
}
```

### 4.2 获取对话列表

**请求**：
```
GET /api/chat/conversations?page=0&size=20&status=ACTIVE
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "conversationUuid": "conv-uuid-123",
        "title": "关于测评集的问题",
        "status": "ACTIVE",
        "messageCount": 10,
        "lastMessageAt": "2026-03-20T18:00:02",
        "createdAt": "2026-03-20T17:30:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

*文档版本：1.0*
*创建时间：2026-03-20*
