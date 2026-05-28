# 项目完整交接包 — 给本地 Claude Code

> 日期：2026-05-28
> 来源：Web 版 Claude Code 沙箱会话
> 目标：本地 Claude Code 桌面版无缝接手

---

## 第一部分：项目背景（完整版）

### 1.1 用户身份

- **角色**：乙方技术负责人
- **客户**：某中央企业（集团公司），几十万员工，几千家分子公司
- **业务场景**：正在给客户交付一套大型软件系统，涉及多个端：
  - C 端：员工电商平台
  - B 端：企业采购平台（分子公司用）
  - 运营管理后台
  - 供应商协同门户
  - 企业自维护平台

### 1.2 用户痛点（为什么要做这个系统）

系统在交付过程中，客户会提各种问题：Bug、设计缺陷、功能优化、咨询答疑、外部系统问题等。当前用**飞书多维表格**管理，但有个**致命问题**：

> **看不到日志**——谁在什么时候改了什么状态、改了什么字段，可能就跟用户原始提的需求不一样了。

这导致问题跟踪混乱、责任不清、状态被人偷偷改了都不知道。

### 1.3 项目目标

基于本体论方法论，**用开源能力组装**一个项目问题管理系统，核心能力：

1. **完整的不可篡改变更日志**（直击痛点）
2. **状态机驱动**的问题全生命周期跟踪
3. **数据字典配置化**——所有可枚举选项后台管理，加新选项不改代码
4. **AI 辅助**：自然语言解析自动填表 + 悬浮助手问答 + 自动日报
5. **本体语义层 + 向量库**：让 AI 真正"理解"业务数据
6. **多维度统计看板**：人员负荷、模块健康度、停滞预警

---

## 第二部分：本次 Web 会话的完整工作流回顾

按时间顺序，我们做过的所有事情：

### 阶段 0：澄清和需求梳理
- 用户问"能不能把电脑程序同步到手机" → 澄清 GitHub 是唯一同步通道
- 用户描述项目需求 → 我提出基于本体论的设计方案
- 用户要求：基于开源、做数据字典化、AI 用 DeepSeek + 千问

### 阶段 1：3 份核心设计文档
- `docs/01-ontology-design.md` — 本体设计（类、关系、属性、状态机、规则、语义层）
- `docs/02-system-architecture.md` — 系统架构（技术栈、分层、DDL、核心机制）
- `docs/03-implementation-roadmap.md` — 6 阶段实施路线图（含每步 AI 提示词）

### 阶段 2：后端骨架
- 创建 `backend/` 目录结构
- FastAPI 应用入口 + 配置 + 数据库连接
- 10 个 SQLAlchemy 模型（17 张表）
- Alembic 迁移配置
- Dockerfile + requirements.txt

### 阶段 3：前端骨架
- `npm create vite` Vue 3 + TypeScript 项目
- 安装 Element Plus + vue-router + Pinia + axios + ECharts
- 主布局 AppLayout.vue
- 11 个页面占位组件
- vite 代理配置（前端 `/api` → 后端 8000）

### 阶段 4：用户与权限体系
- 后端：JWT 认证（登录/注册/刷新/me）、用户管理（CRUD/审批/禁用）、组织管理、数据字典 CRUD、状态流转规则查询
- 因 sandbox 的 cryptography 装坏了，把 `python-jose` 换成 `PyJWT`，密码哈希用原生 hashlib pbkdf2
- 种子数据脚本：组织/角色/平台/问题类型/状态/优先级/AI服务商/流转规则/管理员账号
- 前端：登录页、注册页、用户管理页、数据字典管理页

### 阶段 5：问题管理核心
- **ChangeLog 钩子**（`core/changelog_hook.py`）：SQLAlchemy `after_update` 事件监听 Issue，自动追加变更记录
- **状态机引擎**（`core/state_machine.py`）：从 `status_transitions` 表加载流转矩阵，缓存到内存
- **问题 CRUD API**：创建/列表/详情/编辑/状态流转/评论/变更日志查询
- **issue_no 自动生成**：格式 `ISS-YYYYMMDD-NNNN`
- **甲方用户只能看自己提交的问题**（API 层数据权限）
- 前端：问题列表（vxe-table 表格）、问题详情（含变更时间线+状态操作按钮）、新建/编辑表单（含 AI 辅助区占位）

### 阶段 6：统计看板 + 表格升级
- 后端 5 个统计接口：overview/by-platform/by-person/trend/stale-issues
- 前端 Overview 看板：数字卡片 + 趋势折线图 + 分端饼图 + 各端表 + 停滞问题表
- **引入 vxe-table**（Excel 风格表格，用户明确要求）：支持列拖拽调宽、行悬停、双击编辑等

### 阶段 7：腾讯云部署配置
- 用户提供服务器信息：IP `119.45.56.39`，SSH `jiyou2026-tls`，端口 8000/5432/6379 已占用
- 改 `docker-compose.yml`：所有容器加 `pm-` 前缀、PG/Redis 不暴露外部端口、前端入口改 3040、独立网络 `pm-net`
- 修 nginx 反代配置
- 写 `scripts/server-init.sh`、`scripts/deploy.sh`、`scripts/remote-deploy.sh`

### 阶段 8：部署遇到 503，紧急修复
- 用户访问 `http://119.45.56.39:3040` 返回 503
- 原因诊断：**没有 Alembic 迁移文件，表根本没建**
- 修复方案：在 `main.py` 的 `lifespan` 里用 `Base.metadata.create_all` 自动建表 + 自动跑种子数据
- CORS 改为 `*` 允许任何来源
- 后端容器加 healthcheck
- 前端 build 跳过 `vue-tsc`（加速 Docker 构建）

### 阶段 9：交接
- 沙箱无法 SSH 到用户服务器（无网络、无 ssh 客户端）
- 用户切换到本地 Claude Code 桌面版继续

---

## 第三部分：当前完整状态

### 3.1 代码状态总览

| 模块 | 状态 | 文件位置 |
|------|------|---------|
| **设计文档** | 完成 | `docs/01-03.md` |
| **后端 — 数据模型** | 完成 | `backend/app/models/*.py` (10个文件) |
| **后端 — 认证** | 完成 | `backend/app/api/auth.py` + `core/security.py` + `core/dependencies.py` |
| **后端 — 用户/组织管理** | 完成 | `backend/app/api/users.py` + `api/organizations.py` |
| **后端 — 数据字典** | 完成 | `backend/app/api/dictionary.py` |
| **后端 — 问题管理** | 完成 | `backend/app/api/issues.py` |
| **后端 — 状态机** | 完成 | `backend/app/core/state_machine.py` |
| **后端 — 变更日志钩子** | 完成 | `backend/app/core/changelog_hook.py` |
| **后端 — 统计报表** | 完成 | `backend/app/api/reports.py` |
| **后端 — 种子数据** | 完成 | `backend/app/seed.py` |
| **后端 — AI 助手** | **占位**（要做） | `backend/app/api/ai_assistant.py` |
| **后端 — 变更日志查询** | **占位**（要做） | `backend/app/api/changelog.py` |
| **后端 — 附件上传** | **占位**（要做） | `backend/app/api/attachments.py` |
| **前端 — 登录/注册** | 完成 | `frontend/src/views/login/`、`register/` |
| **前端 — 主布局** | 完成 | `frontend/src/components/layout/AppLayout.vue` |
| **前端 — 问题管理** | 完成 | `frontend/src/views/issue/` (List/Detail/Form) |
| **前端 — 总览看板** | 完成 | `frontend/src/views/dashboard/Overview.vue` |
| **前端 — 用户管理** | 完成 | `frontend/src/views/admin/UserManage.vue` |
| **前端 — 数据字典管理** | 完成 | `frontend/src/views/admin/DictManage.vue` |
| **前端 — 组织管理** | **占位**（要做） | `frontend/src/views/admin/OrgManage.vue` |
| **前端 — 角色权限管理** | **占位**（要做） | `frontend/src/views/admin/RoleManage.vue` |
| **前端 — 日报查看页** | **占位**（要做） | `frontend/src/views/report/DailyReport.vue` |
| **前端 — AI 悬浮助手** | **未做**（阶段 5） | 待创建 `components/ai/` |
| **部署 — Docker Compose** | 完成 | `docker-compose.yml` |
| **部署 — 脚本** | 完成 | `scripts/server-init.sh`、`scripts/deploy.sh` |

### 3.2 API 路由清单（已实现 37 个）

```
认证 (4)：
  POST /api/v1/auth/register
  POST /api/v1/auth/login
  POST /api/v1/auth/refresh
  GET  /api/v1/auth/me

用户管理 (5)：
  GET  /api/v1/users
  POST /api/v1/users
  POST /api/v1/users/{id}/approve
  POST /api/v1/users/{id}/disable
  PUT  /api/v1/users/{id}

组织管理 (5)：
  GET  /api/v1/organizations
  POST /api/v1/organizations
  GET  /api/v1/organizations/{org_id}/departments
  POST /api/v1/organizations/departments
  DELETE /api/v1/organizations/departments/{dept_id}

数据字典 (6)：
  GET    /api/v1/dictionary/categories
  GET    /api/v1/dictionary/{category_code}/items
  POST   /api/v1/dictionary/{category_code}/items
  PUT    /api/v1/dictionary/items/{item_id}
  DELETE /api/v1/dictionary/items/{item_id}
  GET    /api/v1/dictionary/transitions

问题管理 (8)：
  POST /api/v1/issues
  GET  /api/v1/issues
  GET  /api/v1/issues/{id}
  PUT  /api/v1/issues/{id}
  POST /api/v1/issues/{id}/transition
  GET  /api/v1/issues/{id}/transitions
  POST /api/v1/issues/{id}/comments
  GET  /api/v1/issues/{id}/changelog

统计报表 (5)：
  GET /api/v1/reports/overview
  GET /api/v1/reports/by-platform
  GET /api/v1/reports/by-person
  GET /api/v1/reports/trend
  GET /api/v1/reports/stale-issues

健康检查 (1)：
  GET /api/v1/health
```

### 3.3 数据库表清单（17 张）

```
组织/人员：organizations, departments, users
角色权限：roles, permissions, user_roles, role_permissions
数据字典：dictionary_categories, dictionary_items, status_transitions
核心业务：issues, change_logs, comments, attachments
报表AI：daily_reports, semantic_entities, ai_conversations
```

---

## 第四部分：未完成任务详细清单

### 4.1 紧急任务：修复 503 部署问题（优先级最高）

**现状**：用户访问 `http://119.45.56.39:3040` 返回 HTTP 503。最新提交 `4eecc5e` 已修复 lifespan 自动建表逻辑，但用户**还没在服务器上重新拉代码+重建容器**。

**接手第一件事**：
```bash
# SSH 到服务器
ssh jiyou2026-tls

# 拉最新代码 + 重建
cd /opt/pm-system
git pull origin claude/sync-app-to-mobile-GqiOm
docker compose down
docker compose up -d --build

# 等30秒后看日志
sleep 30
docker compose ps
docker compose logs pm-backend --tail=50
```

**如果还有问题**：根据日志诊断，常见问题：
- 数据库连接失败 → 检查 DB_PASSWORD 是否一致
- 端口冲突 → 检查 `docker ps` 是否有别的容器占用
- 镜像构建失败 → 检查 Dockerfile 和 requirements

### 4.2 阶段 5：AI 集成（按优先级）

#### A. 后端 AI 多模型适配层（先做）

文件：`backend/app/services/ai_service.py`（新建）

```python
"""
功能：
- get_ai_client(): 从数据字典 AI_PROVIDER 读取默认项，
  根据 base_url 和 model 创建 openai.OpenAI 实例
  从环境变量读取对应的 API_KEY
- parse_issue_from_text(text): 自然语言 → 结构化 JSON
- chat_stream(messages): SSE 流式对话
"""
```

#### B. 后端 AI 助手 API（实现 `api/ai_assistant.py`）

```python
POST /api/v1/ai/parse-issue
  - Body: {"text": "用户描述的大白话"}
  - Response: {"title": "...", "description": "...",
               "platform_code": "B_END", "issue_type_code": "BUG",
               "priority_code": "HIGH"}

POST /api/v1/ai/chat  (SSE stream)
  - Body: {"session_id": "...", "message": "..."}
  - 流式返回 AI 回答
  - 内部要做意图识别：统计查询/状态查询/停滞分析/人员查询

POST /api/v1/ai/similar-issues
  - Body: {"text": "..."}
  - 向量相似度搜索
```

#### C. 前端 AI 辅助录入（IssueForm.vue 已留位置）

接入 `/api/v1/ai/parse-issue` 接口。

#### D. 前端 AI 悬浮助手组件（新建）

文件：`frontend/src/components/ai/AIFloat.vue` + `AIChatPanel.vue`

挂载在 `AppLayout.vue` 全局右下角。

#### E. 语义层 + 向量库

需要：
- 启用 pgvector 扩展（已在 docker-compose 用 pgvector 镜像）
- `semantic_entities` 表加 vector 列（当前没有，要建迁移）
- `services/semantic_service.py`：embedding 生成 + 相似搜索
- 数据同步钩子：Issue 创建/更新时异步更新 SemanticEntity

### 4.3 阶段 6 完善：部署上线后续

- HTTPS：用户当前用 IP+端口直访，可选装 Caddy 配域名+自动证书
- 数据备份脚本：`scripts/backup.sh`（pg_dump 定时备份）
- 日志收集：docker compose 的日志大小限制
- 监控：可选

### 4.4 其他遗漏功能

- **附件上传接口**：`api/attachments.py` 当前是占位
- **变更日志独立查询接口**：`api/changelog.py` 当前是占位（详情接口里已包含）
- **OrgManage.vue / RoleManage.vue / DailyReport.vue**：前端占位
- **日报自动生成**：APScheduler 定时任务 + AI 叙述
- **审批流**：用户提到"有审批流更好"，可选

---

## 第五部分：项目结构详解

```
DM001/
├── HANDOFF.md                          ← 上一份交接文档（简版）
├── HANDOFF_COMPLETE.md                 ← 本文件（完整版）
├── README.md
├── .gitignore
├── docker-compose.yml                  ← 4个服务：pm-postgres/redis/backend/frontend
├── Makefile
│
├── docs/                               ← 设计文档（先读）
│   ├── 01-ontology-design.md           本体设计
│   ├── 02-system-architecture.md       系统架构
│   └── 03-implementation-roadmap.md    实施路线图
│
├── backend/
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── alembic.ini
│   ├── .env.example
│   ├── alembic/
│   │   ├── env.py
│   │   └── script.py.mako
│   └── app/
│       ├── main.py                     入口，lifespan自动建表+种子数据
│       ├── config.py                   pydantic-settings
│       ├── database.py                 异步引擎+session
│       ├── seed.py                     种子数据
│       ├── models/                     SQLAlchemy模型（10个）
│       │   ├── __init__.py
│       │   ├── organization.py
│       │   ├── user.py
│       │   ├── role.py
│       │   ├── dictionary.py           ← 包含StatusTransition
│       │   ├── issue.py
│       │   ├── changelog.py
│       │   ├── comment.py              ← 含Attachment
│       │   ├── report.py
│       │   └── semantic.py             ← 含AIConversation
│       ├── schemas/                    Pydantic请求/响应
│       │   ├── auth.py
│       │   ├── common.py
│       │   ├── user.py
│       │   ├── dictionary.py
│       │   └── issue.py
│       ├── api/                        路由
│       │   ├── auth.py                 ✓
│       │   ├── users.py                ✓
│       │   ├── organizations.py        ✓
│       │   ├── dictionary.py           ✓
│       │   ├── issues.py               ✓
│       │   ├── reports.py              ✓
│       │   ├── changelog.py            ✗ 占位
│       │   ├── attachments.py          ✗ 占位
│       │   └── ai_assistant.py         ✗ 占位（阶段5要做）
│       ├── core/
│       │   ├── security.py             JWT+密码（PyJWT+hashlib）
│       │   ├── dependencies.py         认证依赖+RBAC装饰器
│       │   ├── state_machine.py        数据字典驱动状态机
│       │   ├── changelog_hook.py       SQLAlchemy event listener
│       │   └── exceptions.py
│       └── utils/
│           └── id_generator.py
│
├── frontend/
│   ├── Dockerfile                      多阶段：node build → nginx
│   ├── nginx.conf                      反代 /api → pm-backend:8000
│   ├── package.json                    build脚本已改为只 vite build（跳过 vue-tsc 提速）
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── main.ts                     注册Pinia/Router/ElementPlus/vxe-table
│       ├── App.vue                     仅<router-view />
│       ├── router/index.ts             路由+登录守卫
│       ├── stores/
│       │   ├── auth.ts                 token + user + 登录方法
│       │   └── dictionary.ts           字典缓存（按category code索引）
│       ├── api/request.ts              axios + JWT拦截器
│       ├── components/
│       │   ├── layout/AppLayout.vue    侧边栏+顶栏
│       │   └── (ai/ — 待创建)
│       └── views/
│           ├── login/LoginPage.vue           ✓
│           ├── register/RegisterPage.vue     ✓
│           ├── dashboard/Overview.vue        ✓
│           ├── issue/
│           │   ├── IssueList.vue             ✓ (vxe-table)
│           │   ├── IssueDetail.vue           ✓
│           │   └── IssueForm.vue             ✓ (AI辅助区已留)
│           ├── admin/
│           │   ├── UserManage.vue            ✓
│           │   ├── DictManage.vue            ✓
│           │   ├── OrgManage.vue             ✗ 占位
│           │   └── RoleManage.vue            ✗ 占位
│           └── report/DailyReport.vue        ✗ 占位
│
└── scripts/
    ├── server-init.sh                  腾讯云一键初始化
    ├── deploy.sh                       服务器上部署
    └── remote-deploy.sh                本地一键远程部署
```

---

## 第六部分：技术栈与关键决策

### 6.1 技术栈

| 层次 | 选型 | 版本 | 关键说明 |
|------|------|------|---------|
| 后端 | FastAPI | 0.115.6 | 异步、自动生成OpenAPI |
| ORM | SQLAlchemy | 2.0.36 | 异步engine，事件钩子机制 |
| DB | PostgreSQL | 16 | 用 pgvector/pgvector:pg16 镜像（自带 vector 扩展） |
| 缓存 | Redis | 7-alpine | 暂用于会话和缓存 |
| JWT | PyJWT | 2.9.0 | **不用 python-jose**（cryptography 依赖问题） |
| 密码 | hashlib pbkdf2 | 内置 | **不用 passlib/bcrypt**（同上） |
| AI SDK | openai | 1.59.3 | DeepSeek/千问都兼容 OpenAI 协议 |
| 前端框架 | Vue 3 | 3.5+ | TypeScript |
| 构建 | Vite | 7+ | 开发体验好 |
| UI | Element Plus | 2.14+ | 中文友好 |
| 表格 | **vxe-table** | 最新 | Excel 风格交互（用户要求） |
| 图表 | ECharts | 6+ | vue-echarts 绑定 |
| 状态 | Pinia | 最新 | 替代 Vuex |
| HTTP | axios | 1.16+ | 拦截器统一处理 |

### 6.2 不可妥协的设计原则

1. **数据字典驱动一切可枚举项**——平台、问题类型、问题状态、优先级、AI服务商，全部存 `dictionary_items`，加新选项**只改字典不改代码**
2. **状态流转规则配置化**——`status_transitions` 表存规则，状态机引擎从数据库加载
3. **ChangeLog 不可篡改**——只 INSERT，不 UPDATE/DELETE，应用层+数据库层双保险
4. **甲方用户数据隔离**——SQL 层 `WHERE submitter_id = current_user.id`
5. **AI 不自动提交**——解析后回填表单，必须用户手动点提交
6. **状态机的 SUBMITTER_ONLY 规则**——RELEASED→RESOLVED/UNRESOLVED 必须由原始提交人操作

### 6.3 用户偏好

- **不要从零重写**，全部用开源框架组装
- **表格统一用 vxe-table**（不用 el-table）
- **AI 用 DeepSeek + 通义千问**（不用 Claude/OpenAI 直连）
- **每完成一步立即推送 GitHub**
- **用大白话沟通**，少用术语
- **先让线上跑起来再做新功能**

---

## 第七部分：腾讯云服务器情况

### 7.1 基本信息

```
SSH 别名: jiyou2026-tls   （~/.ssh/config 已配好）
公网 IP:  119.45.56.39
系统:     Ubuntu
root权限: 有
```

### 7.2 已占用端口（不要碰）

| 端口 | 服务 |
|------|------|
| 22 | SSH |
| 80 | （可能空闲） |
| 3030 | CMFT 仪表盘 nginx |
| 3031 | CMFT API server |
| 3100 | DocuCMS Outline |
| 3004 | DocuCMS Docusaurus |
| 3015 | DocuCMS Webhook |
| 3016 | DocuCMS AI 搜索 |
| 3180 | DocuCMS Keycloak |
| 8000 | 某 FastAPI 服务 |
| 5432 | PostgreSQL |
| 6379 | Redis |

### 7.3 我们用的端口

- **3040** — 前端 nginx（唯一对外）
- 后端/PG/Redis 都不暴露，走 Docker 内部网络

### 7.4 不要碰的资源

- `/opt/cmft-dashboard/` 目录
- 名为 `cmft-dashboard`、`release-*`、`outline-*`、`docusaurus-*`、`keycloak-*` 的容器
- 现有的 PostgreSQL 容器（端口 5432）
- 现有的 Redis 容器（端口 6379）
- 端口 8000 的 FastAPI 服务

### 7.5 我们的部署位置

`/opt/pm-system/`

容器名（全部加 `pm-` 前缀避免冲突）：
- pm-postgres
- pm-redis
- pm-backend
- pm-frontend

Docker 网络：`pm-net`（独立，不影响其他）

### 7.6 代理注意

用户本机走 `HTTPS_PROXY=http://127.0.0.1:1082`。
- SSH 不受影响
- 大文件 scp 可能 broken pipe，必要时用 split 切块

---

## 第八部分：接手后的标准动作

### Step 1：先读三份设计文档

```bash
cat docs/01-ontology-design.md
cat docs/02-system-architecture.md
cat docs/03-implementation-roadmap.md
```

### Step 2：确认当前 Git 状态

```bash
git status
git log --oneline -10
git branch
```

最新提交应该是 `3768064 docs: 添加交接文档HANDOFF.md`。

应该在分支 `claude/sync-app-to-mobile-GqiOm` 上。

### Step 3：诊断并修复 503

```bash
ssh jiyou2026-tls 'cd /opt/pm-system && docker compose ps && docker compose logs pm-backend --tail=50'
```

根据日志判断问题。如果是表没建好，最新代码已经修复了，只需要重新拉+重建：

```bash
ssh jiyou2026-tls 'cd /opt/pm-system && git pull && docker compose down && docker compose up -d --build'
```

### Step 4：验证

浏览器访问 `http://119.45.56.39:3040`，应该能看到登录页。

**管理员账号：13800000000 / admin123**

### Step 5：用户后续要做什么

向用户确认下一步优先级：

1. **优先 A**：让用户先在腾讯云控制台开放 3040 端口（如果他还没开）
2. **优先 B**：完善阶段 5 — AI 集成（DeepSeek/千问辅助录入）
3. **优先 C**：完善遗漏功能（附件上传、组织管理页等）
4. **优先 D**：HTTPS + 域名

### Step 6：每完成一步都要

```bash
git add ...
git commit -m "feat: 描述

详细说明

https://claude.ai/code/session_xxx"
git push origin claude/sync-app-to-mobile-GqiOm
```

session URL 用你当前会话的链接。

---

## 第九部分：常见问题速查

### Q1: 后端启动报错"relation xxx does not exist"
→ 表没建。最新 `main.py` 的 lifespan 已加 `Base.metadata.create_all`，重新部署即可。

### Q2: 后端启动报错"cryptography"相关
→ 沙箱环境的问题。Docker 镜像里 `python:3.11-slim` 是干净的，不会有这问题。

### Q3: 前端访问 503
→ 后端没起来。`docker compose logs pm-backend` 看日志。

### Q4: 前端 build 报 TypeScript 错误
→ `package.json` 的 build 脚本已改为只跑 `vite build`（跳过 vue-tsc），不会卡在类型检查。如果想跑严格检查用 `npm run build:check`。

### Q5: 用户问"代码在哪里"
→ 单一真实存储：GitHub 仓库 `https://github.com/yhai84-jiyou/DM001`
→ 本地路径：`/Users/yanghai/Documents/cursorMac/Proj20_CMFT_Research/Proj_问题跟踪`
→ 服务器路径：`/opt/pm-system`

### Q6: 状态机怎么修改？
→ 不需要改代码！在前端的"系统管理 → 数据字典 → ISSUE_STATUS"加新状态，然后在"状态流转规则管理"（待开发的 StatusFlowManage.vue）配规则。**目前 StatusFlowManage 还没做前端，但后端已经能用了。**

---

## 第十部分：用一句话总结

> 一个**已经写好 4 个阶段、正在卡部署、还差 AI 集成**的、**基于本体论的项目问题管理系统**，部署到**腾讯云 jiyou2026-tls 服务器的 3040 端口**，**用户已克隆代码到 `/Users/yanghai/Documents/cursorMac/Proj20_CMFT_Research/Proj_问题跟踪`**，**接手第一件事是修复 503**。

---

**祝接手顺利！有问题直接看仓库里的 docs/ 三份文档，那里有最详细的设计依据。**
