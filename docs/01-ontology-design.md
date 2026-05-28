# 基于本体论的项目问题管理系统 — 本体设计文档

> 版本：1.0 | 日期：2026-05-28

---

## 一、本体论设计总纲

本体论（Ontology）的核心是回答三个问题：**世界里有什么东西（类）、它们之间有什么关系（关系）、它们遵守什么规则（公理/约束）**。

本系统的本体模型围绕一个中心实体 **Issue（问题）** 展开，所有其他实体都是为了描述"谁在什么时候对哪个端提了什么类型的问题、谁在跟进、当前什么状态、发生了哪些变更"。

---

## 二、核心本体类（Classes）

### 2.1 类层级总览

```
Thing
├── Organization（组织）
│   ├── ClientOrg（甲方组织）
│   └── VendorOrg（乙方组织）
├── Department（部门）
├── Person（人员）
│   ├── ClientUser（甲方用户）
│   └── VendorUser（乙方用户）
│       ├── ProductOwner（产品跟进人）
│       ├── Developer（研发跟进人）
│       └── Tester（测试跟进人）
├── Role（角色）
├── Permission（权限）
├── Platform（平台/端）
├── Issue（问题）
├── IssueType（问题类型）
├── IssueStatus（问题状态）
├── ChangeLog（变更日志）
├── Comment（评论/沟通记录）
├── Attachment（附件）
├── DictionaryCategory（数据字典分类）
├── DictionaryItem（数据字典项）
├── DailyReport（日报）
├── SemanticEntity（语义实体）
└── AIConversation（AI对话记录）
```

### 2.2 各类详细定义

#### Organization（组织）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| name | String | Y | 组织名称 |
| org_type | Enum(CLIENT, VENDOR) | Y | 甲方/乙方 |
| parent_id | UUID | N | 上级组织（支持集团-分子公司层级） |
| code | String | Y | 组织编码 |
| created_at | DateTime | Y | 创建时间 |
| status | Enum(ACTIVE, INACTIVE) | Y | 状态 |

#### Department（部门）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| name | String | Y | 部门名称 |
| org_id | UUID | Y | 所属组织 |
| parent_id | UUID | N | 上级部门 |
| sort_order | Integer | N | 排序 |

#### Person / User（人员）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| name | String | Y | 姓名 |
| phone | String | Y | 手机号（登录凭证） |
| email | String | N | 邮箱 |
| org_id | UUID | Y | 所属组织 |
| dept_id | UUID | N | 所属部门 |
| user_type | Enum(CLIENT, VENDOR) | Y | 甲方/乙方 |
| vendor_role | Enum(PRODUCT, DEV, TEST, ADMIN) | N | 乙方角色细分 |
| status | Enum(PENDING, ACTIVE, DISABLED) | Y | 账号状态 |
| created_at | DateTime | Y | 注册时间 |
| approved_by | UUID | N | 审批人 |
| approved_at | DateTime | N | 审批时间 |

#### Role（角色）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| name | String | Y | 角色名称 |
| code | String | Y | 角色编码 |
| description | String | N | 描述 |
| permissions | [Permission] | Y | 权限集合 |

**预置角色**：
- `ADMIN` — 系统管理员（全部权限）
- `CLIENT_USER` — 甲方普通用户（提问题、看自己的、验证关闭）
- `CLIENT_MANAGER` — 甲方管理者（看所有甲方问题、看统计看板）
- `VENDOR_PRODUCT` — 乙方产品（问题评估、分配、产品验证）
- `VENDOR_DEV` — 乙方研发（研发跟进、状态流转）
- `VENDOR_TEST` — 乙方测试（测试跟进、状态流转）
- `VENDOR_MANAGER` — 乙方管理者（全部问题视图、统计看板、人员管理）

#### Permission（权限）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| resource | String | 资源标识（issue、user、report、dictionary...） |
| action | Enum(CREATE, READ, UPDATE, DELETE, EXPORT) | 操作 |
| scope | Enum(OWN, DEPARTMENT, ORG, ALL) | 数据范围 |

#### Platform（平台/端）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| name | String | Y | 端名称 |
| code | String | Y | 编码 |
| description | String | N | 描述 |
| sort_order | Integer | N | 排序 |

**预置平台**：
| 编码 | 名称 |
|------|------|
| C_END | C端（员工电商平台） |
| B_END | B端（企业采购平台） |
| OPS_BACKEND | 运营管理后台 |
| SUPPLIER_PORTAL | 供应商协同门户 |
| ENTERPRISE_PORTAL | 企业自维护平台 |

#### Issue（问题） — 核心实体

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| issue_no | String | Y | 问题编号（自动生成，如 ISS-20260528-0001） |
| title | String | Y | 一句话描述 |
| description | RichText | N | 详细描述（支持富文本、图片） |
| platform_id | UUID | Y | 所属平台/端 |
| issue_type_id | UUID | Y | 问题类型（关联数据字典） |
| priority | Enum(CRITICAL, HIGH, MEDIUM, LOW) | N | 优先级 |
| status | Enum(见下方状态机) | Y | 当前状态 |
| submitter_id | UUID | Y | 提交人（系统自动记录） |
| submitted_at | DateTime | Y | 提交时间（系统自动记录） |
| product_owner_id | UUID | N | 产品跟进人 |
| dev_owner_id | UUID | N | 研发跟进人 |
| test_owner_id | UUID | N | 测试跟进人 |
| expected_date | Date | N | 期望解决日期 |
| resolved_at | DateTime | N | 解决时间 |
| closed_by | UUID | N | 关闭人（必须为提交人） |
| closed_at | DateTime | N | 关闭时间 |
| created_at | DateTime | Y | 创建时间 |
| updated_at | DateTime | Y | 最后更新时间 |

#### IssueType（问题类型） — 数据字典驱动

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| name | String | Y | 类型名称 |
| code | String | Y | 编码 |
| sort_order | Integer | N | 排序 |
| is_active | Boolean | Y | 是否启用 |

**预置类型**：
| 编码 | 名称 |
|------|------|
| PENDING_EVAL | 待评估 |
| BUG | 系统Bug |
| DESIGN_DEFECT | 设计缺陷 |
| FEATURE_OPT | 功能优化 |
| CONSULTATION | 咨询答疑 |
| EXTERNAL_ISSUE | 外部系统问题 |

#### IssueStatus（问题状态）

**状态枚举与流转权限**：

| 状态编码 | 状态名称 | 谁可以流转到此状态 |
|----------|----------|-------------------|
| SUBMITTED | 已提交 | 甲方用户（创建时自动） |
| EVALUATING | 待评估 | 乙方产品 |
| PENDING | 待处理 | 乙方产品（评估后分配） |
| DEV_IN_PROGRESS | 研发跟进中 | 乙方研发 |
| PRODUCT_FOLLOW_UP | 产品跟进中 | 乙方产品 |
| TESTING | 测试中 | 乙方测试 |
| TEST_DONE_PENDING_VERIFY | 测试完成待产品验证 | 乙方测试 |
| VERIFIED_PENDING_RELEASE | 产品验证完成待上线 | 乙方产品 |
| RELEASED | 已上线 | 乙方产品/研发 |
| RESOLVED | 已解决 | **仅提交人**（甲方） |
| UNRESOLVED | 验证不通过 | **仅提交人**（甲方） |

#### ChangeLog（变更日志） — 核心痛点解决方案

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| entity_type | String | Y | 变更实体类型（issue、user...） |
| entity_id | UUID | Y | 变更实体ID |
| field_name | String | Y | 变更字段名 |
| old_value | Text | N | 变更前值 |
| new_value | Text | N | 变更后值 |
| old_display | Text | N | 变更前显示文本 |
| new_display | Text | N | 变更后显示文本 |
| changed_by | UUID | Y | 变更人 |
| changed_at | DateTime | Y | 变更时间 |
| change_source | Enum(MANUAL, SYSTEM, AI_ASSIST) | Y | 变更来源 |

**设计要点**：每次对 Issue 的任何字段做修改，系统自动生成 ChangeLog 记录。这是一个**只增不改不删**的追加日志表，解决了"多维表格看不到谁改了什么"的核心痛点。

#### Comment（评论/沟通记录）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| issue_id | UUID | Y | 所属问题 |
| author_id | UUID | Y | 评论人 |
| content | RichText | Y | 内容 |
| created_at | DateTime | Y | 创建时间 |

#### Attachment（附件）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | UUID | Y | 主键 |
| issue_id | UUID | N | 关联问题 |
| comment_id | UUID | N | 关联评论 |
| file_name | String | Y | 文件名 |
| file_path | String | Y | 存储路径 |
| file_size | Long | Y | 文件大小 |
| mime_type | String | Y | MIME类型 |
| uploaded_by | UUID | Y | 上传人 |
| uploaded_at | DateTime | Y | 上传时间 |

#### DictionaryCategory / DictionaryItem（数据字典）

**DictionaryCategory**:
| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| name | String | 分类名称（如"问题类型"、"优先级"） |
| code | String | 编码 |
| is_system | Boolean | 是否系统内置（内置不可删） |

**DictionaryItem**:
| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| category_id | UUID | 所属分类 |
| name | String | 项名称 |
| code | String | 编码 |
| sort_order | Integer | 排序 |
| is_active | Boolean | 是否启用 |
| extra_config | JSON | 扩展配置 |

#### DailyReport（日报/周报）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| report_date | Date | 报告日期 |
| report_type | Enum(DAILY, WEEKLY) | 报告类型 |
| summary_json | JSON | 结构化统计数据 |
| ai_narrative | Text | AI生成的文字描述 |
| generated_at | DateTime | 生成时间 |

#### SemanticEntity（语义实体） — 本体语义层

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| entity_type | String | 实体类型 |
| entity_id | UUID | 关联业务实体ID |
| semantic_label | String | 语义标签 |
| embedding | Vector(1536) | 向量嵌入 |
| metadata_json | JSON | 元数据 |
| updated_at | DateTime | 更新时间 |

#### AIConversation（AI对话记录）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| session_id | String | 会话ID |
| user_id | UUID | 用户 |
| role | Enum(USER, ASSISTANT) | 消息角色 |
| content | Text | 消息内容 |
| context_refs | JSON | 引用的实体（问题ID、人员ID等） |
| created_at | DateTime | 时间 |

---

## 三、本体关系（Object Properties / Relations）

### 3.1 关系总览图（文字描述）

```
Organization --[has_department]--> Department
Department --[has_sub_department]--> Department
Organization --[has_member]--> Person
Department --[has_member]--> Person
Person --[has_role]--> Role
Role --[has_permission]--> Permission

Person(ClientUser) --[submits]--> Issue
Person(VendorProduct) --[product_owns]--> Issue
Person(VendorDev) --[dev_owns]--> Issue
Person(VendorTest) --[test_owns]--> Issue

Issue --[belongs_to_platform]--> Platform
Issue --[has_type]--> IssueType
Issue --[has_status]--> IssueStatus
Issue --[has_attachment]--> Attachment
Issue --[has_comment]--> Comment
Issue --[has_changelog]--> ChangeLog

Person --[authors]--> Comment
Person --[triggers]--> ChangeLog

DictionaryCategory --[contains]--> DictionaryItem
IssueType --[is_a]--> DictionaryItem

SemanticEntity --[represents]--> Issue | Person | Platform | ...
AIConversation --[initiated_by]--> Person
AIConversation --[references]--> Issue | Person | ...
```

### 3.2 关键关系详述

| 关系名称 | 主体 | 客体 | 基数 | 业务含义 |
|----------|------|------|------|----------|
| submits | ClientUser | Issue | 1:N | 一个甲方用户可提交多个问题 |
| product_owns | VendorProduct | Issue | 1:N | 一个产品人可跟进多个问题 |
| dev_owns | VendorDev | Issue | 1:N | 一个研发可跟进多个问题 |
| test_owns | VendorTest | Issue | 1:N | 一个测试可跟进多个问题 |
| closes | ClientUser | Issue | 1:N | 只有提交人可关闭 |
| belongs_to_platform | Issue | Platform | N:1 | 每个问题归属一个端 |
| has_changelog | Issue | ChangeLog | 1:N | 每个问题有多条变更记录 |
| changed_by | ChangeLog | Person | N:1 | 每条变更由一个人触发 |

---

## 四、状态机（Issue Lifecycle State Machine）

```
                    ┌──────────────────────────────────────────────────────────┐
                    │                                                          │
                    ▼                                                          │
┌──────────┐   ┌──────────┐   ┌─────────┐   ┌──────────────┐                 │
│ SUBMITTED│──▶│EVALUATING│──▶│ PENDING  │──▶│DEV_IN_PROGRESS│                │
│ 已提交    │   │ 待评估    │   │ 待处理   │   │ 研发跟进中     │                │
└──────────┘   └──────────┘   └─────────┘   └──────┬───────┘                 │
                                                     │                        │
                                    ┌────────────────┤                        │
                                    ▼                ▼                        │
                          ┌─────────────────┐  ┌──────────┐                   │
                          │PRODUCT_FOLLOW_UP│  │ TESTING  │                   │
                          │ 产品跟进中        │  │ 测试中   │                   │
                          └────────┬────────┘  └────┬─────┘                   │
                                   │                 │                        │
                                   │                 ▼                        │
                                   │   ┌──────────────────────────┐           │
                                   │   │TEST_DONE_PENDING_VERIFY  │           │
                                   │   │ 测试完成待产品验证         │           │
                                   │   └───────────┬──────────────┘           │
                                   │               │                          │
                                   ▼               ▼                          │
                          ┌──────────────────────────┐                        │
                          │ VERIFIED_PENDING_RELEASE  │                       │
                          │ 产品验证完成待上线          │                       │
                          └───────────┬──────────────┘                        │
                                      │                                       │
                                      ▼                                       │
                               ┌──────────┐                                   │
                               │ RELEASED │                                   │
                               │ 已上线    │                                   │
                               └────┬─────┘                                   │
                                    │                                         │
                          ┌─────────┴─────────┐                               │
                          ▼                   ▼                               │
                   ┌──────────┐        ┌────────────┐                         │
                   │ RESOLVED │        │ UNRESOLVED │─────────────────────────┘
                   │ 已解决    │        │ 验证不通过  │  （回到待处理重新流转）
                   └──────────┘        └────────────┘
```

### 状态流转规则

| 当前状态 | 可流转到 | 操作角色 | 触发条件 |
|----------|---------|---------|---------|
| SUBMITTED | EVALUATING | 乙方产品 | 受理问题 |
| EVALUATING | PENDING | 乙方产品 | 评估完成，确定类型并分配 |
| PENDING | DEV_IN_PROGRESS | 乙方研发 | 开始研发跟进 |
| PENDING | PRODUCT_FOLLOW_UP | 乙方产品 | 产品侧先处理 |
| DEV_IN_PROGRESS | TESTING | 乙方研发 | 开发完成提测 |
| DEV_IN_PROGRESS | PRODUCT_FOLLOW_UP | 乙方研发 | 需产品确认 |
| PRODUCT_FOLLOW_UP | DEV_IN_PROGRESS | 乙方产品 | 确认后转研发 |
| PRODUCT_FOLLOW_UP | VERIFIED_PENDING_RELEASE | 乙方产品 | 产品侧验证通过 |
| TESTING | TEST_DONE_PENDING_VERIFY | 乙方测试 | 测试通过 |
| TESTING | DEV_IN_PROGRESS | 乙方测试 | 测试不通过打回 |
| TEST_DONE_PENDING_VERIFY | VERIFIED_PENDING_RELEASE | 乙方产品 | 产品二次验证通过 |
| TEST_DONE_PENDING_VERIFY | TESTING | 乙方产品 | 验证不通过打回 |
| VERIFIED_PENDING_RELEASE | RELEASED | 乙方产品/研发 | 已发布上线 |
| RELEASED | RESOLVED | **仅原始提交人** | 客户验证通过 |
| RELEASED | UNRESOLVED | **仅原始提交人** | 客户验证不通过 |
| UNRESOLVED | PENDING | 乙方产品 | 重新进入处理流程 |

---

## 五、本体公理与业务规则（Axioms & Rules）

### R1 — 变更日志不可篡改
> 对 Issue 任何字段的修改，系统必须自动在 ChangeLog 表中生成一条记录，记录变更人、变更时间、变更前后值。ChangeLog 记录只能新增，不可修改、不可删除。

### R2 — 谁提的问题谁关闭
> Issue 的 `status` 只有当操作人 `== submitter_id` 时，才允许从 `RELEASED` 流转到 `RESOLVED` 或 `UNRESOLVED`。

### R3 — 状态流转必须符合状态机
> Issue 的状态变更必须遵循第四章定义的状态机。任何不在状态机中定义的流转路径都应被系统拒绝。

### R4 — 角色-状态流转对应
> 每个状态流转操作必须校验当前操作人的角色是否有权执行。见状态流转规则表的"操作角色"列。

### R5 — AI辅助不自动提交
> AI解析用户自然语言后，结果必须回显到表单供用户确认。AI不得直接调用提交接口。人工确认是提交的唯一入口。

### R6 — 数据字典可扩展
> 管理员可增删改数据字典项（IssueType、Priority 等），但系统内置项（is_system=true）不可删除，只可禁用。

### R7 — 问题编号唯一且不可变
> Issue 创建时自动生成 issue_no（格式：ISS-YYYYMMDD-NNNN），生成后不可修改。

### R8 — 语义实体同步规则
> 当 Issue 被创建或关键字段变更时，系统异步更新对应的 SemanticEntity 记录（重新生成 embedding）。

### R9 — 日报自动生成规则
> 系统每日凌晨自动计算前一日的问题统计（新增、关闭、状态变化、分端、分人），生成 DailyReport 并调用 AI 生成文字叙述。

### R10 — 停滞预警规则
> 当一个 Issue 在某个状态停留超过配置的阈值（默认2天）且无任何 ChangeLog 产生时，系统标记为"停滞"，在看板上高亮显示，并可通过 AI 助手查询。

---

## 六、语义层设计（Semantic Layer）

### 6.1 语义层的作用

语义层是连接**结构化业务数据**和**AI自然语言理解**的桥梁。它的职责是：

1. **实体语义化**：为每个业务对象生成自然语言描述和向量嵌入，使AI能理解"ISS-20260528-0012 是B端采购模块的一个高优先级Bug，目前卡在测试阶段已经3天"。
2. **关系语义化**：将对象间的关系转化为可检索的语义三元组（主-谓-宾），如 `(张三, 负责研发, ISS-0012)`。
3. **查询翻译**：将用户的自然语言问题翻译为结构化查询。如"B端最近一周有多少新Bug" → SQL/API查询。

### 6.2 语义三元组模型

```
(Subject, Predicate, Object)
```

示例三元组：
```
(张三, 提交了问题, ISS-20260528-0012)
(ISS-20260528-0012, 属于平台, B端企业采购平台)
(ISS-20260528-0012, 问题类型是, 系统Bug)
(ISS-20260528-0012, 当前状态是, 研发跟进中)
(ISS-20260528-0012, 研发跟进人是, 李四)
(ISS-20260528-0012, 已停滞, 3天)
(李四, 名下待处理问题数, 12)
(B端, 本周新增问题数, 8)
```

### 6.3 向量嵌入策略

每个 Issue 生成一段自然语言摘要，再通过 Embedding 模型（如 text-embedding-3-small）转为向量存入向量库：

**摘要模板**：
```
问题编号 {issue_no}，{platform} 的 {issue_type} 问题。
标题：{title}。
描述摘要：{description_summary}。
提交人：{submitter}，提交时间：{submitted_at}。
当前状态：{status}，产品跟进：{product_owner}，研发跟进：{dev_owner}，测试跟进：{test_owner}。
最后更新：{updated_at}。
```

### 6.4 AI 可回答的问题类型

| 问题类型 | 示例 | 实现方式 |
|---------|------|---------|
| 统计查询 | "B端这周新增了多少Bug？" | 语义解析 → SQL查询 |
| 状态查询 | "ISS-0012 现在什么状态？" | 实体检索 → 字段读取 |
| 人员负荷 | "张三手上有多少问题还没关闭？" | 语义解析 → 聚合查询 |
| 停滞分析 | "哪些问题超过3天没动了？" | 规则查询 → ChangeLog分析 |
| 趋势分析 | "最近一个月Bug趋势怎样？" | 时序聚合 → AI叙述 |
| 模块健康度 | "哪个端的问题最多？" | 分组聚合 → 排序 |
| 相似问题 | "有没有跟这个类似的问题？" | 向量相似搜索 |
| 综合研判 | "项目整体风险怎么样？" | 多维聚合 → AI综合分析 |

---

## 七、本体模型 OWL 形式化表达（简化版）

```turtle
@prefix pm: <http://project-management.ontology/> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

# Classes
pm:Organization a owl:Class .
pm:Department a owl:Class .
pm:Person a owl:Class .
pm:ClientUser rdfs:subClassOf pm:Person .
pm:VendorUser rdfs:subClassOf pm:Person .
pm:Role a owl:Class .
pm:Permission a owl:Class .
pm:Platform a owl:Class .
pm:Issue a owl:Class .
pm:IssueType a owl:Class .
pm:IssueStatus a owl:Class .
pm:ChangeLog a owl:Class .
pm:Comment a owl:Class .
pm:Attachment a owl:Class .

# Object Properties
pm:submits a owl:ObjectProperty ;
    rdfs:domain pm:ClientUser ;
    rdfs:range pm:Issue .

pm:productOwns a owl:ObjectProperty ;
    rdfs:domain pm:VendorUser ;
    rdfs:range pm:Issue .

pm:belongsToPlatform a owl:ObjectProperty ;
    rdfs:domain pm:Issue ;
    rdfs:range pm:Platform ;
    a owl:FunctionalProperty .  # 每个Issue只属于一个平台

pm:hasType a owl:ObjectProperty ;
    rdfs:domain pm:Issue ;
    rdfs:range pm:IssueType ;
    a owl:FunctionalProperty .

pm:hasChangeLog a owl:ObjectProperty ;
    rdfs:domain pm:Issue ;
    rdfs:range pm:ChangeLog .

# Data Properties
pm:issueNo a owl:DatatypeProperty ;
    rdfs:domain pm:Issue ;
    rdfs:range xsd:string .

pm:submittedAt a owl:DatatypeProperty ;
    rdfs:domain pm:Issue ;
    rdfs:range xsd:dateTime .

# Axiom: only submitter can close
# (expressed as SWRL rule)
# Person(?p) ∧ Issue(?i) ∧ submits(?p, ?i) → canClose(?p, ?i)
```

---

## 八、本体扩展点

本体设计预留以下扩展能力，但**第一期不实现**：

| 扩展方向 | 说明 |
|---------|------|
| SLA 管理 | 为不同优先级定义响应时限和解决时限 |
| 关联问题 | Issue 之间的关联（重复、依赖、阻塞） |
| 版本管理 | 将 Issue 关联到版本/迭代 |
| 审批流 | 通用审批引擎 |
| 知识库 | 从已解决问题中沉淀FAQ |
| 飞书集成 | 通过飞书机器人推送通知、接收问题 |
| 移动端适配 | 响应式或独立移动端 |
