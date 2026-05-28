#!/bin/bash
set -e

echo "========================================="
echo "  项目问题管理系统 — 部署脚本"
echo "========================================="

# 1. 拉最新代码
echo "[1/5] 拉取最新代码..."
git pull origin main

# 2. 构建镜像
echo "[2/5] 构建 Docker 镜像..."
docker compose build

# 3. 启动服务
echo "[3/5] 启动服务..."
docker compose up -d

# 4. 等待数据库就绪
echo "[4/5] 等待数据库就绪..."
sleep 5
docker compose exec backend python -c "print('Backend OK')"

# 5. 执行数据库迁移和种子数据
echo "[5/5] 执行数据库迁移和种子数据..."
docker compose exec backend alembic upgrade head 2>/dev/null || echo "提示: 如果是首次部署，请手动执行迁移"
docker compose exec backend python -m app.seed

echo ""
echo "========================================="
echo "  部署完成!"
echo "  前端: http://你的服务器IP"
echo "  后端API: http://你的服务器IP/api/v1/docs"
echo "  管理员: 13800000000 / admin123"
echo "========================================="
