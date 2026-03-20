---
name: build-package
description: 构建和打包 Maven 项目
version: 1.0.0
---

# Build Package

构建并打包 Maven 项目。执行 `mvn clean package` 并将生成的 JAR 文件复制到 releases 目录。

## 用法

```
/build-package          # 构建并打包（跳过测试）
/build-package test     # 构建并打包（包含测试）
```

## 执行的操作

1. 创建 releases 目录
2. 执行 `mvn clean` 清理项目
3. 执行 `mvn compile` 编译项目
4. 可选执行 `mvn test` 运行测试
5. 执行 `mvn package` 打包项目
6. 复制 JAR 文件到 releases 目录

## 输出

JAR 文件将输出到：
- `releases/demo-{version}.jar` - 可执行 JAR
- `releases/demo-{version}-original.jar` - 原始 JAR
