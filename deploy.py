#!/usr/bin/env python3
"""
部署脚本：打包 JAR 并部署到远程服务器
用法：python deploy.py --host <服务器地址> --user <用户名> --key <密钥路径>
"""

import argparse
import subprocess
import os
import sys
from datetime import datetime


def log_info(msg):
    print(f"\033[92m[INFO]\033[0m {msg}")


def log_error(msg):
    print(f"\033[91m[ERROR]\033[0m {msg}")


def run_command(cmd, cwd=None):
    """执行命令并返回结果"""
    log_info(f"执行: {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
    if result.returncode != 0:
        log_error(f"命令执行失败: {result.stderr}")
        return False, result.stderr
    return True, result.stdout


def build_jar(skip_tests=True):
    """Maven 打包"""
    log_info("开始 Maven 打包...")

    cmd = "mvn clean package"
    if skip_tests:
        cmd += " -DskipTests"

    success, output = run_command(cmd)
    if not success:
        return None

    # 查找生成的 JAR 文件
    target_dir = "target"
    for f in os.listdir(target_dir):
        if f.endswith(".jar") and not f.endswith("-original.jar") and "SNAPSHOT" in f:
            return os.path.join(target_dir, f)

    return None


def deploy_to_server(jar_path, host, port, user, key_path, deploy_path, java_opts):
    """部署到远程服务器"""
    import paramiko
    from scp import SCPClient

    log_info(f"连接服务器 {host}:{port}...")

    # SSH 连接
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        ssh.connect(hostname=host, port=port, username=user, key_filename=key_path)
    except Exception as e:
        log_error(f"SSH 连接失败: {e}")
        return False

    # 上传 JAR
    jar_name = os.path.basename(jar_path)
    remote_jar = f"{deploy_path}/{jar_name}"

    log_info(f"上传 JAR 文件到 {remote_jar}...")

    with SCPClient(ssh.get_transport()) as scp:
        scp.put(jar_path, remote_jar)

    # 停止旧服务
    log_info("停止旧服务...")
    stop_cmd = f"""
        cd {deploy_path} && \
        if [ -f app.pid ]; then
            kill $(cat app.pid) 2>/dev/null || true
            rm -f app.pid
        fi
        sleep 2
    """
    ssh.exec_command(stop_cmd)

    # 启动新服务
    log_info("启动新服务...")
    start_cmd = f"""
        cd {deploy_path} && \
        nohup java {java_opts} -jar {jar_name} --spring.profiles.active=prod > app.log 2>&1 &
        echo $! > app.pid
        sleep 3
        if ps -p $(cat app.pid) > /dev/null 2>&1; then
            echo "Service started with PID $(cat app.pid)"
        else
            echo "Failed to start service"
        fi
    """

    stdin, stdout, stderr = ssh.exec_command(start_cmd)
    output = stdout.read().decode()
    log_info(output)

    ssh.close()
    return True


def main():
    parser = argparse.ArgumentParser(description="部署 JAR 到远程服务器")
    parser.add_argument("--host", required=True, help="远程服务器地址")
    parser.add_argument("--port", type=int, default=22, help="SSH 端口")
    parser.add_argument("--user", required=True, help="SSH 用户名")
    parser.add_argument("--key", required=True, help="SSH 私钥文件路径")
    parser.add_argument("--deploy-path", default="/opt/ai-studio", help="部署目录")
    parser.add_argument("--java-opts", default="-Xmx512m", help="JVM 参数")
    parser.add_argument("--skip-tests", action="store_true", default=True, help="跳过测试")

    args = parser.parse_args()

    # 检查密钥文件
    if not os.path.exists(args.key):
        log_error(f"密钥文件不存在: {args.key}")
        sys.exit(1)

    # 1. 打包
    jar_path = build_jar(args.skip_tests)
    if not jar_path:
        log_error("打包失败")
        sys.exit(1)
    log_info(f"打包成功: {jar_path}")

    # 2. 部署
    if deploy_to_server(jar_path, args.host, args.port, args.user, args.key, args.deploy_path, args.java_opts):
        log_info("部署成功!")
        print(f"\ndeploy_status: success")
        print(f"jar_path: {jar_path}")
        print(f"deploy_time: {datetime.now().isoformat()}")
    else:
        log_error("部署失败")
        sys.exit(1)


if __name__ == "__main__":
    main()
