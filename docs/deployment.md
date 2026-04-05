# 帮助手册网站 - 部署手册

> 版本：v1.0  
> 适用对象：运维人员、系统管理员

---

## 1. 系统要求

| 项目 | 最低要求 | 推荐配置 |
|------|---------|---------|
| 操作系统 | Linux / macOS / Windows | Ubuntu 22.04 LTS |
| Docker | 20.10+ | 最新稳定版 |
| Docker Compose | v2.0+ | 最新稳定版 |
| 内存 | 512MB | 1GB+ |
| 磁盘 | 1GB（不含上传文件） | 根据文件量预估 |
| 网络 | 内网可达 | — |

> 如果不使用 Docker，还需要：Java 21 JRE、Node.js 18+（仅构建时）

---

## 2. Docker 部署（推荐）

### 2.1 快速开始

```bash
# 1. 获取项目代码
git clone <仓库地址> helpmanual
cd helpmanual

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，设置 JWT_SECRET（必须修改！）
# JWT_SECRET=你的随机字符串至少32位

# 3. 构建并启动
docker-compose up -d --build

# 4. 查看状态
docker-compose ps
docker-compose logs -f
```

### 2.2 访问系统

- 前台文档：`http://服务器IP:8080`
- 后台管理：`http://服务器IP:8080/admin`
- 首次访问后台会引导创建管理员账号

### 2.3 环境变量说明

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `JWT_SECRET` | 是 | — | JWT 签名密钥，至少 32 个字符的随机字符串 |
| `SPRING_PROFILES_ACTIVE` | 否 | prod | Spring 配置文件 |
| `JAVA_OPTS` | 否 | -Xmx512m -Xms256m | JVM 参数 |
| `SERVER_PORT` | 否 | 8080 | 服务端口 |

### 2.4 数据持久化

所有数据存储在 `./data` 目录：

```
data/
├── helpmanual.db      # SQLite 数据库（用户、文章、分类等所有数据）
└── uploads/           # 上传的图片、视频、PDF 文件
    ├── 2026/
    │   ├── 04/
    │   │   ├── uuid-1.jpg
    │   │   └── uuid-2.mp4
```

> **重要**：`data/` 目录是系统的全部持久化数据，请务必定期备份。

---

## 3. 数据备份

### 3.1 手动备份

```bash
# 使用备份脚本
./scripts/backup.sh

# 指定备份目录
./scripts/backup.sh /path/to/backups
```

备份文件格式：`helpmanual_backup_20260405_020000.tar.gz`

### 3.2 定时自动备份

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点自动备份，保留最近 30 天
0 2 * * * /path/to/helpmanual/scripts/backup.sh /path/to/backups >> /var/log/helpmanual-backup.log 2>&1
```

### 3.3 恢复数据

```bash
# 使用恢复脚本（会提示确认）
./scripts/restore.sh ./backups/helpmanual_backup_20260405_020000.tar.gz
```

---

## 4. 整盘迁移到新服务器

迁移非常简单，只需要 3 步：

### 步骤 1：在旧服务器打包

```bash
# 停止服务
docker-compose down

# 打包项目代码 + 数据
tar -czf helpmanual-migration.tar.gz \
    --exclude='backend/target' \
    --exclude='frontend/node_modules' \
    --exclude='frontend/dist' \
    .
```

### 步骤 2：传输到新服务器

```bash
scp helpmanual-migration.tar.gz user@新服务器:/opt/
```

### 步骤 3：在新服务器启动

```bash
# 解压
cd /opt
tar -xzf helpmanual-migration.tar.gz -C helpmanual
cd helpmanual

# 确保 .env 配置正确
cat .env

# 启动（无需重新构建，data 目录已包含所有数据）
docker-compose up -d --build

# 验证
curl http://localhost:8080/api/public/categories
```

> 数据完整性：所有用户、文章、版本历史、上传文件都在 `data/` 目录中，迁移后完全一致。

---

## 5. 非 Docker 部署（直接运行 JAR）

### 5.1 构建

```bash
# 构建前端
cd frontend
npm install
npm run build

# 复制前端到后端 static 目录
cp -r dist/* ../backend/src/main/resources/static/

# 构建后端
cd ../backend
mvn clean package -DskipTests

# 生成的 JAR：backend/target/helpmanual.jar
```

### 5.2 运行

```bash
# 设置环境变量
export JWT_SECRET="你的随机密钥至少32位"

# 运行
java -Xmx512m -jar backend/target/helpmanual.jar
```

### 5.3 配置为系统服务（systemd）

创建 `/etc/systemd/system/helpmanual.service`：

```ini
[Unit]
Description=Help Manual Website
After=network.target

[Service]
Type=simple
User=helpmanual
WorkingDirectory=/opt/helpmanual
ExecStart=/usr/bin/java -Xmx512m -jar backend/target/helpmanual.jar
Environment=JWT_SECRET=你的随机密钥
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable helpmanual
sudo systemctl start helpmanual
```

---

## 6. 反向代理配置（Nginx）

如果需要通过域名访问或启用 HTTPS：

```nginx
server {
    listen 80;
    server_name docs.yourcompany.com;

    # 重定向到 HTTPS（可选）
    # return 301 https://$server_name$request_uri;

    client_max_body_size 300m;  # 匹配文件上传限制

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 支持（如果需要）
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 上传文件缓存
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/uploads/;
        proxy_cache_valid 200 7d;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 7. 常见问题

### Q: 启动后访问 8080 端口无响应？
- 检查 Docker 是否正常运行：`docker-compose ps`
- 查看日志：`docker-compose logs -f`
- 检查端口是否被占用：`lsof -i :8080`

### Q: 上传大文件失败？
- 检查 Nginx `client_max_body_size` 是否设置为 300m
- 检查磁盘空间是否充足

### Q: 数据库损坏怎么办？
- 停止服务：`docker-compose down`
- 从备份恢复：`./scripts/restore.sh <备份文件>`
- 重启：`docker-compose up -d`

### Q: 如何修改端口？
编辑 `docker-compose.yml`，将 `"8080:8080"` 改为 `"新端口:8080"`

### Q: 如何查看操作日志？
后台管理 → 操作日志页面，支持按用户和操作类型筛选

### Q: 忘记管理员密码？
目前需要手动操作数据库重置密码。使用 sqlite3 工具连接数据库，更新 user 表的 password_hash 字段（BCrypt 格式）。
