# Build Package Skill

用于构建和打包 Maven 项目的 Claude Code Skill。

## 功能

- 执行 `mvn clean` 清理项目
- 执行 `mvn compile` 编译项目
- 可选执行 `mvn test` 运行测试
- 执行 `mvn package` 打包项目
- 将生成的 JAR 文件移动到 `releases` 文件夹

## 使用方法

### 方式一：通过 Claude Code 直接调用

将此 skill 安装到 Claude Code 后，可以使用以下命令：

```bash
# 清理并打包（跳过测试）
/build-package

# 清理、编译、测试并打包
/build-package with-tests
```

### 方式二：打包为 ZIP 上传给 Agent

将整个 `build-package` 文件夹打包为 ZIP 文件，然后通过 Agent 的 RESTful API 上传：

```bash
POST http://localhost:8080/api/agent/execute/skill
Content-Type: multipart/form-data

taskContent=构建并打包当前项目&timeout=180&skillFile=@build-package.zip
```

### 方式三：直接执行脚本

```bash
# Linux/macOS
./skills/build-package/build.sh          # 跳过测试
./skills/build-package/build.sh false   # 包含测试

# Windows
skills\build-package\build.bat
```

## 目录结构

```
build-package/
├── skill.md       # Skill 描述文件
├── package.json   # Skill 元数据
├── build.sh       # Linux/macOS 构建脚本
├── build.bat      # Windows 构建脚本
└── README.md      # 说明文档
```

## 输出

构建成功后，JAR 文件位于：
- `releases/demo-{version}.jar` - 可执行的 JAR 文件
- `releases/demo-{version}-original.jar` - 原始 JAR 文件

## 打包为 ZIP

```bash
# Linux/macOS
cd skills
zip -r build-package.zip build-package/

# Windows (PowerShell)
Compress-Archive -Path skills\build-package\* -DestinationPath build-package.zip
```

## Java 代码中调用

```java
@Autowired
private AgentService agentService;

// 读取 skill 文件
byte[] skillBytes = Files.readAllBytes(Paths.get("skills/build-package.zip"));

// 执行构建
AgentResponse response = agentService.executeWithSkill(
    "构建并打包当前项目",
    AgentConfig.builder().timeout(180).build(),
    skillBytes,
    "build-package.zip"
);
```

## 要求

- Maven 3.x
- Java 17 或更高版本
- 网络连接（用于下载 Maven 依赖）
