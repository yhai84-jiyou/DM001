# Help Manual Website (帮助手册网站)

内部使用的产品帮助手册网站，提供文档编辑管理后台和前台展示系统。

## 功能特点

- **文档管理**：富文本 / Markdown 双模式编辑器，支持导入 Word 和 Markdown 文件
- **草稿/发布分离**：编辑保存不影响前台，手动发布才上线
- **版本管理**：每次发布需填写版本号和更新说明，支持版本回滚
- **多用户**：管理员创建编辑者账号，所有操作记录在审计日志中
- **可见性控制**：分类级别 + 文章级别双重控制前台展示
- **全文搜索**：SQLite FTS5 全文搜索，支持快捷键 Ctrl+K
- **媒体支持**：图片点击放大查看，视频全屏倍速播放
- **Docker 部署**：一键部署，整盘迁移只需复制 data 目录

## 技术栈

| 层面 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4 + Java 21 + SQLite + JPA |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus |
| 编辑器 | WangEditor 5 (富文本) + md-editor-v3 (Markdown) |
| 搜索 | SQLite FTS5 |
| 部署 | Docker + docker-compose |

## 快速开始

### Docker 部署（推荐）

```bash
# 1. 克隆项目
git clone <仓库地址> helpmanual
cd helpmanual

# 2. 配置
cp .env.example .env
# 编辑 .env，修改 JWT_SECRET

# 3. 启动
docker-compose up -d --build

# 4. 访问
# 前台: http://localhost:8080
# 后台: http://localhost:8080/admin
```

首次访问后台会引导创建管理员账号。

### 本地开发

```bash
# 后端
cd backend
mvn spring-boot:run

# 前端（另一个终端）
cd frontend
npm install
npm run dev
```

- 前台: http://localhost:5173
- 后台: http://localhost:5173/admin
- API: http://localhost:8080/api

## 数据备份与迁移

```bash
# 备份
./scripts/backup.sh

# 恢复
./scripts/restore.sh ./backups/helpmanual_backup_xxx.tar.gz

# 迁移到新服务器：复制 data/ 目录 + docker-compose up
```

## 项目文档

| 文档 | 说明 |
|------|------|
| [需求文档](docs/requirements.md) | 功能需求和非功能需求 |
| [使用手册](docs/user-guide.md) | 管理后台操作指南 |
| [部署手册](docs/deployment.md) | 安装、配置、迁移、备份 |

## 项目结构

```
.
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── docs/             # 项目文档
├── scripts/          # 备份/恢复脚本
├── docker-compose.yml
├── Dockerfile
└── README.md
```

## License

Internal Use Only
