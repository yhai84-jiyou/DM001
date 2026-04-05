#!/bin/bash
# ============================================================
# 帮助手册网站 - 数据备份脚本
# 使用方式: ./scripts/backup.sh [备份目录]
# 定时备份: crontab -e → 0 2 * * * /path/to/backup.sh
# ============================================================

set -e

# 配置
DATA_DIR="${DATA_DIR:-./data}"
BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="helpmanual_backup_${TIMESTAMP}"
KEEP_DAYS=30  # 保留最近30天的备份

# 创建备份目录
mkdir -p "${BACKUP_DIR}"

echo "[$(date)] 开始备份..."

# 1. 备份 SQLite 数据库（使用 sqlite3 的 .backup 命令确保一致性）
if command -v sqlite3 &> /dev/null; then
    echo "  使用 sqlite3 .backup 命令备份数据库..."
    sqlite3 "${DATA_DIR}/helpmanual.db" ".backup '${BACKUP_DIR}/${BACKUP_NAME}.db'"
else
    echo "  sqlite3 未安装，直接复制数据库文件..."
    cp "${DATA_DIR}/helpmanual.db" "${BACKUP_DIR}/${BACKUP_NAME}.db"
fi

# 2. 备份上传文件
if [ -d "${DATA_DIR}/uploads" ]; then
    echo "  备份上传文件..."
    tar -czf "${BACKUP_DIR}/${BACKUP_NAME}_uploads.tar.gz" -C "${DATA_DIR}" uploads/
fi

# 3. 打包为单个备份文件
echo "  打包备份..."
tar -czf "${BACKUP_DIR}/${BACKUP_NAME}.tar.gz" \
    -C "${BACKUP_DIR}" \
    "${BACKUP_NAME}.db" \
    ${BACKUP_NAME}_uploads.tar.gz 2>/dev/null || true

# 清理临时文件
rm -f "${BACKUP_DIR}/${BACKUP_NAME}.db"
rm -f "${BACKUP_DIR}/${BACKUP_NAME}_uploads.tar.gz"

# 4. 清理过期备份
echo "  清理 ${KEEP_DAYS} 天前的备份..."
find "${BACKUP_DIR}" -name "helpmanual_backup_*.tar.gz" -mtime +${KEEP_DAYS} -delete 2>/dev/null || true

BACKUP_SIZE=$(du -h "${BACKUP_DIR}/${BACKUP_NAME}.tar.gz" | cut -f1)
echo "[$(date)] 备份完成: ${BACKUP_DIR}/${BACKUP_NAME}.tar.gz (${BACKUP_SIZE})"
