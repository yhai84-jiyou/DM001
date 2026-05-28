#!/bin/bash
set -e

echo "========================================="
echo "  项目问题管理系统 — 腾讯云服务器初始化"
echo "  目标: /opt/pm-system/"
echo "  端口: 3040 (前端入口)"
echo "========================================="

APP_DIR="/opt/pm-system"

# 1. 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "[1/3] 安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
else
    echo "[1/3] Docker 已就绪: $(docker --version)"
fi

# 2. 克隆/更新代码
if [ ! -d "$APP_DIR" ]; then
    echo "[2/3] 克隆项目到 $APP_DIR ..."
    git clone https://github.com/yhai84-jiyou/DM001.git "$APP_DIR"
else
    echo "[2/3] 项目目录已存在，拉取最新代码..."
    cd "$APP_DIR" && git pull origin main
fi

# 3. 创建 .env
cd "$APP_DIR"
if [ ! -f ".env" ]; then
    echo "[3/3] 创建 .env 配置文件..."
    cat > .env << 'ENVEOF'
# 数据库密码（修改为强密码）
DB_PASSWORD=pm_system_2026

# JWT 密钥（修改为随机字符串）
JWT_SECRET=CHANGE_ME_TO_A_RANDOM_STRING_32_CHARS

# AI 配置（至少填一个）
AI_DEFAULT_PROVIDER=deepseek
AI_DEEPSEEK_API_KEY=sk-your-deepseek-key-here
AI_DEEPSEEK_BASE_URL=https://api.deepseek.com
AI_DEEPSEEK_MODEL=deepseek-chat
AI_QWEN_API_KEY=sk-your-qwen-key-here
AI_QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_QWEN_MODEL=qwen-max

# 文件上传
UPLOAD_DIR=/data/uploads
ENVEOF
    chmod 600 .env
    echo ""
    echo "========================================="
    echo "  请编辑 $APP_DIR/.env"
    echo "  必须修改: JWT_SECRET, AI_DEEPSEEK_API_KEY"
    echo ""
    echo "  nano $APP_DIR/.env"
    echo ""
    echo "  修改完后执行部署:"
    echo "  cd $APP_DIR && bash scripts/deploy.sh"
    echo "========================================="
else
    echo "[3/3] .env 已存在"
fi

echo ""
echo "初始化完成！"
echo ""
echo "=== 提醒 ==="
echo "1. 编辑 .env:  nano $APP_DIR/.env"
echo "2. 部署应用:   cd $APP_DIR && bash scripts/deploy.sh"
echo "3. 腾讯云控制台开放端口 3040 的入站规则"
