#!/bin/bash
set -e

echo "========================================="
echo "  腾讯云服务器初始化脚本"
echo "  适用于 Ubuntu 20.04+ / Debian 11+"
echo "========================================="

# 1. 安装 Docker
if ! command -v docker &> /dev/null; then
    echo "[1/4] 安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
    echo "Docker 安装完成"
else
    echo "[1/4] Docker 已安装: $(docker --version)"
fi

# 2. 安装 Docker Compose (V2 随 Docker 一起安装)
if ! docker compose version &> /dev/null; then
    echo "[2/4] 安装 Docker Compose 插件..."
    apt-get update && apt-get install -y docker-compose-plugin
else
    echo "[2/4] Docker Compose 已安装: $(docker compose version)"
fi

# 3. 安装 Git
if ! command -v git &> /dev/null; then
    echo "[3/4] 安装 Git..."
    apt-get update && apt-get install -y git
else
    echo "[3/4] Git 已安装: $(git --version)"
fi

# 4. 克隆项目
echo "[4/4] 克隆项目..."
if [ ! -d "/opt/pm-system" ]; then
    git clone https://github.com/yhai84-jiyou/DM001.git /opt/pm-system
    cd /opt/pm-system
else
    echo "项目目录已存在，拉取最新代码..."
    cd /opt/pm-system
    git pull
fi

# 5. 创建 .env 文件
if [ ! -f ".env" ]; then
    echo ""
    echo "请配置环境变量..."
    cp backend/.env.example .env
    echo ""
    echo "========================================="
    echo "  重要：请编辑 /opt/pm-system/.env 文件"
    echo "  修改以下配置："
    echo "    - JWT_SECRET (改成随机字符串)"
    echo "    - AI_DEEPSEEK_API_KEY (你的Key)"
    echo "    - AI_QWEN_API_KEY (你的Key)"
    echo ""
    echo "  然后执行: cd /opt/pm-system && bash scripts/deploy.sh"
    echo "========================================="
else
    echo ".env 文件已存在"
fi

echo ""
echo "服务器初始化完成！"
echo ""
echo "下一步："
echo "  1. 编辑 .env 文件: nano /opt/pm-system/.env"
echo "  2. 执行部署: cd /opt/pm-system && bash scripts/deploy.sh"
