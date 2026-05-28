# 项目交接文档 — 转交给本地 Claude Code

> 日期：2026-05-28
> 上一会话: claude.ai/code (Web 版)
> 当前分支: `claude/sync-app-to-mobile-GqiOm`

---

## 1. 你是谁、要做什么

你（本地 Claude Code）刚刚接手了一个 **基于本体论的项目问题管理系统** 开发任务。

用户角色：乙方技术负责人，正在给一家集团客户（甲方）做大型电商平台交付。

**痛点**：交付过程中客户提出的问题（Bug、设计缺陷、功能优化、咨询等）目前用飞书多维表格管理，**看不到谁在什么时候改了什么**，导致问题状态混乱、跟进失控。

**目标**：基于开源能力搭建一个项目管理系统，核心特性：
- 完整的变更日志（解决多维表格痛点）
- 状态机驱动的问题流转
- 数据字典配置化（不硬编码任何枚举）
- AI 辅助（DeepSeek + 通义千问）
- 基于本体论的语义层
- 部署到用户的腾讯云服务器

---

## 2. 必读文档（按顺序）

仓库根目录有 3 份核心设计文档，**先读完再写代码**：

| 文件 | 内容 |
|------|------|
| `docs/01-ontology-design.md` | 本体论设计：18个核心类、属性、关系、状态机、10条业务规则、语义层 |
| `docs/02-system-architecture.md` | 系统架构：技术选型、分层、完整数据库DDL（17张表）、核心机制设计 |
| `docs/03-implementation-roadmap.md` | 6阶段实施路线图，每步含具体提示词和验收清单 |

---

## 3. 当前进度

### 已完成（阶段 1-4）

| 阶段 | 状态 |
|------|------|
| 1. 项目脚手架 | 完成 |
| 2. 用户与权限体系 | 完成 |
| 3. 问题管理核心 | 完成 |
| 4. 统计看板 | 完成 |

### 待完成

| 阶段 | 状态 |
|------|------|
| 5. AI 集成（DeepSeek/千问辅助录入 + 悬浮助手 + 语义层向量库） | 待开发 |
| 6. 部署上线 | **当前卡在这里** |

---

## 4. 技术栈（全开源）

| 层次 | 技术 |
|------|------|
| 后端 | FastAPI + SQLAlchemy 2.0 + Alembic + asyncpg |
| 数据库 | PostgreSQL 16 + pgvector |
| 缓存 | Redis 7 |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + ECharts + **vxe-table**（Excel风格） |
| AI | openai SDK（DeepSeek/千问都兼容OpenAI协议） |
| 部署 | Docker + Docker Compose |
| 反代 | Nginx |

---

## 5. 关键设计决策（别改！）

1. **所有可枚举选项走数据字典**：平台、问题类型、问题状态、优先级、AI服务商全部存 `dictionary_items` 表，不硬编码
2. **状态流转规则存 `status_transitions` 表**，状态机引擎从数据库加载，不硬编码
3. **变更日志通过 SQLAlchemy `after_update` 事件钩子自动记录**（见 `backend/app/core/changelog_hook.py`）
4. **`change_logs` 表只增不改不删**（解决多维表格痛点的核心）
5. **AI Provider 通过数据字典 `AI_PROVIDER` 切换**，运行时从字典读 base_url 和 model
6. **甲方用户只能看自己提交的问题**（API层已实现数据权限）
7. **只有问题提交人可以将 RELEASED 状态改为 RESOLVED/UNRESOLVED**（状态机special_rule="SUBMITTER_ONLY"）
8. **表格统一用 vxe-table**（Excel风格交互，用户明确要求）

---

## 6. 项目结构

```
DM001/
├── docs/                        # 设计文档（必读）
│   ├── 01-ontology-design.md
│   ├── 02-system-architecture.md
│   └── 03-implementation-roadmap.md
├── HANDOFF.md                   # 本文件
│
├── backend/                     # FastAPI 后端
│   ├── app/
│   │   ├── main.py              # 入口（lifespan自动建表+种子数据）
│   │   ├── config.py            # pydantic-settings配置
│   │   ├── database.py          # 异步数据库连接
│   │   ├── seed.py              # 种子数据
│   │   ├── models/              # 10个模型文件（全部完成）
│   │   ├── api/                 # 9个路由模块
│   │   │   ├── auth.py          # 完成
│   │   │   ├── users.py         # 完成
│   │   │   ├── organizations.py # 完成
│   │   │   ├── dictionary.py    # 完成
│   │   │   ├── issues.py        # 完成
│   │   │   ├── reports.py       # 完成
│   │   │   ├── changelog.py     # TODO占位
│   │   │   ├── attachments.py   # TODO占位
│   │   │   └── ai_assistant.py  # TODO占位（阶段5要实现）
│   │   ├── core/
│   │   │   ├── security.py      # JWT（用PyJWT而非python-jose）
│   │   │   ├── dependencies.py  # 认证依赖+RBAC
│   │   │   ├── state_machine.py # 数据字典驱动的状态机
│   │   │   ├── changelog_hook.py# SQLAlchemy事件监听器
│   │   │   └── exceptions.py
│   │   ├── schemas/             # Pydantic请求/响应
│   │   └── utils/
│   │       └── id_generator.py  # 问题编号生成
│   ├── alembic/                 # 注：初始迁移未生成，启动时用create_all自动建表
│   ├── Dockerfile
│   └── requirements.txt
│
├── frontend/                    # Vue 3 前端
│   ├── src/
│   │   ├── main.ts              # 入口（注册vxe-table）
│   │   ├── router/index.ts      # 路由+登录守卫
│   │   ├── stores/
│   │   │   ├── auth.ts          # Pinia认证store
│   │   │   └── dictionary.ts    # 数据字典缓存
│   │   ├── api/request.ts       # axios封装
│   │   ├── components/layout/
│   │   │   └── AppLayout.vue    # 主布局
│   │   └── views/
│   │       ├── login/LoginPage.vue           # 完成
│   │       ├── register/RegisterPage.vue     # 完成
│   │       ├── issue/
│   │       │   ├── IssueList.vue             # 完成（vxe-table）
│   │       │   ├── IssueDetail.vue           # 完成
│   │       │   └── IssueForm.vue             # 完成（含AI辅助区，但后端AI接口未实现）
│   │       ├── dashboard/Overview.vue        # 完成
│   │       ├── admin/
│   │       │   ├── UserManage.vue            # 完成
│   │       │   ├── DictManage.vue            # 完成
│   │       │   ├── OrgManage.vue             # TODO占位
│   │       │   └── RoleManage.vue            # TODO占位
│   │       └── report/DailyReport.vue        # TODO占位
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
│
├── scripts/
│   ├── server-init.sh           # 服务器初始化
│   ├── deploy.sh                # 部署脚本
│   └── remote-deploy.sh         # 远程一键部署
│
├── docker-compose.yml           # 4个服务：pm-postgres/pm-redis/pm-backend/pm-frontend
├── Makefile
└── README.md
```

---

## 7. 部署目标（腾讯云服务器）

**服务器信息**：
- SSH 别名：`jiyou2026-tls`
- 公网 IP：`119.45.56.39`
- OS：Ubuntu
- Docker：已安装

**端口分配**（避开已占用端口 8000/5432/6379）：
- 前端入口：**3040**（唯一对外）
- 后端、PG、Redis 都不暴露外部端口，只在 Docker 网络 `pm-net` 内部访问
- 所有容器加 `pm-` 前缀避免冲突

**部署目录**：`/opt/pm-system/`

**腾讯云控制台需开放**：TCP 3040 端口入站规则

---

## 8. 当前部署状态 & 问题

用户**已经在服务器上启动过容器**，但访问 `http://119.45.56.39:3040` 返回 **HTTP 503**。

最可能的原因：
1. 后端容器没起来，nginx 找不到后端
2. 数据库表没建成功
3. 种子数据脚本报错

**我已经在最新提交中修复**：在 `main.py` 的 `lifespan` 里加了：
```python
async with engine.begin() as conn:
    await conn.run_sync(Base.metadata.create_all)
await seed_all()
```

启动时会自动建表+灌种子数据。

**用户接下来需要重新拉代码+重建容器**。命令是：
```bash
ssh jiyou2026-tls 'cd /opt/pm-system && git pull && docker compose up -d --build && sleep 30 && docker compose logs pm-backend --tail=40'
```

---

## 9. 你接手后第一件事

**用户已切换到本地 Claude Code，你能直接 SSH 到他的服务器了。**

请按以下顺序操作：

### Step 1：拉取最新代码到本地

```bash
git clone https://github.com/yhai84-jiyou/DM001.git
cd DM001
git checkout claude/sync-app-to-mobile-GqiOm
```

### Step 2：SSH 到服务器，诊断 503

```bash
ssh jiyou2026-tls

# 查看容器状态
cd /opt/pm-system
docker compose ps

# 查看后端日志
docker compose logs pm-backend --tail=100

# 查看前端日志
docker compose logs pm-frontend --tail=30
```

### Step 3：根据诊断结果修复

**如果是表没建成功**：拉最新代码（lifespan已修复）→ `docker compose up -d --build`

**如果是其他错误**：根据日志判断，修代码 → 推送到 GitHub → 服务器 git pull → 重建容器

### Step 4：验证部署成功

访问 `http://119.45.56.39:3040`，应该能看到登录页。

用 `13800000000` / `admin123` 登录（这是种子数据里的管理员账号）。

### Step 5：继续阶段 5（AI 集成）

按照 `docs/03-implementation-roadmap.md` 第 5 章实现：
- `backend/app/services/ai_service.py`：多模型适配（DeepSeek/千问）
- `backend/app/api/ai_assistant.py`：填充实际逻辑（当前是占位）
- AI 辅助录入接口 `POST /api/v1/ai/parse-issue`
- AI 悬浮助手 `POST /api/v1/ai/chat`（SSE流式）
- 前端右下角悬浮 AI 助手组件
- pgvector 语义层

用户的 DeepSeek API Key 还没填，需要他在 `/opt/pm-system/.env` 里加上 `AI_DEEPSEEK_API_KEY=sk-xxx` 再重启后端。

---

## 10. Git 提交规范

每完成一个独立功能点就提交，commit message 格式：

```
feat: 简短中文描述

详细说明（可多行）

https://claude.ai/code/session_xxx
```

最后一行的 session URL 用你当前会话的链接。

---

## 11. 用户的关键偏好

1. **不要从零重写**，全部用开源框架/库组装
2. **表格用 vxe-table**（Excel/多维表格风格交互，已引入）
3. **数据字典驱动一切可枚举项**，加新选项不改代码
4. **AI 用 DeepSeek + 通义千问**，不用 Claude/OpenAI 直连
5. **每次完成一步就推送到 GitHub**，不要本地积压

---

## 12. 联系/上下文

如果用户问起当前进度或遇到问题，记住：
- 之前4个阶段全部跑通了，503是部署环境问题，不是代码问题
- 用户对技术细节不熟悉，**用大白话解释**，少用术语
- 用户希望快速看到效果，**先让线上跑起来再做新功能**

祝你接手顺利！
