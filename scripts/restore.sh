#!/bin/bash
# ============================================================
# 帮助手册网站 - 数据恢复脚本
# 使用方式: ./scripts/restore.sh <备份文件路径>
# 示例: ./scripts/restore.sh ./backups/helpmanual_backup_20260405_020000.tar.gz
# ============================================================

set -e

if [ -z "$1" ]; then
    echo "用法: $0 <备份文件路径>"
    echo "示例: $0 ./backups/helpmanual_backup_20260405_020000.tar.gz"
    exit 1
fi

BACKUP_FILE="$1"
DATA_DIR="${DATA_DIR:-./data}"
TEMP_DIR=$(mktemp -d)

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "错误: 备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

echo "警告: 此操作将覆盖当前数据！"
read -p "确认恢复? (输入 yes 继续): " confirm
if [ "$confirm" != "yes" ]; then
    echo "已取消"
    exit 0
fi

echo "[$(date)] 开始恢复..."

# 1. 解压备份
echo "  解压备份文件..."
tar -xzf "${BACKUP_FILE}" -C "${TEMP_DIR}"

# 2. 停止服务（如果在 Docker 中运行）
if command -v docker &> /dev/null && docker ps --format '{{.Names}}' | grep -q helpmanual; then
    echo "  停止 Docker 容器..."
    docker stop helpmanual
fi

# 3. 恢复数据库
BACKUP_DB=$(find "${TEMP_DIR}" -name "*.db" | head -1)
if [ -n "${BACKUP_DB}" ]; then
    echo "  恢复数据库..."
    mkdir -p "${DATA_DIR}"
    cp "${BACKUP_DB}" "${DATA_DIR}/helpmanual.db"
fi

# 4. 恢复上传文件
BACKUP_UPLOADS=$(find "${TEMP_DIR}" -name "*_uploads.tar.gz" | head -1)
if [ -n "${BACKUP_UPLOADS}" ]; then
    echo "  恢复上传文件..."
    mkdir -p "${DATA_DIR}"
    tar -xzf "${BACKUP_UPLOADS}" -C "${DATA_DIR}"
fi

# 5. 重启服务
if command -v docker &> /dev/null && docker ps -a --format '{{.Names}}' | grep -q helpmanual; then
    echo "  重启 Docker 容器..."
    docker start helpmanual
fi

# 清理临时目录
rm -rf "${TEMP_DIR}"

echo "[$(date)] 恢复完成!"
