# Agent 框架使用文档

## 概述

Agent 框架是对 Claude Code RESTful API 的 Java 封装，提供了统一的调用接口，支持：

1. 包装 Claude Code API 的 HTTP 请求
2. 提供统一的 Agent 调用接口
3. 支持 RESTful API 调用
4. 支持 Java 接口直接调用

## 架构说明

```
agent/
├── config/                 # 配置类
│   └── ClaudeCodeProperties.java
├── dto/                    # 数据传输对象
│   ├── AgentConfig.java
│   ├── AgentRequest.java
│   ├── AgentResponse.java
│   ├── HealthCheckResponse.java
│   └── TaskExecuteResponse.java
├── client/                 # HTTP 客户端
│   └── ClaudeCodeApiClient.java
├── framework/              # 框架核心
│   └── AgentExecutor.java
├── controller/             # RESTful API
│   └── AgentController.java
├── api/                    # 统一接口
│   └── AgentService.java
└── impl/                   # 接口实现
    └── AgentServiceImpl.java
```

## 配置

在 `application.yml` 中配置 Claude Code API 连接信息：

```yaml
claude:
  code:
    base-url: http://localhost:3000  # Claude Code API 地址
    connect-timeout: 10              # 连接超时（秒）
    read-timeout: 130                # 读取超时（秒）
    enabled: true                    # 是否启用
    show-danger-mode-warning: true   # 是否显示警告
```

## 使用方式

### 方式一：通过 RESTful API 调用

#### 1. 健康检查

```bash
GET http://localhost:8080/api/agent/health
```

#### 2. 简单执行

```bash
POST http://localhost:8080/api/agent/execute
Content-Type: application/x-www-form-urlencoded

taskContent=列出当前目录文件
```

#### 3. 带配置执行

```bash
POST http://localhost:8080/api/agent/execute/config
Content-Type: application/x-www-form-urlencoded

taskContent=创建一个hello.txt文件&timeout=60&debug=false
```

#### 4. 完整请求执行

```bash
POST http://localhost:8080/api/agent/execute/full
Content-Type: application/json

{
  "taskContent": "创建一个文件",
  "config": {
    "timeout": 60,
    "debug": false
  }
}
```

#### 5. 带 Skill 文件执行

```bash
POST http://localhost:8080/api/agent/execute/skill
Content-Type: multipart/form-data

taskContent=使用自定义Skill执行任务
timeout=120
debug=false
skillFile=@custom-skill.zip
```

### 方式二：通过 Java 接口直接调用

```java
@Autowired
private AgentService agentService;

// 1. 简单调用
AgentResponse response = agentService.execute("列出当前目录文件");

// 2. 带配置调用
AgentResponse response = agentService.execute("创建文件", 60, false);

// 3. 完整请求调用
AgentRequest request = AgentRequest.builder()
    .taskContent("执行任务")
    .config(AgentConfig.builder().timeout(120).debug(true).build())
    .build();
AgentResponse response = agentService.execute(request);

// 4. 带回调调用
AgentResponse response = agentService.execute(request, new AgentCallback() {
    @Override
    public void beforeExecute(AgentRequest req) {
        log.info("开始执行: {}", req.getTaskContent());
    }

    @Override
    public void afterExecute(AgentRequest req, AgentResponse res) {
        log.info("执行完成: {}", res.getSuccess());
    }

    @Override
    public void onError(AgentRequest req, AgentResponse res, Exception e) {
        log.error("执行异常: {}", e.getMessage());
    }
});

// 5. 异步调用
agentService.executeAsync(request, callback);

// 6. 带 Skill 文件调用
byte[] skillBytes = Files.readAllBytes(Paths.get("custom-skill.zip"));
AgentResponse response = agentService.executeWithSkill(
    "使用自定义Skill",
    AgentConfig.builder().timeout(120).build(),
    skillBytes,
    "custom-skill.zip"
);

// 7. 健康检查
boolean healthy = agentService.checkHealth();
```

### 方式三：直接注入 AgentExecutor

```java
@Autowired
private AgentExecutor agentExecutor;

// 使用 AgentExecutor 执行
AgentResponse response = agentExecutor.execute(request);
```

## 数据模型

### AgentRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskContent | String | 是 | 任务内容 |
| config | AgentConfig | 否 | Agent 配置 |
| skillFileBytes | byte[] | 否 | Skill 文件字节数组 |
| skillFileName | String | 否 | Skill 文件名 |

### AgentConfig

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| timeout | Integer | 否 | 120 | 超时时间（秒） |
| debug | Boolean | 否 | false | 是否开启调试 |
| extraParams | Map<String, Object> | 否 | null | 自定义参数 |

### AgentResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| success | Boolean | 执行是否成功 |
| response | String | Claude 响应内容 |
| error | String | 错误信息 |
| errorCode | Integer | 错误码 |
| originalTaskContent | String | 原始任务内容 |
| executionTimeMs | Long | 执行耗时（毫秒） |

## Postman 导入

```json
{
  "info": {
    "name": "Agent Framework API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    }
  ],
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/agent/health"
      }
    },
    {
      "name": "Execute Simple",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/agent/execute",
        "body": {
          "mode": "urlencoded",
          "urlencoded": [
            {
              "key": "taskContent",
              "value": "列出当前目录文件"
            }
          ]
        }
      }
    },
    {
      "name": "Execute With Config",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/agent/execute/config",
        "body": {
          "mode": "urlencoded",
          "urlencoded": [
            {
              "key": "taskContent",
              "value": "创建一个文件"
            },
            {
              "key": "timeout",
              "value": "60"
            }
          ]
        }
      }
    },
    {
      "name": "Execute Full",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/agent/execute/full",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"taskContent\": \"创建一个文件\",\n  \"config\": {\n    \"timeout\": 60,\n    \"debug\": false\n  }\n}"
        }
      }
    }
  ]
}
```

## 安全警告

⚠️ **危险模式警告**：此 Agent 框架通过调用 Claude Code API，使用 `--dangerously-skip-permissions` 标志，跳过所有权限检查，可以执行任意系统命令。请确保仅在内网或受控环境中使用！
