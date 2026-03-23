# 工作流编排功能分析文档

> 本文档基于前端项目的代码分析，记录工作流编排功能的设计与实现。

## 一、概述

工作流编排系统是一个**可视化的AI应用测试编排框架**，核心目标是为生成式AI应用的推理特性测试提供可视化配置能力。

### 核心特性

- **可视化编辑器**：基于Canvas的拖拽式工作流编辑
- **节点式编排**：通过拖拽节点构建测试流程
- **循环体支持**：支持循环嵌套，每个循环可包含独立的工作流
- **多模态数据处理**：支持文本、图像、音频、视频等多种数据类型
- **插件化架构**：节点功能通过插件实现，易于扩展
- **实时预览**：工作流可即时运行并查看结果

---

## 二、工作流组成元素

### 2.1 节点类型

工作流由多种类型的节点组成，按功能分类如下：

| 分类 | 节点类型 | 说明 |
|------|----------|------|
| **基础节点** | `start` | 开始节点，工作流入口 |
| | `end` | 结束节点，工作流出口 |
| | `loopBodyCanvas` | 循环体容器节点 |
| **逻辑控制** | `condition` | 条件判断节点 |
| | `loop` | 循环控制节点 |
| **数据准备** | `envConnect` | 环境对接节点 |
| | `tableExtract` | 表格提取节点 |
| **文本处理** | `textClean` | 文本清洗 |
| | `textDedupe` | 文本去重 |
| | `textGeneralize` | 文本泛化 |
| | `textGenerate` | 文本生成 |
| **图像处理** | `imageGenerate` | 图像生成 |
| | `imageCutout` | 抠图 |
| | `imageEnhance` | 画质提升 |
| **音视频处理** | `videoExtractAudio` | 视频提取音频 |
| | `audioToText` | 音频转文本 |
| | `videoFrame` | 视频抽帧 |
| **测试设计** | `testPlan` | 测试方案生成 |
| **测试执行** | `apiAuto` | HTTPS/HTTP接口调用 |
| | `aiAuto` | AI自动化执行 |
| **结果评估** | `judgeModel` | 裁判模型评估 |
| | `firstTokenLatency` | 首Token响应时延 |
| | `tokenOutputTime` | 每Token输出耗时 |
| | `e2eLatency` | 端到端时延 |
| **报告生成** | `reportGenerate` | 生成测试报告 |
| | `reportAnalysis` | 报告分析 |

### 2.2 节点数据结构

```javascript
{
  id: 'node-uuid',           // 节点唯一标识
  type: 'node-type',         // 节点类型
  name: '节点名称',           // 显示名称
  x: 100,                    // 画布X坐标
  y: 200,                    // 画布Y坐标
  inputs: [                  // 输入端口列表
    { id: 'in-1', name: '输入1' }
  ],
  outputs: [                 // 输出端口列表
    { id: 'out-1', name: '输出1' }
  ],
  inputParams: [],           // 输入参数定义
  outputParams: [],          // 输出参数定义
  config: {}                 // 节点配置参数
}
```

### 2.3 连线系统

连线定义节点间的数据流向：

```javascript
{
  id: 'connection-uuid',
  sourceNodeId: 'node-1',
  sourcePortId: 'out-1',
  targetNodeId: 'node-2',
  targetPortId: 'in-1',
  sourceParamIndex: 0,       // 源参数索引
  targetParamIndex: 0        // 目标参数索引
}
```

### 2.4 循环体机制

循环体是工作流中的特殊结构，用于重复执行一组操作：

- **循环节点（loop）**：控制循环次数和条件
- **循环体画布（loopBodyCanvas）**：独立的子工作流空间
- **关联线**：连接循环节点与循环体的虚线
- **端口简化**：循环体只保留输入/输出端口，内部节点参数隐藏

---

## 三、前端架构

### 3.1 视图结构

```
src/views/workflow/
├── WorkflowView.vue              # 工作流列表页
├── WorkflowEditorView.vue        # 工作流编辑器（核心）
└── components/
    ├── LoopBodyCanvas.vue        # 循环体画布组件
    └── LoopBodyPort.vue          # 循环体端口组件
└── composables/
    ├── useAssociations.js        # 关联线逻辑
    └── useLoopBody.js            # 循环体逻辑
```

### 3.2 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│                        工具栏                                │
├──────────┬──────────────────────────────────┬───────────────┤
│          │                                  │               │
│  节点面板  │           画布区域               │   属性面板     │
│          │                                  │               │
│ - 基础    │    [开始]──>[处理]──>[结束]       │   节点名称     │
│ - 数据准备 │         │                        │   参数配置     │
│ - 测试执行 │         v                        │   输入输出     │
│ - 结果评估 │    [循环节点]──>[判断]            │               │
│ - 报告    │                                  │               │
│          │                                  │               │
└──────────┴──────────────────────────────────┴───────────────┘
```

### 3.3 交互功能

- **拖拽添加节点**：从左侧面板拖拽节点到画布
- **连线操作**：从输出端口拖拽到输入端口创建连线
- **节点配置**：选中节点后在右侧面板配置参数
- **画布操作**：支持缩放、平移、全屏
- **循环体编辑**：双击循环节点进入子画布

---

## 四、数据模型

### 4.1 工作流整体结构

```javascript
{
  // 工作流基本信息
  workflow: {
    id: 'workflow-uuid',
    name: '工作流名称',
    description: '工作流描述',
    published: false,        // 是否已发布
    hasRun: false           // 是否已运行
  },

  // 节点列表
  nodes: [...],

  // 连线列表
  connections: [...],

  // 关联线列表（循环节点与循环体）
  associations: [...]
}
```

### 4.2 后端Agent执行接口

工作流执行通过后端Agent框架实现：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/agent/execute` | POST | 简单任务执行 |
| `/api/agent/execute/config` | POST | 带配置的任务执行 |
| `/api/agent/execute/full` | POST | 完整请求体执行 |
| `/api/agent/execute/skill` | POST | 带Skill文件的执行 |

---

## 五、用户交互流程

### 5.1 工作流创建流程

```
1. 进入工作流管理页面
       ↓
2. 点击"新建工作流"
       ↓
3. 在编辑器中设计工作流：
   - 拖拽节点到画布
   - 连接节点建立数据流
   - 配置节点参数
   - 添加循环体（如需要）
       ↓
4. 保存工作流
       ↓
5. 发布工作流（可选）
```

### 5.2 工作流执行流程

```
1. 选择工作流
       ↓
2. 点击"运行"
       ↓
3. 前端序列化工作流为Agent任务
       ↓
4. 调用后端 /api/agent/execute
       ↓
5. 后端执行任务
       ↓
6. 返回执行结果
       ↓
7. 前端展示结果
```

### 5.3 循环体使用流程

```
1. 添加"循环"节点到画布
       ↓
2. 双击循环节点创建循环体
       ↓
3. 在循环体画布中添加处理节点
       ↓
4. 连接循环体输入/输出端口
       ↓
5. 配置循环参数（次数/条件）
       ↓
6. 保存
```

---

## 六、技术实现亮点

### 6.1 可视化编辑

- 使用Canvas实现拖拽、连线等交互
- 支持画布缩放和平移
- 实时渲染节点和连线
- 响应式布局适配

### 6.2 循环体嵌套

- 递归支持多层循环嵌套
- 循环体作为独立子画布管理
- 虚线关联表示层级关系
- 端口自动映射

### 6.3 状态管理

- 使用Vue 3 Composition API
- 响应式状态同步
- 模块化逻辑抽离（composables）
- 本地状态与远程同步

### 6.4 扩展性设计

- 节点类型可配置
- 插件化架构
- 支持自定义节点
- 参数动态绑定

---

## 七、后端对接建议

基于前端工作流的设计，后端需要提供以下能力：

### 7.1 工作流管理API

| 功能 | 接口 | 方法 |
|------|------|------|
| 创建工作流 | `/api/workflow` | POST |
| 获取工作流列表 | `/api/workflow` | GET |
| 获取工作流详情 | `/api/workflow/{id}` | GET |
| 更新工作流 | `/api/workflow/{id}` | PUT |
| 删除工作流 | `/api/workflow/{id}` | DELETE |
| 复制工作流 | `/api/workflow/{id}/copy` | POST |
| 发布工作流 | `/api/workflow/{id}/publish` | POST |

### 7.2 工作流执行API

| 功能 | 接口 | 方法 |
|------|------|------|
| 执行工作流 | `/api/workflow/{id}/execute` | POST |
| 获取执行状态 | `/api/workflow/execution/{id}` | GET |
| 中止执行 | `/api/workflow/execution/{id}/abort` | POST |
| 获取执行结果 | `/api/workflow/execution/{id}/result` | GET |

### 7.3 数据模型建议

```java
// 工作流实体
@Entity
public class Workflow {
    private String id;
    private String name;
    private String description;
    private Boolean published;
    private String nodes;       // JSON
    private String connections; // JSON
    private String associations;// JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// 工作流执行记录
@Entity
public class WorkflowExecution {
    private String id;
    private String workflowId;
    private ExecutionStatus status;
    private String result;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

---

## 八、总结

该工作流编排系统是一个功能完整的AI应用测试可视化编排平台，具有以下优势：

1. **直观易用**：可视化拖拽界面，降低使用门槛
2. **功能强大**：支持复杂测试流程编排和多模态数据处理
3. **灵活扩展**：插件化架构，易于添加新功能
4. **完整闭环**：从设计、执行到结果展示的完整流程

后端在实现时需要关注：
- 工作流的持久化存储
- 节点类型的可扩展性
- 执行引擎的设计
- 与现有Agent框架的集成
