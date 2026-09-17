#!/bin/bash
# ============================================
# cpa-external 服务器部署脚本（在服务器上执行）
#
# 应用镜像由 GitHub Actions 自动构建并推送到 ghcr.io，
# 本脚本负责拉取镜像并管理容器，无需源码和 jar。
# 私有镜像首次使用前需执行：docker login ghcr.io（PAT 勾选 read:packages），
# 或在 GitHub Package 设置中将镜像改为 public。
#
# 此目录只需要以下文件：
#   - docker-compose.yml
#   - deploy-server.sh
#   - .env
#   - mysql/conf/my.cnf、mysql/init/*.sql
#   - redis/conf/redis.conf
#
# Web 入口由宿主机 Caddy 负责（安装与配置见 DEPLOY.md），不在本脚本管理范围。
#
# 用法：
#   bash deploy-server.sh            # 拉取镜像并启动
#   bash deploy-server.sh start      # 拉取镜像并启动
#   bash deploy-server.sh stop       # 停止所有服务
#   bash deploy-server.sh restart    # 重启服务
#   bash deploy-server.sh update     # 拉取最新镜像并更新
#   bash deploy-server.sh logs [svc] # 查看日志
#   bash deploy-server.sh status     # 查看服务状态
# ============================================

set -e

# ---- 颜色输出 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ---- 获取脚本所在目录 ----
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ---- 检查 Docker 环境 ----
check_docker() {
    if ! command -v docker &> /dev/null; then
        error "Docker 未安装，请先安装 Docker"
    fi

    if ! docker compose version &> /dev/null; then
        if ! command -v docker-compose &> /dev/null; then
            error "Docker Compose 未安装，请先安装 Docker Compose"
        fi
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    COMPOSE_FILE="-f docker-compose.yml"
    ok "Docker 环境检查通过"
}

# ---- 检查 .env 文件 ----
check_env() {
    if [ ! -f .env ]; then
        error ".env 文件不存在，请先创建并配置"
    fi

    set -a
    source .env
    set +a

    # 必填项校验，缺失时直接终止，避免带空密钥上线
    if [ -z "${MYSQL_ROOT_PASSWORD}" ] || [ "${MYSQL_ROOT_PASSWORD}" = "change-me-mysql-root-pwd" ]; then
        error "MYSQL_ROOT_PASSWORD 未配置"
    fi
    if [ -z "${REDIS_PASSWORD}" ] || [ "${REDIS_PASSWORD}" = "change-me-redis-pwd" ]; then
        error "REDIS_PASSWORD 未配置"
    fi
    if [ -z "${TOKEN_SECRET}" ] || [ "${TOKEN_SECRET}" = "change-me-token-secret" ]; then
        error "TOKEN_SECRET 未配置"
    fi

    ok "环境变量加载并校验完成"
}

# ---- 创建必要目录 ----
create_dirs() {
    mkdir -p mysql/data redis/data uploads logs
    ok "目录结构就绪"
}

# ---- 拉取镜像 ----
pull_images() {
    info "拉取镜像..."
    if ! $COMPOSE_CMD $COMPOSE_FILE pull; then
        error "镜像拉取失败：私有镜像请先执行 docker login ghcr.io，或核对 .env 中 GHCR_OWNER"
    fi
    ok "镜像拉取完成"
}

# ---- 启动服务 ----
do_start() {
    info "启动 cpa-external 服务..."
    check_docker
    check_env
    create_dirs
    pull_images

    $COMPOSE_CMD $COMPOSE_FILE up -d

    echo ""
    ok "=========================================="
    ok "  cpa-external 部署完成！"
    ok "=========================================="
    echo ""
    info "对外入口由宿主机 Caddy 提供（域名见 /etc/caddy/Caddyfile）"
    echo ""
    info "服务状态："
    $COMPOSE_CMD $COMPOSE_FILE ps
}

# ---- 停止服务 ----
do_stop() {
    check_docker
    info "停止 cpa-external 服务..."
    $COMPOSE_CMD $COMPOSE_FILE down
    ok "所有服务已停止"
}

# ---- 重启服务 ----
do_restart() {
    check_docker
    info "重启 cpa-external 服务..."
    $COMPOSE_CMD $COMPOSE_FILE restart
    ok "服务已重启"
}

# ---- 拉取最新镜像并更新 ----
do_update() {
    info "更新 cpa-external 服务..."
    check_docker
    check_env
    pull_images

    $COMPOSE_CMD $COMPOSE_FILE up -d --remove-orphans

    echo ""
    ok "=========================================="
    ok "  cpa-external 更新完成！"
    ok "=========================================="
    $COMPOSE_CMD $COMPOSE_FILE ps
    echo ""
    info "回滚方法：在 .env 中将 IMAGE_TAG 改为指定 commit 短哈希后重新执行 update"
}

# ---- 查看日志 ----
do_logs() {
    check_docker
    local service="${1:-}"
    if [ -n "$service" ]; then
        $COMPOSE_CMD $COMPOSE_FILE logs -f "$service"
    else
        $COMPOSE_CMD $COMPOSE_FILE logs -f
    fi
}

# ---- 查看状态 ----
do_status() {
    check_docker
    info "服务状态："
    $COMPOSE_CMD $COMPOSE_FILE ps
}

# ---- 主入口 ----
case "${1:-start}" in
    start)   do_start   ;;
    stop)    do_stop    ;;
    restart) do_restart ;;
    update)  do_update  ;;
    logs)    do_logs "$2" ;;
    status)  do_status  ;;
    *)
        echo "用法: bash deploy-server.sh {start|stop|restart|update|logs|status}"
        echo ""
        echo "  start    拉取镜像并启动（默认）"
        echo "  stop     停止所有服务"
        echo "  restart  重启所有服务"
        echo "  update   拉取最新镜像并更新"
        echo "  logs     查看所有服务日志"
        echo "  logs Svc 查看指定服务日志（cpa-external/mysql/redis）"
        echo "  status   查看服务状态"
        exit 1
        ;;
esac
