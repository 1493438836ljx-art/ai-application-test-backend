#!/bin/bash

# Build Package Skill Script
# 用于构建和打包 Maven 项目

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 获取参数
SKIP_TESTS="${1:-true}"
RUN_TESTS="${2:-false}"

# 输出带颜色的消息
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    log_error "Maven 未安装或未在 PATH 中"
    exit 1
fi

log_info "开始构建项目..."
log_info "跳过测试: $SKIP_TESTS"

# 创建 releases 目录
mkdir -p releases

# 清理项目
log_info "执行 mvn clean..."
mvn clean -q

# 编译项目
log_info "执行 mvn compile..."
mvn compile -q

# 运行测试（如果指定）
if [ "$RUN_TESTS" = "true" ] || [ "$SKIP_TESTS" = "false" ]; then
    log_info "执行 mvn test..."
    mvn test
fi

# 打包项目
log_info "执行 mvn package..."
if [ "$SKIP_TESTS" = "true" ]; then
    mvn package -DskipTests -q
else
    mvn package -q
fi

# 获取版本号
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_NAME="demo-${VERSION}.jar"
ORIGINAL_JAR_NAME="demo-${VERSION}-original.jar"

log_info "构建完成！"
log_info "版本: $VERSION"

# 复制 JAR 到 releases 目录
if [ -f "target/$JAR_NAME" ]; then
    cp "target/$JAR_NAME" "releases/"
    log_info "JAR 文件已复制到 releases/$JAR_NAME"
else
    log_error "未找到 JAR 文件: target/$JAR_NAME"
    exit 1
fi

if [ -f "target/$ORIGINAL_JAR_NAME" ]; then
    cp "target/$ORIGINAL_JAR_NAME" "releases/"
    log_info "原始 JAR 文件已复制到 releases/$ORIGINAL_JAR_NAME"
fi

log_info "========================================"
log_info "构建成功！"
log_info "========================================"
echo ""
log_info "JAR 文件位置:"
echo "  - releases/$JAR_NAME (可执行)"
if [ -f "releases/$ORIGINAL_JAR_NAME" ]; then
    echo "  - releases/$ORIGINAL_JAR_NAME"
fi
