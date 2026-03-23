# AI智能助手功能分析文档

> 本文档基于前端项目 `ai-application-test-frontend` 的代码分析，记录AI智能助手功能的设计与实现。

## 一、概述

AI智能助手是一个**全局浮动的对话式交互组件**，为用户提供智能化的帮助服务。该组件目前采用前端模拟实现，预留了后端API对接接口。

### 核心特性

- **全局悬浮窗口**：基于Teleport实现，固定在页面底部
- **实时对话交互**：支持用户输入和AI回复的对话模式
- **快捷问题预设**：提供常用问题的快速入口
- **拖拽调整高度**：支持鼠标拖拽调整聊天框高度（300px-700px）
- **打字动画效果**：AI回复时显示打字指示器
- **消息历史管理**：记录当前会话的对话历史
- **键盘快捷键**：Enter发送、ESC关闭

---

## 二、前端架构

### 2.1 组件结构

```
src/components/chat/
└── AiChat.vue              # 主聊天组件（910行）

src/components/layout/
└── AppSidebar.vue          # 侧边栏（集成AI助手入口）

src/views/workflow/
└── WorkflowEditorView.vue  # 工作流编辑器（独立AI面板）
```

### 2.2 组件层级关系

```
App.vue
└── AppSidebar.vue
    ├── 导航菜单
    ├── AI助手按钮（侧边栏底部）
    └── AiChat.vue（通过v-model:visible控制显示）
```

### 2.3 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│                        侧边栏 (220px)                        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  导航菜单项...                                           ││
│  │                                                          ││
│  │  ┌─────────────────────────────────────────────────────┐││
│  │  │  🤖 AI助手  (点击打开聊天框)                          │││
│  │  └─────────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘

                    AI聊天框（打开后）
┌─────────────────────────────────────────────────────────────┐
│ ════════════════════ 拖拽调整手柄 ════════════════════════ │
├─────────────────────────────────────────────────────────────┤
│  🤖 AI 智能助手                    🗑️清空  ✕关闭            │
│  ● 在线 · 随时为您服务                                       │
├─────────────────────────────────────────────────────────────┤
│  💡如何创建测评集？  🔧环境管理配置？  📊测试报告？  🚀入门   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🤖 你好！我是AI助手，有什么可以帮助你的吗？     17:30       │
│                                                             │
│                      我要创建一个测评集  👤     17:31       │
│                                                             │
│  🤖 这是一个很好的问题！让我来帮你分析一下...   17:31       │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  📝 输入消息，按 Enter 发送...              [ 发送 ]       │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、核心代码分析

### 3.1 组件Props与Events

```javascript
// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false,  // 控制聊天框显示/隐藏
  },
})

// Events
const emit = defineEmits(['update:visible', 'close'])
```

### 3.2 状态管理

```javascript
// 主要状态变量
const inputMessage = ref('')           // 输入框内容
const messages = ref([...])            // 消息列表
const isTyping = ref(false)            // AI是否正在输入
const chatHeight = ref(520)            // 聊天框高度
const showScrollButton = ref(false)    // 是否显示滚动按钮

// 消息结构
{
  id: 1,                              // 消息ID
  type: 'ai' | 'user',               // 消息类型
  content: '消息内容',                // 消息内容
  time: new Date(),                  // 时间戳
}
```

### 3.3 预设快捷问题

```javascript
const quickQuestions = [
  { icon: '💡', text: '如何创建测评集？' },
  { icon: '🔧', text: '环境管理怎么配置？' },
  { icon: '📊', text: '如何查看测试报告？' },
  { icon: '🚀', text: '快速入门指南' },
]
```

### 3.4 消息发送流程（当前模拟实现）

```javascript
const sendMessage = async (content = null) => {
  const messageContent = content || inputMessage.value.trim()
  if (!messageContent) return

  // 1. 添加用户消息
  const userMessage = {
    id: Date.now(),
    type: 'user',
    content: messageContent,
    time: new Date(),
  }
  messages.value.push(userMessage)
  inputMessage.value = ''

  // 2. 滚动到底部
  await scrollToBottom()

  // 3. 显示AI正在输入
  isTyping.value = true

  // 4. 模拟AI回复（1-2秒延迟）
  setTimeout(async () => {
    isTyping.value = false
    const aiMessage = {
      id: Date.now() + 1,
      type: 'ai',
      content: getRandomReply(),  // 从预设数组随机选择
      time: new Date(),
    }
    messages.value.push(aiMessage)
    await scrollToBottom()
  }, 1000 + Math.random() * 1000)
}
```

### 3.5 拖拽调整高度

```javascript
const minHeight = 300
const maxHeight = 700
const isDragging = ref(false)

const startDrag = (e) => {
  isDragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'ns-resize'
}

const onDrag = (e) => {
  if (!isDragging.value) return
  const windowHeight = window.innerHeight
  const newHeight = windowHeight - e.clientY - 24
  if (newHeight >= minHeight && newHeight <= maxHeight) {
    chatHeight.value = newHeight
  }
}
```

---

## 四、技术实现亮点

### 4.1 视觉设计

- **毛玻璃效果**：使用 `backdrop-filter: blur(20px)` 实现
- **渐变色主题**：头部采用紫色渐变 (`#6366f1` → `#8b5cf6` → `#a855f7`)
- **微交互动画**：按钮悬停、消息气泡、打字指示器等
- **状态指示**：在线状态的呼吸灯效果

### 4.2 动画效果

```css
/* 聊天框滑入动画 */
@keyframes chat-slide-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 打字指示器动画 */
@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

/* 状态点呼吸效果 */
@keyframes pulse-dot {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
}
```

### 4.3 无障碍支持

```css
/* 减少动画（用户偏好） */
@media (prefers-reduced-motion: reduce) {
  .chat-slide-enter-active,
  .chat-slide-leave-active {
    animation: none;
  }
  .typing-dot,
  .avatar-glow,
  .status-dot {
    animation: none;
  }
}
```

### 4.4 响应式设计

```css
@media (max-width: 768px) {
  .ai-chat-container {
    left: 16px;
    right: 16px;
    bottom: 16px;
    border-radius: 16px;
  }
}
```

---

## 五、工作流编辑器中的AI助手

在工作流编辑器页面（`WorkflowEditorView.vue`）中，有一个独立的AI助手面板实现：

### 5.1 面板特点

- 位于右侧属性面板底部
- 可展开/折叠
- 独立的消息状态管理
- 与主AI聊天组件功能相同

### 5.2 相关代码

```javascript
// 工作流编辑器中的AI助手状态
const aiChatExpanded = ref(false)
const aiChatInput = ref('')
const aiChatMessages = ref([
  { id: 1, type: 'ai', content: '你好！我是AI助手，有什么可以帮助你的吗？', time: new Date() }
])
const aiChatIsTyping = ref(false)

// 切换AI聊天框展开/折叠
const toggleAiChat = () => {
  aiChatExpanded.value = !aiChatExpanded.value
}
```

---

## 六、后端API对接建议

### 6.1 当前状态

**重要**：当前AI助手组件是**前端模拟实现**，使用预设回复数组模拟AI响应，并未与后端API集成。

```javascript
// 当前使用的预设回复
const aiReplies = [
  '这是一个很好的问题！让我来帮你分析一下。',
  '我理解你的需求。根据你的描述，我建议...',
  '好的，我已经收到你的消息了。请问还有什么需要补充的吗？',
  // ...
]
```

### 6.2 建议的后端API设计

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 发送消息 | `/api/chat/send` | POST | 发送用户消息并获取AI回复 |
| 获取历史 | `/api/chat/history` | GET | 获取对话历史记录 |
| 清空对话 | `/api/chat/clear` | POST | 清空当前会话对话 |
| 流式回复 | `/api/chat/stream` | POST(SSE) | 流式返回AI回复 |

### 6.3 API请求/响应示例

#### 发送消息

**请求**：
```json
POST /api/chat/send
{
  "message": "如何创建测评集？",
  "conversationId": "conv-uuid-123",
  "context": {
    "currentPage": "/evaluation",
    "userId": "user-001"
  }
}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "messageId": "msg-uuid-456",
    "content": "创建测评集的步骤如下：\n1. 进入测评集管理页面\n2. 点击"新建测评集"按钮\n3. 填写测评集基本信息...",
    "conversationId": "conv-uuid-123",
    "timestamp": "2026-03-20T17:30:00"
  }
}
```

### 6.4 前端集成修改点

在 `sendMessage` 函数中替换模拟逻辑：

```javascript
// 原代码（模拟）
setTimeout(async () => {
  isTyping.value = false
  const aiMessage = {
    id: Date.now() + 1,
    type: 'ai',
    content: getRandomReply(),
    time: new Date(),
  }
  messages.value.push(aiMessage)
}, 1000 + Math.random() * 1000)

// 改为真实API调用
const response = await fetch('/api/chat/send', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    message: messageContent,
    conversationId: currentConversationId.value,
  })
})
const data = await response.json()
isTyping.value = false
const aiMessage = {
  id: data.data.messageId,
  type: 'ai',
  content: data.data.content,
  time: new Date(data.data.timestamp),
}
messages.value.push(aiMessage)
```

---

## 七、扩展功能建议

### 7.1 短期增强

1. **对话持久化**：使用localStorage或后端存储对话历史
2. **上下文记忆**：实现多轮对话的上下文保持
3. **错误处理**：添加网络错误、超时等异常处理
4. **重试机制**：支持消息发送失败后重试

### 7.2 中期增强

1. **流式输出**：使用SSE实现打字机效果的实时输出
2. **Markdown渲染**：支持AI回复中的Markdown格式
3. **代码高亮**：支持代码块的语法高亮
4. **文件上传**：支持发送文件给AI分析

### 7.3 长期增强

1. **语音输入**：集成Web Speech API
2. **多语言支持**：支持中英文等多语言交互
3. **智能推荐**：根据当前页面动态推荐问题
4. **对话分享**：支持导出和分享对话记录

---

## 八、数据库设计建议

### 8.1 对话表

```sql
CREATE TABLE `chat_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `conversation_uuid` VARCHAR(36) NOT NULL COMMENT '对话UUID',
  `user_id` VARCHAR(64) COMMENT '用户ID',
  `title` VARCHAR(200) COMMENT '对话标题',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_conversation_uuid` (`conversation_uuid`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话表';
```

### 8.2 消息表

```sql
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL COMMENT '对话ID',
  `message_uuid` VARCHAR(36) NOT NULL COMMENT '消息UUID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色(user/assistant)',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `tokens` INT DEFAULT 0 COMMENT 'Token数量',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_conversation_id` (`conversation_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';
```

---

## 九、总结

当前AI智能助手是一个功能完整的UI组件，具备以下特点：

| 方面 | 状态 | 说明 |
|------|------|------|
| UI界面 | ✅ 完整 | 现代化设计，动画流畅 |
| 交互功能 | ✅ 完整 | 支持拖拽、快捷键、滚动等 |
| API对接 | ⏳ 待实现 | 当前使用模拟数据 |
| 数据持久化 | ⏳ 待实现 | 刷新后历史丢失 |
| 上下文管理 | ⏳ 待实现 | 无多轮对话支持 |

下一步工作重点是实现后端API对接，将前端模拟逻辑替换为实际的AI服务调用。

---

*文档版本：1.0*
*创建时间：2026-03-20*
*基于项目：ai-application-test-frontend*
