# 系统架构设计文档

> 版本：1.0 | 日期：2026-05-28

---

## 一、架构设计原则

1. **全开源栈**：所有组件均采用开源方案，零许可费用
2. **单体优先**：第一期采用模块化单体架构，不搞微服务，降低部署和运维复杂度
3. **API-First**：前后端分离，所有功能通过 RESTful API 暴露
4. **变更可溯**：所有业务数据的修改都生成不可篡改的变更日志
5. **AI-Native**：AI 不是附加功能，而是从数据模型层就为 AI 理解而设计

---

## 二、技术选型

### 2.1 技术栈总览

| 层次 | 技术 | 版本 | 选择理由 |
|------|------|------|----------|
| **后端框架** | FastAPI (Python) | 0.110+ | 异步高性能、自动生成API文档、Python AI生态丰富 |
| **ORM** | SQLAlchemy 2.0 | 2.0+ | Python 最成熟的 ORM，支持事件监听（用于自动生成ChangeLog） |
| **数据库迁移** | Alembic | 1.13+ | SQLAlchemy 官方迁移工具 |
| **数据库** | PostgreSQL | 16+ | 支持 JSON、全文搜索、pgvector 扩展 |
| **向量库** | pgvector (PostgreSQL扩展) | 0.7+ | 无需额外部署，直接在 PG 中存储和检索向量 |
| **缓存** | Redis | 7+ | 会话管理、缓存、统计计数 |
| **前端框架** | Vue 3 + TypeScript | 3.4+ | 渐进式框架，中文社区活跃 |
| **UI组件库** | Element Plus | 2.7+ | 企业后台最成熟的 Vue 组件库 |
| **前端构建** | Vite | 5+ | 开发体验好，构建快 |
| **富文本编辑** | TipTap | 2.x | 基于 ProseMirror，支持图片、协作 |
| **图表** | ECharts | 5+ | Apache 开源，中文友好 |
| **AI SDK** | Anthropic Python SDK | 最新 | 调用 Claude API |
| **任务调度** | APScheduler | 3.10+ | 定时生成日报/周报 |
| **文件存储** | 本地磁盘 + MinIO（可选） | - | 初期本地存储，后续可切 MinIO |
| **认证** | JWT (python-jose) | - | 轻量，前后端分离友好 |
| **部署** | Docker + Docker Compose | - | 一键部署整套环境 |
| **反向代理** | Nginx | 1.25+ | 静态资源服务 + API 反代 |

### 2.2 为什么不用 Django

Django 自带 Admin、Auth、ORM，看似省事，但：
- Django ORM 对异步支持弱于 SQLAlchemy
- Django Admin 做简单管理可以，做复杂定制 UI 反而束缚大
- FastAPI + SQLAlchemy 的事件钩子更适合实现"自动 ChangeLog"
- FastAPI 的依赖注入和类型系统对 AI 工具链更友好

### 2.3 为什么用 pgvector 而不是独立向量库

- 避免多引入一个组件（Milvus/Chroma），降低运维负担
- pgvector 对于 10 万级向量完全够用（项目问题不会有百万级）
- 可以在一条 SQL 中同时做结构化过滤和向量相似搜索

---

## 三、系统分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Nginx (反向代理)                          │
│               静态资源 / API 路由 / HTTPS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    前端 (Vue 3 SPA)                        │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐  │  │
│  │  │ 问题管理  │ │ 统计看板  │ │ 系统管理  │ │ AI 助手悬浮 │  │  │
│  │  │ 模块     │ │ 模块     │ │ 模块     │ │ 模块        │  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └─────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                            │ HTTP/REST                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                   后端 API (FastAPI)                       │  │
│  │                                                           │  │
│  │  ┌─────────────── API Layer ───────────────────────────┐  │  │
│  │  │ auth_router │ issue_router │ report_router │ ai_rtr │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                         │                                 │  │
│  │  ┌─────────────── Service Layer ───────────────────────┐  │  │
│  │  │ AuthService │ IssueService │ ReportService │AISvc   │  │  │
│  │  │             │ (含状态机)    │ (含调度)       │        │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                         │                                 │  │
│  │  ┌─────────────── Domain Layer ────────────────────────┐  │  │
│  │  │ Models(SQLAlchemy) │ StateMachine │ ChangeLogHook   │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                         │                                 │  │
│  │  ┌─────────────── Infrastructure Layer ────────────────┐  │  │
│  │  │ PostgreSQL │ pgvector │ Redis │ FileStorage │Claude  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、后端模块划分

```
backend/
├── app/
│   ├── main.py                  # FastAPI 应用入口
│   ├── config.py                # 配置管理
│   ├── database.py              # 数据库连接
│   │
│   ├── models/                  # SQLAlchemy 模型
│   │   ├── __init__.py
│   │   ├── organization.py      # Organization, Department
│   │   ├── user.py              # User, Role, Permission
│   │   ├── issue.py             # Issue, IssueType, IssueStatus
│   │   ├── changelog.py         # ChangeLog
│   │   ├── comment.py           # Comment, Attachment
│   │   ├── dictionary.py        # DictionaryCategory, DictionaryItem
│   │   ├── report.py            # DailyReport
│   │   └── semantic.py          # SemanticEntity, AIConversation
│   │
│   ├── schemas/                 # Pydantic 请求/响应模型
│   │   ├── __init__.py
│   │   ├── auth.py
│   │   ├── issue.py
│   │   ├── user.py
│   │   ├── report.py
│   │   └── ai.py
│   │
│   ├── api/                     # API 路由
│   │   ├── __init__.py
│   │   ├── auth.py              # 登录/注册/Token
│   │   ├── users.py             # 用户管理
│   │   ├── organizations.py     # 组织/部门管理
│   │   ├── issues.py            # 问题 CRUD + 状态流转
│   │   ├── dictionary.py        # 数据字典管理
│   │   ├── reports.py           # 统计/看板数据
│   │   ├── changelog.py         # 变更日志查询
│   │   ├── attachments.py       # 文件上传
│   │   └── ai_assistant.py      # AI 助手接口
│   │
│   ├── services/                # 业务逻辑
│   │   ├── __init__.py
│   │   ├── auth_service.py
│   │   ├── issue_service.py     # 含状态机逻辑
│   │   ├── changelog_service.py # 变更日志核心逻辑
│   │   ├── report_service.py    # 统计计算 + 日报生成
│   │   ├── ai_service.py        # Claude API 调用
│   │   ├── semantic_service.py  # 语义层 + 向量操作
│   │   └── scheduler_service.py # 定时任务
│   │
│   ├── core/                    # 核心基础设施
│   │   ├── __init__.py
│   │   ├── security.py          # JWT 生成/验证
│   │   ├── permissions.py       # 权限检查装饰器
│   │   ├── state_machine.py     # 问题状态机定义
│   │   ├── changelog_hook.py    # SQLAlchemy 事件监听器
│   │   └── exceptions.py        # 自定义异常
│   │
│   └── utils/
│       ├── __init__.py
│       └── id_generator.py      # 问题编号生成
│
├── alembic/                     # 数据库迁移
│   ├── alembic.ini
│   └── versions/
│
├── tests/
│   ├── test_issues.py
│   ├── test_changelog.py
│   └── test_state_machine.py
│
├── Dockerfile
├── requirements.txt
└── .env.example
```

---

## 五、前端模块划分

```
frontend/
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── router/
│   │   └── index.ts              # 路由定义
│   │
│   ├── stores/                   # Pinia 状态管理
│   │   ├── auth.ts
│   │   ├── issue.ts
│   │   └── dictionary.ts
│   │
│   ├── api/                      # API 请求封装
│   │   ├── request.ts            # Axios 实例 + 拦截器
│   │   ├── auth.ts
│   │   ├── issue.ts
│   │   ├── report.ts
│   │   └── ai.ts
│   │
│   ├── views/                    # 页面
│   │   ├── login/
│   │   │   └── LoginPage.vue
│   │   ├── register/
│   │   │   └── RegisterPage.vue
│   │   ├── issue/
│   │   │   ├── IssueList.vue     # 问题列表（含筛选）
│   │   │   ├── IssueDetail.vue   # 问题详情（含变更日志时间线）
│   │   │   └── IssueForm.vue     # 新建/编辑（含 AI 辅助）
│   │   ├── dashboard/
│   │   │   ├── Overview.vue      # 总览看板
│   │   │   ├── PlatformView.vue  # 分端看板
│   │   │   ├── PersonView.vue    # 分人看板
│   │   │   └── TrendView.vue     # 趋势分析
│   │   ├── admin/
│   │   │   ├── UserManage.vue    # 用户管理
│   │   │   ├── OrgManage.vue     # 组织管理
│   │   │   ├── RoleManage.vue    # 角色权限
│   │   │   └── DictManage.vue    # 数据字典
│   │   └── report/
│   │       └── DailyReport.vue   # 日报/周报查看
│   │
│   ├── components/               # 公共组件
│   │   ├── layout/
│   │   │   ├── AppLayout.vue     # 主布局
│   │   │   ├── Sidebar.vue       # 侧边导航
│   │   │   └── Header.vue        # 顶部栏
│   │   ├── issue/
│   │   │   ├── StatusBadge.vue   # 状态标签
│   │   │   ├── StatusFlow.vue    # 状态流转操作栏
│   │   │   └── ChangeTimeline.vue # 变更日志时间线
│   │   ├── ai/
│   │   │   ├── AIFloat.vue       # 右下角悬浮AI助手
│   │   │   ├── AIChatPanel.vue   # AI对话面板
│   │   │   └── AIInputHelper.vue # 表单AI辅助填写
│   │   └── common/
│   │       ├── RichEditor.vue    # 富文本编辑器
│   │       └── ImageUpload.vue   # 图片上传
│   │
│   ├── composables/              # 组合式函数
│   │   ├── useAuth.ts
│   │   ├── usePermission.ts
│   │   └── useAI.ts
│   │
│   └── styles/
│       └── global.scss
│
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
└── Dockerfile
```

---

## 六、数据库 ER 设计（核心表）

```sql
-- 组织
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    org_type VARCHAR(20) NOT NULL,  -- CLIENT / VENDOR
    parent_id UUID REFERENCES organizations(id),
    code VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 部门
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    org_id UUID NOT NULL REFERENCES organizations(id),
    parent_id UUID REFERENCES departments(id),
    sort_order INT DEFAULT 0
);

-- 用户
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(200),
    password_hash VARCHAR(256) NOT NULL,
    org_id UUID NOT NULL REFERENCES organizations(id),
    dept_id UUID REFERENCES departments(id),
    user_type VARCHAR(20) NOT NULL,   -- CLIENT / VENDOR
    vendor_role VARCHAR(20),          -- PRODUCT / DEV / TEST / ADMIN
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMPTZ
);

-- 角色
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- 用户-角色关联
CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role_id UUID REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- 权限
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL,
    scope VARCHAR(20) DEFAULT 'OWN'
);

-- 角色-权限关联
CREATE TABLE role_permissions (
    role_id UUID REFERENCES roles(id),
    permission_id UUID REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- 平台/端
CREATE TABLE platforms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0
);

-- 数据字典分类
CREATE TABLE dictionary_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    is_system BOOLEAN DEFAULT FALSE
);

-- 数据字典项
CREATE TABLE dictionary_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES dictionary_categories(id),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    extra_config JSONB
);

-- 问题（核心表）
CREATE TABLE issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_no VARCHAR(30) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    platform_id UUID NOT NULL REFERENCES platforms(id),
    issue_type_id UUID NOT NULL REFERENCES dictionary_items(id),
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',
    submitter_id UUID NOT NULL REFERENCES users(id),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    product_owner_id UUID REFERENCES users(id),
    dev_owner_id UUID REFERENCES users(id),
    test_owner_id UUID REFERENCES users(id),
    expected_date DATE,
    resolved_at TIMESTAMPTZ,
    closed_by UUID REFERENCES users(id),
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_issues_status ON issues(status);
CREATE INDEX idx_issues_platform ON issues(platform_id);
CREATE INDEX idx_issues_submitter ON issues(submitter_id);
CREATE INDEX idx_issues_submitted_at ON issues(submitted_at);

-- 变更日志（核心表 — 只增不改不删）
CREATE TABLE change_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    old_display TEXT,
    new_display TEXT,
    changed_by UUID NOT NULL REFERENCES users(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    change_source VARCHAR(20) DEFAULT 'MANUAL'
);

CREATE INDEX idx_changelog_entity ON change_logs(entity_type, entity_id);
CREATE INDEX idx_changelog_time ON change_logs(changed_at);
CREATE INDEX idx_changelog_person ON change_logs(changed_by);

-- 评论
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id),
    author_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 附件
CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID REFERENCES issues(id),
    comment_id UUID REFERENCES comments(id),
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMPTZ DEFAULT NOW()
);

-- 日报
CREATE TABLE daily_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_date DATE NOT NULL,
    report_type VARCHAR(20) NOT NULL,
    summary_json JSONB NOT NULL,
    ai_narrative TEXT,
    generated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 语义实体（含向量）
-- 需要先: CREATE EXTENSION vector;
CREATE TABLE semantic_entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    semantic_label TEXT,
    embedding vector(1536),
    metadata_json JSONB,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_semantic_embedding ON semantic_entities
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- AI对话记录
CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    context_refs JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 七、核心机制设计

### 7.1 变更日志自动记录（ChangeLog Hook）

利用 SQLAlchemy 的 `after_update` 事件监听器，在 ORM 层自动捕获所有字段变更：

```python
# 伪代码 — 核心思路
@event.listens_for(Issue, 'after_update')
def issue_after_update(mapper, connection, target):
    state = inspect(target)
    for attr in state.attrs:
        history = attr.history
        if history.has_changes():
            connection.execute(
                change_logs.insert().values(
                    entity_type='issue',
                    entity_id=target.id,
                    field_name=attr.key,
                    old_value=str(history.deleted[0]) if history.deleted else None,
                    new_value=str(history.added[0]) if history.added else None,
                    changed_by=current_user_id(),
                    changed_at=datetime.utcnow()
                )
            )
```

### 7.2 状态机引擎

```python
# 状态流转定义
TRANSITIONS = {
    'SUBMITTED':                  {'EVALUATING': ['VENDOR_PRODUCT']},
    'EVALUATING':                 {'PENDING': ['VENDOR_PRODUCT']},
    'PENDING':                    {'DEV_IN_PROGRESS': ['VENDOR_DEV'],
                                   'PRODUCT_FOLLOW_UP': ['VENDOR_PRODUCT']},
    'DEV_IN_PROGRESS':            {'TESTING': ['VENDOR_DEV'],
                                   'PRODUCT_FOLLOW_UP': ['VENDOR_DEV']},
    'PRODUCT_FOLLOW_UP':          {'DEV_IN_PROGRESS': ['VENDOR_PRODUCT'],
                                   'VERIFIED_PENDING_RELEASE': ['VENDOR_PRODUCT']},
    'TESTING':                    {'TEST_DONE_PENDING_VERIFY': ['VENDOR_TEST'],
                                   'DEV_IN_PROGRESS': ['VENDOR_TEST']},
    'TEST_DONE_PENDING_VERIFY':   {'VERIFIED_PENDING_RELEASE': ['VENDOR_PRODUCT'],
                                   'TESTING': ['VENDOR_PRODUCT']},
    'VERIFIED_PENDING_RELEASE':   {'RELEASED': ['VENDOR_PRODUCT', 'VENDOR_DEV']},
    'RELEASED':                   {'RESOLVED': ['SUBMITTER_ONLY'],
                                   'UNRESOLVED': ['SUBMITTER_ONLY']},
    'UNRESOLVED':                 {'PENDING': ['VENDOR_PRODUCT']},
}
```

### 7.3 AI 辅助录入流程

```
用户在表单页输入一段大白话
        │
        ▼
前端调用 POST /api/ai/parse-issue
        │
        ▼
后端将大白话 + 系统 Prompt 发给 Claude API
System Prompt 包含：字段定义、数据字典值、平台列表
        │
        ▼
Claude 返回结构化 JSON：
{
  "title": "...",
  "description": "...",
  "platform_code": "B_END",
  "issue_type_code": "BUG",
  "priority": "HIGH"
}
        │
        ▼
前端将解析结果回填到表单各字段
        │
        ▼
用户核对、修改 → 手动点击「提交」
```

### 7.4 AI 悬浮助手交互流程

```
用户点击右下角悬浮按钮 → 打开对话面板
        │
        ▼
用户输入自然语言问题
        │
        ▼
前端调用 POST /api/ai/chat （流式SSE）
        │
        ▼
后端 AI Service 处理流程：
  1. 分析用户意图（统计查询 / 状态查询 / 趋势分析...）
  2. 如果需要数据：生成 SQL 查询 或 向量搜索
  3. 执行查询获取结果
  4. 将结果 + 上下文传给 Claude 生成自然语言回答
        │
        ▼
流式返回回答到前端对话面板
```

### 7.5 日报自动生成

```
APScheduler 每日 01:00 触发
        │
        ▼
ReportService 计算前一日统计：
  - 新增问题数（总/分端/分人）
  - 关闭问题数（总/分端/分人）
  - 状态变化问题数
  - 停滞问题数（超过2天无变更）
  - 各状态分布
        │
        ▼
将统计数据 JSON 传给 Claude API
System Prompt: "你是项目日报助手，请基于以下数据生成简洁的日报..."
        │
        ▼
存入 daily_reports 表
```

---

## 八、统计看板维度设计

### 8.1 总览看板

| 指标 | 说明 |
|------|------|
| 今日新增 | 今日提交的问题数 |
| 今日关闭 | 今日标记为 RESOLVED 的问题数 |
| 今日状态变化 | 今日有 ChangeLog 的问题数（不含关闭） |
| 待处理总数 | 状态不为 RESOLVED 的问题总数 |
| 停滞问题数 | 超过2天无变更的未关闭问题 |
| 平均解决周期 | 从 SUBMITTED 到 RESOLVED 的平均天数 |

### 8.2 分端看板

按 Platform 分组展示：
- 每个端的问题总数、未关闭数、本周新增、本周关闭
- 每个端的问题类型分布（饼图）
- 每个端的状态分布（柱状图）

### 8.3 分人看板

按跟进人（产品/研发/测试）分组展示：
- 每人名下问题总数
- 每人各状态的问题数
- 每人每日问题变化曲线（折线图）
- 停滞问题高亮标记

### 8.4 趋势看板

- 每日新增/关闭趋势线（过去30天）
- 累计未关闭问题趋势
- 分端趋势对比
- 平均解决周期趋势

### 8.5 甲方视角看板

甲方用户登录后看到的简化看板：
- 我提交的问题总数及状态分布
- 本组织提交的问题整体进展
- 近7天关闭情况
- 待我验证的问题列表（状态为 RELEASED）

---

## 九、部署架构

```
┌─────────────────────────────────────────────────┐
│                  你的服务器                       │
│                                                  │
│   docker-compose.yml                             │
│   ┌──────────────┐  ┌──────────────┐             │
│   │    Nginx     │  │    Redis     │             │
│   │  :80 / :443  │  │   :6379      │             │
│   └──────┬───────┘  └──────────────┘             │
│          │                                       │
│   ┌──────▼───────┐  ┌──────────────┐             │
│   │   FastAPI    │  │  PostgreSQL  │             │
│   │   Backend    │──│  + pgvector  │             │
│   │   :8000      │  │   :5432      │             │
│   └──────────────┘  └──────────────┘             │
│                                                  │
│   /data/uploads/     (附件存储目录)               │
│   /data/db/          (PG数据卷)                   │
└─────────────────────────────────────────────────┘
          │
          │ HTTPS
          ▼
    Claude API (api.anthropic.com)
```

### Docker Compose 服务清单

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| nginx | nginx:1.25-alpine | 80, 443 | 反向代理 + 前端静态资源 |
| backend | 自建镜像 | 8000 | FastAPI 应用 |
| postgres | pgvector/pgvector:pg16 | 5432 | 数据库 + 向量扩展 |
| redis | redis:7-alpine | 6379 | 缓存 + 会话 |

---

## 十、安全设计

| 措施 | 实现方式 |
|------|---------|
| 认证 | JWT Token，access_token 2h + refresh_token 7d |
| 密码 | bcrypt 哈希存储 |
| 权限 | RBAC + 数据范围（OWN/DEPT/ORG/ALL） |
| API安全 | 限速（Redis） + CORS 白名单 + 请求签名 |
| 数据安全 | ChangeLog 表设置为 INSERT ONLY（应用层 + 数据库触发器） |
| 传输安全 | HTTPS (Let's Encrypt) |
| SQL注入 | ORM 参数化查询，不拼接 SQL |
| XSS | 富文本内容 sanitize + CSP 头 |
| 文件上传 | 类型白名单 + 大小限制 + 独立存储目录 |
