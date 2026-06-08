#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f .env ]; then
    echo "❌ 错误: 未找到 .env 文件"
    exit 1
fi

source .env

echo ""
echo "=========================================="
echo "  校园学习资料互助圈 - 项目启动脚本"
echo "=========================================="
echo ""

check_port() {
    local port=$1
    local service_name=$2
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN > /dev/null 2>&1; then
        local pid=$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t | head -1)
        local proc_name=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
        echo ""
        echo "❌ 端口冲突: $service_name 端口 $port 已被占用"
        echo "   占用进程 PID: $pid"
        echo "   进程名称: $proc_name"
        echo "   请先停止占用进程或修改 .env 中的端口配置"
        echo ""
        exit 1
    fi
}

echo "🔍 正在检查端口占用情况..."
check_port "$FRONTEND_PORT" "前端(Nginx)"
check_port "$BACKEND_PORT" "后端(SpringBoot)"
check_port "$MYSQL_PORT" "MySQL"
check_port "$REDIS_PORT" "Redis"
echo "✅ 所有端口可用"
echo ""

echo "🚀 正在启动 Docker 容器集群..."
echo ""
docker compose up -d --build

echo ""
echo "⏳ 等待服务启动完成（约需 30-60 秒）..."
sleep 15

MAX_RETRY=20
RETRY=0
FRONTEND_READY=0
BACKEND_READY=0

while [ $RETRY -lt $MAX_RETRY ]; do
    if [ $FRONTEND_READY -eq 0 ] && curl -sSf "http://127.0.0.1:${FRONTEND_PORT}" > /dev/null 2>&1; then
        FRONTEND_READY=1
    fi
    
    if [ $BACKEND_READY -eq 0 ] && curl -sSf "http://127.0.0.1:${BACKEND_PORT}/categories" > /dev/null 2>&1; then
        BACKEND_READY=1
    fi
    
    if [ $FRONTEND_READY -eq 1 ] && [ $BACKEND_READY -eq 1 ]; then
        break
    fi
    
    sleep 3
    RETRY=$((RETRY + 1))
    echo -n "."
done

echo ""
echo ""
echo "=========================================="
echo "  ✅ 项目启动成功！"
echo "=========================================="
echo ""
echo "  🌐 前端访问地址:"
echo "     http://localhost:${FRONTEND_PORT}"
echo "     http://127.0.0.1:${FRONTEND_PORT}"
echo ""
echo "  🔌 后端 API 地址:"
echo "     http://127.0.0.1:${BACKEND_PORT}"
echo ""
echo "  🐬 MySQL 连接:"
echo "     Host: 127.0.0.1  Port: ${MYSQL_PORT}"
echo "     Database: ${MYSQL_DATABASE}"
echo "     Username: ${MYSQL_USER}"
echo "     Password: ${MYSQL_PASSWORD}"
echo ""
echo "  📦 Redis 连接:"
echo "     Host: 127.0.0.1  Port: ${REDIS_PORT}"
echo "     Password: ${REDIS_PASSWORD}"
echo ""
echo "  📋 常用命令:"
echo "     查看状态:  docker compose ps"
echo "     查看日志:  docker compose logs -f"
echo "     停止服务:  docker compose down"
echo ""
echo "=========================================="
echo ""
