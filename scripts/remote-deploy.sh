#!/bin/bash
# ============================================
# 远程一键部署脚本
# 在你的电脑终端执行（不是在服务器上执行）
#
# 用法: bash remote-deploy.sh
# 前提: 你的电脑能 ssh jiyou2026-tls
# ============================================
set -e

SERVER="jiyou2026-tls"
APP_DIR="/opt/pm-system"
BRANCH="claude/sync-app-to-mobile-GqiOm"
REPO="https://github.com/yhai84-jiyou/DM001.git"

echo "========================================="
echo "  远程部署：项目问题管理系统"
echo "  服务器: $SERVER"
echo "  目录: $APP_DIR"
echo "  端口: 3040"
echo "========================================="

# ----- 在服务器上执行的命令 -----
ssh "$SERVER" bash -s "$APP_DIR" "$BRANCH" "$REPO" << 'REMOTE_SCRIPT'
set -e
APP_DIR="$1"
BRANCH="$2"
REPO="$3"

echo ""
echo "[1/6] 检查 Docker..."
docker --version || { echo "Docker 未安装，正在安装..."; curl -fsSL https://get.docker.com | sh; systemctl enable docker; systemctl start docker; }

echo "[2/6] 克隆/更新代码..."
if [ ! -d "$APP_DIR" ]; then
    git clone "$REPO" "$APP_DIR"
    cd "$APP_DIR"
    git checkout "$BRANCH"
else
    cd "$APP_DIR"
    git fetch origin
    git checkout "$BRANCH"
    git pull origin "$BRANCH"
fi

echo "[3/6] 创建 .env 配置..."
if [ ! -f "$APP_DIR/.env" ]; then
    JWT_RANDOM=$(head -c 32 /dev/urandom | xxd -p | head -c 32)
    cat > "$APP_DIR/.env" << ENVEOF
DB_PASSWORD=pm_sys_$(head -c 8 /dev/urandom | xxd -p)
JWT_SECRET=${JWT_RANDOM}
AI_DEFAULT_PROVIDER=deepseek
AI_DEEPSEEK_API_KEY=
AI_DEEPSEEK_BASE_URL=https://api.deepseek.com
AI_DEEPSEEK_MODEL=deepseek-chat
AI_QWEN_API_KEY=
AI_QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_QWEN_MODEL=qwen-max
UPLOAD_DIR=/data/uploads
ENVEOF
    chmod 600 "$APP_DIR/.env"
    echo "  .env 已创建（JWT_SECRET 已自动生成随机值）"
    echo "  注意：AI Key 稍后可手动填写，不影响基础功能"
else
    echo "  .env 已存在，跳过"
fi

echo "[4/6] 构建 Docker 镜像（首次约3-5分钟）..."
cd "$APP_DIR"
docker compose build

echo "[5/6] 启动服务..."
docker compose up -d

echo "  等待数据库就绪..."
for i in $(seq 1 30); do
    if docker compose exec pm-postgres pg_isready -U pm_admin -d pm_system > /dev/null 2>&1; then
        echo "  数据库已就绪"
        break
    fi
    [ "$i" -eq 30 ] && echo "  警告: 数据库等待超时" && exit 1
    sleep 2
done

echo "[6/6] 初始化种子数据..."
docker compose exec pm-backend python -m app.seed || echo "  种子数据可能已初始化过"

echo ""
echo "========================================="
echo "  部署完成!"
echo ""
echo "  容器状态:"
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "  访问地址: http://119.45.56.39:3040"
echo "  管理员: 13800000000 / admin123"
echo ""
echo "  如需填写 AI Key:"
echo "    nano $APP_DIR/.env"
echo "    docker compose restart pm-backend"
echo ""
echo "  别忘了在腾讯云控制台安全组开放 TCP 3040"
echo "========================================="
REMOTE_SCRIPT

echo ""
echo "远程部署脚本执行完毕！"
echo "请访问: http://119.45.56.39:3040"
