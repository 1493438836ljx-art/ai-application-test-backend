# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介
这是一个基于 Spring Boot 3.2.3 构建的 RESTful API 项目，项目负责的业务为：支撑生成式AI应用/软件的推理特性的测试。
项目核心功能：
- 提供 RESTful API 接口，用于测试生成式AI应用/软件的推理特性。
- 测评集管理，包括创建/导入、查询、更新、删除测评集。
- Prompt管理，包括创建、查询、更新、删除Prompt。
- 环境管理，支持对接不同的生成式AI应用/软件环境。
- 插件管理，包括测试推理的执行插件、推理结果的评估插件。
- 测试执行管理，包括创建、执行、中止执行任务，以及查看测试任务进度，
- 测试结果管理，包括自动生成测试报告，以及查询、导出测试结果。

## 构建和运行命令

```bash
# 编译项目
./mvnw compile

# 运行应用
./mvnw spring-boot:run

# 运行所有测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=DemoApplicationTests

# 打包（跳过测试）
./mvnw package -DskipTests

# 清理并打包
./mvnw clean package
```

## 技术栈

- **Java 17** + **Spring Boot 3.2.3**
- **Maven** 构建工具
- **Spring Data JPA** + **Hibernate**
- **H2** 内存数据库（开发环境）/ **MySQL**（生产环境）
- **Lombok** 减少样板代码

## 项目架构

```
src/main/java/com/example/demo/
├── DemoApplication.java      # 主入口
├── controller/               # REST 控制器层
├── service/                  # 业务逻辑层（待创建）
├── repository/               # 数据访问层（待创建）
├── entity/                   # JPA 实体类（待创建）
└── dto/                      # 数据传输对象（待创建）
```

## 配置

- 应用端口：8080
- H2 控制台：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:mem:testdb`，用户名: `sa`，密码为空）
- API 基础路径：`/api/*`

## 数据库规范

**数据库对接的是 OpenGauss，数据库和代码中的 SQL 首先必须遵从 GaussDB100 语法。**

GaussDB100 使用 Oracle 风格语法，与标准 PostgreSQL 的主要语法差异：
- 自增列使用 `BIGINT AUTO_INCREMENT`，不支持 `BIGSERIAL`
- 不支持 `DEFAULT NULL`（NULL 为默认行为，无需显式声明）
- `DROP TABLE` 级联删除使用 `CASCADE CONSTRAINTS`，不支持 `CASCADE`
- 不支持 `TEXT` 类型，需使用 `VARCHAR(N)` 替代并限定长度
- `VARCHAR(N)` 的 N 最大值为 8000
- 禁止使用外键约束，数据一致性由应用层 Service 代码保证
- 唯一标识符统一使用 36 位 UUID（VARCHAR(36)），不使用自增整数
- 禁止在表名和列名上加双引号或引号（如 `"table_name"`、`"column_name"` 均不允许）

## 依赖说明

项目已配置 OpenGauss 驱动，生产环境在 `application.yml` 中配置数据库连接。

## Git 工作流

**重要：每次 `git push` 之前，必须先编译运行当前项目，确保代码可以正常构建和启动。**

**禁止提交的文件：**
- `src/main/resources/application.yml` - 包含数据库密码等敏感信息，禁止提交和推送。

```bash

```bash
# 推送前验证流程
./mvnw clean package -DskipTests   # 清理并打包
java -jar target/demo-0.0.1-SNAPSHOT.jar  # 启动验证
# 确认启动成功后，停止应用，再执行 git push
```
