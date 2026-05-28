# 分阶段实施路线图 — AI 驱动开发指南

> 版本：1.0 | 日期：2026-05-28

---

## 总览

整个系统分为 **6 个阶段**，每个阶段包含若干步骤。每一步都附带了具体的 **AI 提示词**，你可以直接复制给 AI（Claude Code 或其他编程 AI），让它按步骤完成开发。

### 阶段依赖关系

```
阶段1：项目脚手架 ──▶ 阶段2：用户与权限 ──▶ 阶段3：问题管理核心
                                                     │
                                              ┌──────┴──────┐
                                              ▼             ▼
                                     阶段4：统计看板   阶段5：AI集成
                                              │             │
                                              └──────┬──────┘
                                                     ▼
                                            阶段6：部署上线
```

### 时间估算

| 阶段 | 预计工作量 | 累计 |
|------|-----------|------|
| 阶段1：项目脚手架 | 1-2天 | 2天 |
| 阶段2：用户与权限 | 2-3天 | 5天 |
| 阶段3：问题管理核心 | 3-5天 | 10天 |
| 阶段4：统计看板 | 2-3天 | 13天 |
| 阶段5：AI集成 | 3-4天 | 17天 |
| 阶段6：部署上线 | 1-2天 | 19天 |

---

## 开发规范（所有阶段通用）

在每次给 AI 下指令之前，先让 AI 阅读以下规范。你可以把这段话在每个新会话的第一条消息中发给 AI：

```
## 项目开发规范

请在开发本项目时严格遵守以下规范：

1. 【项目结构】后端代码在 backend/ 目录，前端代码在 frontend/ 目录，文档在 docs/ 目录
2. 【后端技术栈】Python 3.11+ / FastAPI / SQLAlchemy 2.0 / Alembic / PostgreSQL 16 + pgvector / Redis
3. 【前端技术栈】Vue 3 + TypeScript / Vite / Element Plus / Pinia / Axios / TipTap / ECharts
4. 【代码风格】后端用 Python type hints，前端用 TypeScript strict mode
5. 【API 规范】RESTful，路径格式 /api/v1/{resource}，统一返回格式 {"code": 0, "data": {}, "message": ""}
6. 【数据库】所有表用 UUID 主键，时间字段用 TIMESTAMPTZ，状态字段用 VARCHAR 存枚举字符串
7. 【变更日志】对 Issue 表的任何更新都必须通过 SQLAlchemy event listener 自动记录到 change_logs 表
8. 【认证】JWT Token，通过 FastAPI Depends 注入当前用户
9. 【权限】RBAC 模型，通过装饰器/依赖注入检查权限
10. 【Git】每完成一个独立功能点就提交一次，commit message 用中文描述改动内容

请先阅读 docs/ 目录下的三份设计文档，理解系统的本体模型和架构设计，然后再开始编码。
```

---

## 阶段 1：项目脚手架搭建

### 目标
搭建前后端项目骨架，确保能跑起来。

### 步骤 1.1 — 后端项目初始化

**给 AI 的提示词：**

```
请帮我初始化后端项目。在当前仓库的根目录下创建 backend/ 目录，搭建 FastAPI 项目骨架。

具体要求：
1. 创建 backend/app/ 目录结构：main.py, config.py, database.py, models/, schemas/, api/, services/, core/, utils/
2. main.py：创建 FastAPI 应用，配置 CORS（允许 localhost:5173），挂载 API 路由前缀 /api/v1
3. config.py：用 pydantic-settings 管理配置，从环境变量读取 DATABASE_URL, REDIS_URL, JWT_SECRET, CLAUDE_API_KEY
4. database.py：配置 SQLAlchemy 2.0 异步引擎和会话，提供 get_db 依赖
5. 创建 backend/requirements.txt，包含：fastapi, uvicorn[standard], sqlalchemy[asyncio], asyncpg, alembic, pydantic-settings, python-jose[cryptography], passlib[bcrypt], redis, httpx, python-multipart, anthropic
6. 创建 backend/.env.example 示例配置文件
7. 创建 backend/Dockerfile（基于 python:3.11-slim）
8. 初始化 Alembic：在 backend/ 下运行 alembic init，配置 alembic/env.py 使用异步引擎

确保 `cd backend && pip install -r requirements.txt && uvicorn app.main:app` 能启动成功。
```

### 步骤 1.2 — 前端项目初始化

**给 AI 的提示词：**

```
请帮我初始化前端项目。在当前仓库的根目录下创建 frontend/ 目录。

具体要求：
1. 用 Vite 创建 Vue 3 + TypeScript 项目：npm create vite@latest frontend -- --template vue-ts
2. 安装依赖：element-plus, @element-plus/icons-vue, vue-router@4, pinia, axios, @vueup/vue-quill 或 @tiptap/vue-3（富文本编辑器）, echarts, vue-echarts
3. 配置 Element Plus 按需导入
4. 创建基础目录结构：src/api/, src/views/, src/components/, src/stores/, src/router/, src/composables/, src/styles/
5. 创建主布局 AppLayout.vue：左侧边栏 + 顶部栏 + 主内容区（Element Plus 的 el-container 布局）
6. 配置 vue-router，先创建占位页面：/login, /dashboard, /issues, /admin
7. 配置 axios 实例（src/api/request.ts）：baseURL 指向 /api/v1，请求拦截器自动加 JWT token，响应拦截器统一处理错误
8. 创建 Pinia store 骨架：auth.ts
9. vite.config.ts 配置代理：/api → http://localhost:8000
10. 创建 frontend/Dockerfile（多阶段构建：先 npm build，再用 nginx 托管）

确保 `cd frontend && npm install && npm run dev` 能在 localhost:5173 显示带侧边栏的空布局。
```

### 步骤 1.3 — Docker Compose 编排

**给 AI 的提示词：**

```
请在仓库根目录创建 docker-compose.yml，编排以下服务：

1. postgres：使用 pgvector/pgvector:pg16 镜像，端口 5432，数据卷挂载到 ./data/db，环境变量设置 POSTGRES_DB=pm_system, POSTGRES_USER=pm_admin, POSTGRES_PASSWORD=changeme
2. redis：使用 redis:7-alpine，端口 6379
3. backend：构建 ./backend/Dockerfile，端口 8000，depends_on postgres 和 redis，环境变量从 .env 文件读取
4. frontend：构建 ./frontend/Dockerfile，端口 80
5. 创建 .env.example 包含所有需要的环境变量

同时在根目录创建 Makefile，包含常用命令：
- make dev：启动 postgres + redis（开发时前后端本地运行）
- make up：启动所有服务
- make down：停止所有服务
- make migrate：运行数据库迁移
- make seed：初始化种子数据
```

---

## 阶段 2：用户与权限体系

### 目标
实现组织架构、用户注册/登录/审批、角色权限管理。

### 步骤 2.1 — 数据模型

**给 AI 的提示词：**

```
请参考 docs/01-ontology-design.md 和 docs/02-system-architecture.md 中的数据库设计，创建以下 SQLAlchemy 模型：

1. backend/app/models/organization.py：Organization 和 Department 模型
2. backend/app/models/user.py：User 模型，包含 name, phone, email, password_hash, org_id, dept_id, user_type(CLIENT/VENDOR), vendor_role(PRODUCT/DEV/TEST/ADMIN), status(PENDING/ACTIVE/DISABLED)
3. backend/app/models/role.py：Role, Permission, UserRole(关联表), RolePermission(关联表)

所有模型：
- 使用 UUID 主键（mapped_column(UUID, primary_key=True, default=uuid4)）
- 时间字段用 DateTime(timezone=True)
- 定义好 relationship
- 在 models/__init__.py 中统一导出

然后用 Alembic 生成并执行迁移。
```

### 步骤 2.2 — 认证接口

**给 AI 的提示词：**

```
请实现用户认证相关功能：

1. backend/app/core/security.py：
   - create_access_token(user_id, expires_delta=2h)
   - create_refresh_token(user_id, expires_delta=7d)
   - verify_token(token) → user_id
   - hash_password / verify_password（bcrypt）

2. backend/app/api/auth.py 路由：
   - POST /api/v1/auth/register — 接收 name, phone, password，创建状态为 PENDING 的用户
   - POST /api/v1/auth/login — 手机号+密码登录，返回 access_token 和 refresh_token
   - POST /api/v1/auth/refresh — 刷新 token
   - GET /api/v1/auth/me — 获取当前用户信息

3. backend/app/core/permissions.py：
   - get_current_user 依赖：从 Authorization header 解析 JWT 获取当前用户
   - require_role(*roles) 依赖：检查用户角色
   - require_permission(resource, action) 依赖：检查用户权限

4. backend/app/schemas/auth.py：定义请求和响应的 Pydantic 模型
```

### 步骤 2.3 — 用户管理接口

**给 AI 的提示词：**

```
请实现用户和组织管理的后端接口：

1. backend/app/api/users.py：
   - GET /api/v1/users — 用户列表（分页、筛选：org_id, user_type, status）
   - GET /api/v1/users/{id} — 用户详情
   - PUT /api/v1/users/{id} — 编辑用户信息
   - POST /api/v1/users/{id}/approve — 审批用户（PENDING → ACTIVE）
   - POST /api/v1/users/{id}/disable — 禁用用户
   - POST /api/v1/users/batch-create — 批量创建用户（管理员后台开通账号）

2. backend/app/api/organizations.py：
   - CRUD for organizations（支持树形结构）
   - CRUD for departments（支持树形结构）

3. backend/app/api/roles.py：
   - CRUD for roles
   - 角色分配/取消分配给用户

4. backend/app/services/auth_service.py：封装业务逻辑

权限要求：
- 用户列表/详情：VENDOR_MANAGER 和 ADMIN 可看所有，普通用户只看自己
- 审批/禁用：仅 ADMIN
- 组织/部门/角色管理：仅 ADMIN
```

### 步骤 2.4 — 种子数据

**给 AI 的提示词：**

```
请创建种子数据脚本 backend/app/seed.py，初始化以下数据：

1. 组织：
   - 甲方组织："XX集团"（org_type=CLIENT）
   - 乙方组织："我方团队"（org_type=VENDOR）

2. 平台/端（platforms 表）：
   - C端（员工电商平台）, B端（企业采购平台）, 运营管理后台, 供应商协同门户, 企业自维护平台

3. 预置角色（roles 表）：
   - ADMIN, CLIENT_USER, CLIENT_MANAGER, VENDOR_PRODUCT, VENDOR_DEV, VENDOR_TEST, VENDOR_MANAGER
   - 每个角色配置对应的权限

4. 数据字典：
   - 分类"问题类型"下的项：待评估, 系统Bug, 设计缺陷, 功能优化, 咨询答疑, 外部系统问题
   - 分类"优先级"下的项：紧急, 高, 中, 低

5. 管理员账号：admin / 手机号 13800000000 / 密码 admin123（ADMIN 角色）

在 main.py 启动时检查数据库是否为空，如果为空则自动执行种子数据。
也可以通过命令 `python -m app.seed` 手动执行。
```

### 步骤 2.5 — 前端登录和用户管理页面

**给 AI 的提示词：**

```
请实现前端的用户体系页面：

1. src/views/login/LoginPage.vue：
   - 手机号 + 密码登录表单
   - 登录成功后存 token 到 localStorage，跳转 /dashboard
   - 底部有"申请账号"链接

2. src/views/register/RegisterPage.vue：
   - 姓名 + 手机号 + 密码 表单
   - 提交后提示"申请已提交，等待管理员审批"

3. src/stores/auth.ts（Pinia）：
   - state: user, token, isLoggedIn
   - actions: login, logout, fetchMe, refreshToken
   - 自动从 localStorage 恢复 token

4. src/router/index.ts：
   - 路由守卫：未登录跳 /login，已登录访问 /login 跳 /dashboard

5. src/views/admin/UserManage.vue：
   - 用户列表表格（Element Plus el-table）：姓名、手机、组织、角色、状态、操作
   - 筛选：按组织、用户类型、状态筛选
   - 操作：审批（待审核用户）、禁用、编辑
   - 新建用户对话框

6. src/views/admin/OrgManage.vue：
   - 树形展示组织和部门
   - 支持增删改

7. src/views/admin/DictManage.vue：
   - 左侧字典分类列表，右侧字典项列表
   - 支持增删改（系统内置项不可删除）

使用 Element Plus 组件，保持界面简洁专业。
```

---

## 阶段 3：问题管理核心

### 目标
实现问题的创建、编辑、状态流转、变更日志 —— 这是整个系统的核心。

### 步骤 3.1 — 问题数据模型与变更日志钩子

**给 AI 的提示词：**

```
请实现问题管理的核心数据层：

1. backend/app/models/issue.py：Issue 模型，严格按照 docs/01-ontology-design.md 中的 Issue 实体定义
2. backend/app/models/changelog.py：ChangeLog 模型
3. backend/app/models/comment.py：Comment 和 Attachment 模型

4. backend/app/core/changelog_hook.py — 这是最关键的部分：
   利用 SQLAlchemy 的 event.listens_for 机制，监听 Issue 模型的 after_update 事件。
   当 Issue 的任何字段被修改时，自动在 change_logs 表中插入一条记录，包含：
   - entity_type: 'issue'
   - entity_id: issue.id
   - field_name: 被修改的字段名
   - old_value: 修改前的值（转为字符串）
   - new_value: 修改后的值（转为字符串）
   - old_display: 修改前的显示文本（如状态名称而非编码）
   - new_display: 修改后的显示文本
   - changed_by: 当前操作用户ID（通过 contextvars 传递）
   - changed_at: 当前时间
   - change_source: 'MANUAL'

   实现要点：
   - 使用 Python contextvars 在请求级别传递 current_user_id
   - 在 API 层的中间件中设置 contextvars
   - 每个变更字段生成一条独立的 ChangeLog 记录
   - ChangeLog 表在应用层禁止 UPDATE 和 DELETE 操作

5. 生成 Alembic 迁移并执行。
```

### 步骤 3.2 — 状态机引擎

**给 AI 的提示词：**

```
请实现问题状态机：

backend/app/core/state_machine.py：

1. 定义所有状态枚举（参考 docs/01-ontology-design.md 第四章）：
   SUBMITTED, EVALUATING, PENDING, DEV_IN_PROGRESS, PRODUCT_FOLLOW_UP,
   TESTING, TEST_DONE_PENDING_VERIFY, VERIFIED_PENDING_RELEASE,
   RELEASED, RESOLVED, UNRESOLVED

2. 定义状态流转矩阵 TRANSITIONS：一个字典，key 是当前状态，value 是
   {目标状态: [允许的角色列表]} 的字典。
   特殊处理：RELEASED → RESOLVED/UNRESOLVED 时，角色检查改为"必须是该问题的 submitter_id"

3. 实现 transition(issue, target_status, current_user) 函数：
   - 校验当前状态是否可流转到目标状态
   - 校验当前用户是否有权执行此流转
   - 对于 RESOLVED/UNRESOLVED，校验 current_user.id == issue.submitter_id
   - 校验通过则更新 issue.status
   - 如果流转到 RESOLVED，自动设置 resolved_at 和 closed_by、closed_at
   - 如果流转到 UNRESOLVED，自动清空 resolved_at
   - 校验失败抛出自定义异常 StateTransitionError

4. 实现 get_available_transitions(issue, current_user) 函数：
   返回当前用户对当前问题可以执行的所有目标状态列表（前端用来显示可操作的按钮）

5. 编写单元测试 backend/tests/test_state_machine.py，覆盖：
   - 正常流转路径
   - 非法流转被拒绝
   - 角色权限校验
   - 提交人关闭权限校验
```

### 步骤 3.3 — 问题 CRUD API

**给 AI 的提示词：**

```
请实现问题管理的后端 API：

backend/app/api/issues.py：

1. POST /api/v1/issues — 创建问题
   - 甲方用户可以创建
   - 自动生成 issue_no（格式 ISS-YYYYMMDD-NNNN）
   - 自动设置 submitter_id 和 submitted_at
   - 初始状态为 SUBMITTED

2. GET /api/v1/issues — 问题列表
   - 分页（page, page_size）
   - 筛选：platform_id, issue_type_id, status, priority, submitter_id, product_owner_id, dev_owner_id, test_owner_id, date_range
   - 排序：submitted_at, updated_at, priority
   - 甲方用户只能看到自己组织提交的问题
   - 返回数据包含关联的 submitter、owner 等人员信息

3. GET /api/v1/issues/{id} — 问题详情
   - 返回完整问题信息 + 评论列表 + 变更日志列表（按时间倒序）

4. PUT /api/v1/issues/{id} — 编辑问题
   - 修改字段后自动触发 ChangeLog（通过步骤3.1的钩子）
   - 权限：管理员和乙方管理者可编辑所有字段，对应角色可编辑自己负责的字段

5. POST /api/v1/issues/{id}/transition — 状态流转
   - 请求体 {"target_status": "DEV_IN_PROGRESS"}
   - 调用状态机引擎校验并执行流转

6. GET /api/v1/issues/{id}/transitions — 获取可用的状态流转
   - 返回当前用户对此问题可执行的目标状态列表

7. POST /api/v1/issues/{id}/comments — 添加评论
8. POST /api/v1/issues/{id}/attachments — 上传附件（图片）

9. GET /api/v1/issues/{id}/changelog — 获取变更日志（单独接口，支持分页）

backend/app/services/issue_service.py：封装业务逻辑
backend/app/schemas/issue.py：请求/响应 Pydantic 模型
```

### 步骤 3.4 — 前端问题管理页面

**给 AI 的提示词：**

```
请实现前端的问题管理页面：

1. src/views/issue/IssueList.vue — 问题列表页：
   - 顶部筛选栏：平台（下拉）、问题类型（下拉）、状态（多选）、优先级（下拉）、日期范围（日期选择器）、关键词搜索
   - Element Plus el-table 展示列表：问题编号、标题、平台、类型、状态（彩色标签）、优先级、提交人、提交时间、产品/研发/测试跟进人
   - 分页组件
   - 点击行跳转详情页
   - 右上角"新建问题"按钮

2. src/views/issue/IssueForm.vue — 新建/编辑问题页：
   - 表单字段：标题（输入框）、详细描述（富文本编辑器，支持粘贴图片）、平台（下拉选择）、问题类型（下拉选择）、优先级（下拉选择）
   - 编辑模式下额外显示：产品跟进人、研发跟进人、测试跟进人（用户选择器）
   - 【重要】表单上方有一个"AI 辅助填写"区域：一个大文本框 + "AI 解析"按钮。用户输入大白话，点击按钮调用 AI 解析接口，解析结果自动回填到下方表单各字段，用户核对后手动点"提交"

3. src/views/issue/IssueDetail.vue — 问题详情页：
   - 顶部：问题编号、标题、状态标签
   - 左侧主区域：
     - 基本信息卡片（平台、类型、优先级、提交人、提交时间）
     - 详细描述（富文本渲染）
     - 评论区（时间线形式，底部有输入框可添加评论）
   - 右侧边栏：
     - 跟进人信息卡片（产品/研发/测试跟进人）
     - 状态流转操作区：根据 /transitions 接口返回的可用状态，显示对应的操作按钮
   - 底部标签页：
     - "变更记录" tab：时间线展示所有 ChangeLog，格式如"张三 于 2026-05-28 14:30 将 状态 从 待处理 改为 研发跟进中"

4. src/components/issue/StatusBadge.vue — 状态标签组件（不同状态不同颜色）
5. src/components/issue/ChangeTimeline.vue — 变更日志时间线组件
```

---

## 阶段 4：统计看板

### 目标
实现多维度的统计分析看板。

### 步骤 4.1 — 统计后端接口

**给 AI 的提示词：**

```
请实现统计看板的后端 API：

backend/app/api/reports.py：

1. GET /api/v1/reports/overview — 总览数据
   返回：
   {
     "today_new": 5,
     "today_closed": 3,
     "today_status_changed": 8,
     "total_open": 42,
     "total_stale": 6,         // 超过2天无变更的未关闭问题
     "avg_resolve_days": 4.2
   }

2. GET /api/v1/reports/by-platform — 分端统计
   参数：date_from, date_to
   返回每个平台的：问题总数、未关闭数、本周新增、本周关闭、按类型分布、按状态分布

3. GET /api/v1/reports/by-person — 分人统计
   参数：date_from, date_to, role_type(product/dev/test)
   返回每个跟进人的：名下总数、各状态数量、停滞问题数

4. GET /api/v1/reports/trend — 趋势数据
   参数：date_from, date_to, granularity(day/week)
   返回每天/每周的：新增数、关闭数、累计未关闭数

5. GET /api/v1/reports/person-daily — 个人每日变化
   参数：user_id, date_from, date_to
   返回该人员每天名下问题的变化情况（新增、关闭、净增）

6. GET /api/v1/reports/stale-issues — 停滞问题列表
   返回超过 N 天（默认2天）没有任何 ChangeLog 的未关闭问题列表

backend/app/services/report_service.py：
- 统计逻辑主要通过 SQL 聚合查询实现
- 停滞问题检测：left join change_logs，找 last_change_at < now() - interval N days 的 issue
```

### 步骤 4.2 — 前端统计看板页面

**给 AI 的提示词：**

```
请实现前端统计看板页面：

1. src/views/dashboard/Overview.vue — 总览看板：
   - 顶部一行数字卡片（el-card）：今日新增、今日关闭、今日变化、待处理总数、停滞问题数、平均解决天数
   - 中间：问题趋势折线图（ECharts，过去30天每日新增/关闭）
   - 下方左：按状态分布的饼图
   - 下方右：按平台分布的柱状图

2. src/views/dashboard/PlatformView.vue — 分端看板：
   - 顶部平台标签页切换
   - 每个平台展示：数字卡片（总数/未关闭/本周新增/本周关闭）+ 类型分布饼图 + 状态分布柱状图

3. src/views/dashboard/PersonView.vue — 分人看板：
   - 筛选：角色类型（产品/研发/测试）
   - 表格展示：人员姓名、名下总数、各状态问题数、停滞问题数
   - 停滞问题数 > 0 的行用红色高亮
   - 点击人员名称展开：该人员每日问题变化折线图

4. src/views/dashboard/TrendView.vue — 趋势分析：
   - 日期范围选择器
   - 新增/关闭趋势对比折线图
   - 累计未关闭趋势线
   - 分端趋势对比（多条折线）

5. 甲方用户看到的简化看板（根据 user_type 判断）：
   - 我提交的问题状态分布
   - 待我验证的问题列表（状态=RELEASED，submitter=me）
   - 我的组织提交的问题整体进展

使用 ECharts 绑定 vue-echarts，图表支持响应式，配色使用 Element Plus 的主题色。
```

### 步骤 4.3 — 日报自动生成

**给 AI 的提示词：**

```
请实现日报自动生成功能：

1. backend/app/models/report.py：DailyReport 模型

2. backend/app/services/scheduler_service.py：
   - 使用 APScheduler 配置定时任务
   - 在 FastAPI 的 lifespan 中启动调度器
   - 每日 01:00 执行日报生成任务

3. backend/app/services/report_service.py 中添加 generate_daily_report() 方法：
   - 计算前一日的所有统计数据（新增、关闭、状态变化、分端、分人等）
   - 将统计数据组装成 JSON
   - 调用 Claude API，prompt 如下：
     "你是一个项目管理日报助手。以下是昨日的问题跟踪数据统计：{json_data}。
      请生成一份简洁的项目日报，包括：1）整体概况 2）重点关注（停滞问题、高优问题）
      3）各端情况简述 4）建议关注事项。控制在300字以内。"
   - 将统计 JSON + AI 生成的文字存入 daily_reports 表

4. backend/app/api/reports.py 中添加：
   - GET /api/v1/reports/daily — 获取日报列表（按日期倒序）
   - GET /api/v1/reports/daily/{date} — 获取指定日期的日报
   - POST /api/v1/reports/daily/generate — 手动触发生成（仅管理员）

5. 前端 src/views/report/DailyReport.vue：
   - 左侧日期列表，右侧展示选中日期的日报内容
   - 上方显示结构化统计数据（数字卡片+小图表）
   - 下方显示 AI 生成的文字叙述
```

---

## 阶段 5：AI 集成

### 目标
实现 AI 辅助录入、智能助手对话、语义层构建。

### 步骤 5.1 — AI 辅助录入

**给 AI 的提示词：**

```
请实现 AI 辅助填写问题表单的功能：

1. backend/app/services/ai_service.py 中实现 parse_issue_from_text(text: str) 方法：
   - 从数据库加载当前可用的数据字典（问题类型列表、平台列表、优先级列表）
   - 构建 System Prompt：
     "你是一个项目问题解析助手。用户会用自然语言描述一个问题，你需要从中提取结构化信息。
      
      可选的平台：{platforms}
      可选的问题类型：{issue_types}
      可选的优先级：紧急/高/中/低
      
      请从用户描述中提取以下字段，返回 JSON 格式：
      {
        "title": "一句话标题（不超过50字）",
        "description": "详细描述（保留用户原始信息，可以补充结构化）",
        "platform_code": "平台编码",
        "issue_type_code": "问题类型编码",
        "priority": "优先级"
      }
      
      如果某个字段无法从用户描述中判断，对应值设为 null。
      只返回 JSON，不要其他说明文字。"
   - 调用 Claude API（使用用户配置的 API Key）
   - 解析返回的 JSON 并返回

2. backend/app/api/ai_assistant.py：
   - POST /api/v1/ai/parse-issue — 接收 {"text": "用户输入的大白话"}，返回解析后的结构化数据

3. 前端 src/views/issue/IssueForm.vue 中的 AI 辅助区域：
   - 文本框（el-input type="textarea" rows=4）+ "AI 智能解析"按钮
   - 点击按钮后 loading 状态，调用接口
   - 接口返回后，将各字段值回填到表单（title → 标题输入框，platform_code → 平台下拉 等）
   - 回填时用高亮动画提示用户哪些字段被 AI 填写了
   - 用户可以修改任何 AI 填写的字段
   - 最终提交动作必须由用户手动点击"提交"按钮

注意：AI 解析只是辅助填写，不会自动提交。这是硬性规则。
```

### 步骤 5.2 — AI 悬浮助手

**给 AI 的提示词：**

```
请实现右下角的 AI 悬浮助手功能：

1. backend/app/services/ai_service.py 中实现 chat(session_id, user_id, message) 方法：
   - 分析用户意图（通过 Claude API）：
     a) 统计查询类（如"B端有多少Bug"）→ 生成 SQL 查询 → 执行 → 将结果传给 Claude 生成回答
     b) 状态查询类（如"ISS-0012 什么状态"）→ 查询 Issue → 格式化回答
     c) 停滞分析类（如"哪些问题卡住了"）→ 查询停滞问题 → 生成分析
     d) 人员查询类（如"张三手上有多少问题"）→ 聚合查询 → 回答
     e) 通用问答 → 直接 Claude 回答

   - 实现方式：
     Step 1: 用 Claude 做意图识别 + 生成查询参数
     System Prompt 包含数据库表结构摘要和可用的查询函数列表
     Step 2: 根据意图调用对应的 Service 方法获取数据
     Step 3: 将数据 + 用户问题传给 Claude 生成最终回答

   - 保存对话记录到 ai_conversations 表

2. backend/app/api/ai_assistant.py：
   - POST /api/v1/ai/chat — 接收 {"session_id": "...", "message": "..."}
   - 使用 SSE (Server-Sent Events) 流式返回 AI 回答

3. 前端组件：
   - src/components/ai/AIFloat.vue — 右下角悬浮按钮（圆形，带 AI 图标）
   - src/components/ai/AIChatPanel.vue — 点击悬浮按钮展开的对话面板：
     - 顶部标题栏"AI 助手" + 关闭按钮
     - 中间消息列表（用户消息靠右，AI 消息靠左）
     - AI 消息支持 Markdown 渲染
     - 底部输入框 + 发送按钮
     - 支持 SSE 流式显示 AI 回答（打字机效果）
   - 在 App.vue 或 AppLayout.vue 中全局挂载 AIFloat 组件

4. 对话面板支持显示"引用"：当 AI 回答中涉及具体问题时，显示可点击的问题编号链接
```

### 步骤 5.3 — 语义层与向量库

**给 AI 的提示词：**

```
请实现语义层和向量搜索功能：

1. 确保 PostgreSQL 启用了 pgvector 扩展（在迁移中加入 CREATE EXTENSION IF NOT EXISTS vector）

2. backend/app/services/semantic_service.py：

   a) build_issue_summary(issue) 方法：
      将一个 Issue 对象转化为自然语言摘要文本，格式：
      "问题{issue_no}，{platform}的{issue_type}问题。标题：{title}。
       提交人：{submitter}，{submitted_at}。当前状态：{status}。
       产品跟进：{product_owner}，研发跟进：{dev_owner}，测试跟进：{test_owner}。
       最后更新：{updated_at}。"

   b) generate_embedding(text) 方法：
      调用 Claude/OpenAI 的 Embedding API 将文本转为向量
      （如果用 Claude，可以用 Voyage AI 的 embedding；或者用开源的 sentence-transformers 本地生成）

   c) upsert_semantic_entity(entity_type, entity_id, summary_text) 方法：
      生成 embedding → 存入/更新 semantic_entities 表

   d) search_similar(query_text, top_k=5) 方法：
      将查询文本转为向量 → 在 semantic_entities 表中做 cosine 相似搜索 → 返回最相似的实体

   e) sync_all_issues() 方法：
      批量为所有 Issue 生成语义实体（初始化用）

3. 语义实体自动同步：
   - 在 Issue 创建时，异步调用 upsert_semantic_entity
   - 在 Issue 关键字段（title, description, status, owner）变更时，异步更新语义实体
   - 使用 Python asyncio.create_task 实现异步，不阻塞主请求

4. 在 AI 助手的 chat 方法中集成向量搜索：
   - 当用户问题涉及"类似问题"、"相关问题"时，先做向量搜索找到相似 Issue
   - 将搜索结果作为上下文传给 Claude 生成回答

5. backend/app/api/ai_assistant.py 添加：
   - POST /api/v1/ai/similar-issues — 接收 {"text": "..."}，返回语义最相似的问题列表
```

---

## 阶段 6：部署上线

### 目标
在你的服务器上部署整套系统。

### 步骤 6.1 — 生产环境配置

**给 AI 的提示词：**

```
请帮我准备生产环境部署配置：

1. 创建 docker-compose.prod.yml（基于 docker-compose.yml 修改）：
   - postgres 和 redis 不对外暴露端口
   - backend 使用 gunicorn + uvicorn workers 启动
   - nginx 配置 HTTPS（使用 certbot/let's encrypt）
   - 所有服务设置 restart: always
   - 日志配置：json-file driver，max-size 10m，max-file 3

2. nginx/nginx.prod.conf：
   - 监听 80 和 443
   - HTTP 自动跳转 HTTPS
   - /api/ 代理到 backend:8000
   - / 服务前端静态文件
   - 配置 gzip 压缩
   - 上传文件大小限制 20MB

3. backend/gunicorn.conf.py：
   - workers = 4
   - worker_class = "uvicorn.workers.UvicornWorker"
   - bind = "0.0.0.0:8000"

4. 创建 scripts/deploy.sh 部署脚本：
   - git pull
   - docker-compose -f docker-compose.prod.yml build
   - docker-compose -f docker-compose.prod.yml up -d
   - docker-compose exec backend alembic upgrade head
   - 健康检查

5. 创建 scripts/backup.sh 数据库备份脚本：
   - pg_dump 导出
   - 保留最近 7 天的备份
   - 可通过 crontab 定时执行

6. 更新 .env.example，标注每个环境变量的含义和生产建议值
```

### 步骤 6.2 — 数据初始化与测试

**给 AI 的提示词：**

```
请帮我准备上线前的数据初始化和测试：

1. 完善 backend/app/seed.py，确保执行后系统可用：
   - 甲方组织和乙方组织
   - 各平台数据
   - 数据字典（问题类型、优先级）
   - 预置角色和权限
   - 管理员账号

2. 创建 backend/tests/ 下的关键测试：
   - test_auth.py：注册、登录、token刷新
   - test_issues.py：创建、编辑、状态流转
   - test_changelog.py：变更日志自动记录
   - test_state_machine.py：状态机规则
   - test_permissions.py：权限校验

3. 创建 scripts/init-production.sh：
   首次部署执行：
   - 等待 PostgreSQL 就绪
   - 执行 Alembic 迁移
   - 执行种子数据
   - 创建管理员账号（交互式输入密码）

4. 在 README.md 中更新部署说明
```

---

## 附录 A：每个阶段完成后的验收清单

### 阶段 1 验收
- [ ] `docker-compose up` 后 PostgreSQL、Redis 正常运行
- [ ] 后端 `http://localhost:8000/docs` 可访问 Swagger 文档
- [ ] 前端 `http://localhost:5173` 显示带侧边栏的空布局

### 阶段 2 验收
- [ ] 能注册新用户（状态为 PENDING）
- [ ] 管理员能审批用户
- [ ] 登录后能获取 JWT token
- [ ] 组织、部门、角色的 CRUD 正常
- [ ] 数据字典管理页面可增删改
- [ ] 不同角色访问受限接口返回 403

### 阶段 3 验收
- [ ] 甲方用户能创建问题（自动生成编号、记录提交人和时间）
- [ ] 问题列表支持筛选和分页
- [ ] 问题详情页展示完整信息
- [ ] 状态流转按钮根据角色和状态机正确显示
- [ ] 每次修改都自动生成 ChangeLog
- [ ] 变更日志时间线正确展示"谁在什么时候把什么改成了什么"
- [ ] 只有提交人能关闭问题
- [ ] ChangeLog 记录不可被修改或删除

### 阶段 4 验收
- [ ] 总览看板数字准确
- [ ] 分端看板数据正确
- [ ] 分人看板展示各人负荷和停滞情况
- [ ] 趋势图表正确渲染
- [ ] 日报能自动生成且 AI 叙述通顺
- [ ] 甲方用户看到简化版看板

### 阶段 5 验收
- [ ] AI 辅助录入能正确解析自然语言为结构化字段
- [ ] AI 解析结果自动回填表单但不自动提交
- [ ] AI 悬浮助手能回答统计类问题
- [ ] AI 助手能查询具体问题的状态
- [ ] AI 助手能识别停滞问题
- [ ] 向量搜索能找到语义相似的问题

### 阶段 6 验收
- [ ] 服务器上 docker-compose 一键启动
- [ ] HTTPS 正常访问
- [ ] 管理员账号可登录
- [ ] 全功能链路测试通过
- [ ] 数据库备份脚本可执行

---

## 附录 B：开源替代方案参考

如果想从现有开源项目改造而非从零搭建，以下项目可作为基础：

| 项目 | 地址 | 优势 | 劣势 |
|------|------|------|------|
| Plane | github.com/makeplane/plane | 现代UI、Issue管理完善 | Python后端+React前端，改造成本中等 |
| Taiga | github.com/taigaio/taiga | 功能完整的项目管理 | 技术栈较老 |
| Redmine | github.com/redmine/redmine | 成熟稳定、插件丰富 | Ruby on Rails，不利于AI集成 |
| Django-vue-admin | 多个开源项目 | Python+Vue，中文社区 | 功能基础，需大量定制 |

**建议**：鉴于你的核心需求（变更日志、AI 集成、语义层）都是高度定制的，从零搭建（阶段1-6的路径）反而比改造开源项目更可控。开源项目的价值在于参考它们的 UI 设计和交互模式。
