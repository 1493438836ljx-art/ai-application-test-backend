# 部署脚本使用说明

## 安装依赖

```bash
pip install paramiko scp
```

## 使用方法

```bash
python deploy.py --host <服务器地址> --user <用户名> --key <密钥路径>
```

## 参数说明

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| --host | 是 | - | 远程服务器地址 |
| --user | 是 | - | SSH 用户名 |
| --key | 是 | - | SSH 私钥文件路径 |
| --port | 否 | 22 | SSH 端口 |
| --deploy-path | 否 | /opt/ai-studio | 部署目录 |
| --java-opts | 否 | -Xmx512m | JVM 参数 |
| --skip-tests | 否 | True | 跳过测试 |

## 示例

```bash
# 基本用法
python deploy.py --host 192.168.1.100 --user root --key ~/.ssh/id_rsa

# 自定义部署路径和 JVM 参数
python deploy.py --host 192.168.1.100 --user root --key ~/.ssh/id_rsa --deploy-path /app --java-opts "-Xmx1g -Xms512m"

# 包含测试
python deploy.py --host 192.168.1.100 --user root --key ~/.ssh/id_rsa --skip-tests=false
```

## 执行流程

1. Maven 打包（mvn clean package -DskipTests）
2. SCP 上传 JAR 到远程服务器
3. SSH 停止旧服务
4. SSH 启动新服务
