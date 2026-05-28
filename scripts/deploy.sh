#!/bin/bash
set -e

APP_DIR="/opt/pm-system"
cd "$APP_DIR"

echo "========================================="
echo "  项目问题管理系统 — 部署"
echo "========================================="

# 检查 .env
if [ ! -f ".env" ]; then
    echo "错误: .env 文件不存在，请先执行 bash scripts/server-init.sh"
    exit 1
fi

# 1. 拉最新代码
echo "[1/5] 拉取最新代码..."
git pull origin main 2>/dev/null || git pull

# 2. 构建镜像
echo "[2/5] 构建 Docker 镜像（首次较慢）..."
docker compose build

# 3. 启动服务
echo "[3/5] 启动服务..."
docker compose up -d

# 4. 等待数据库就绪
echo "[4/5] 等待数据库就绪..."
for i in $(seq 1 30); do
    if docker compose exec pm-postgres pg_isready -U pm_admin -d pm_system > /dev/null 2>&1; then
        echo "  数据库已就绪"
        break
    fi
    sleep 2
done

# 5. 初始化种子数据
echo "[5/5] 初始化种子数据..."
docker compose exec pm-backend python -m app.seed

echo ""
echo "========================================="
echo "  部署完成!"
echo ""
echo "  访问地址: http://119.45.56.39:3040"
echo "  API 文档: http://119.45.56.39:3040/api/v1/docs"
echo "  管理员账号: 13800000000"
echo "  管理员密码: admin123"
echo ""
echo "  提醒: 请在腾讯云控制台安全组中"
echo "  开放 TCP 3040 端口的入站规则"
echo "========================================="
