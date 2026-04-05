# Help Manual Website - Project Memory

## Project Overview
内部使用的产品帮助手册网站，类似 Easy Vibe / GitBook 的文档展示系统，带 Web 管理后台。

## Key Decisions (DO NOT CHANGE without user approval)

1. **草稿/发布分离**：article 表有 draft_* 和 published_* 两套字段。编辑只改 draft，发布时 draft→published。前台只读 published。
2. **前台可见性控制**：分类和文章都有 visible 字段。父分类不可见则子内容自动隐藏。与"发布"是两个独立概念。
3. **权限模型简化**：不做板块权限。所有编辑者可编辑所有文章，但操作记录在 operation_log 中追溯。
4. **版本管理**：发布时必须填写版本号(如 v1.0)和更新说明，创建不可变版本快照。
5. **同时只有一个草稿**：每篇文章只有一个草稿态。
6. **编辑器模式锁定**：创建文章时选择编辑器（富文本/Markdown），之后不可切换。
7. **文件上传限制**：<300MB，MIME 白名单（image/*, video/*, application/pdf）。
8. **Docker 部署**：所有持久化数据(SQLite + uploads)通过 volume 挂载，迁移只需复制 data 目录。
9. **不需要 SEO**：内部使用，Vue SPA 方案即可。
10. **不需要多语言内容翻译**：UI 固定中文。
11. **AI 能力预留**：Service 层预留接口，当前不实现。
12. **前台实时 API**：Vue SPA 通过 REST API 实时获取数据，后台发布后前台刷新即可见，不需要重新 build 前端。

## Tech Stack
- Backend: Spring Boot 3.4 + Java 21 + SQLite (WAL mode) + JPA + Caffeine Cache
- Frontend: Vue 3 + TypeScript + Vite + Element Plus (按需引入)
- Rich Text Editor: WangEditor 5
- Markdown Editor: md-editor-v3 (lazy load)
- Word Import: mammoth.js
- Image Viewer: viewerjs (前台点击放大/缩小/旋转)
- Video Player: video.js or xgplayer (全屏/倍速/进度条)
- Code Highlight: highlight.js
- HTML Sanitize: jsoup (backend) + DOMPurify (frontend)
- Auth: JWT (jjwt 0.12)
- Search: SQLite FTS5
- Deploy: Docker + docker-compose

## Database Tables
user, category, article (draft/published分离), article_version, media, operation_log, article_fts (FTS5)

## Implementation Plan
Full plan at: /root/.claude/plans/clever-imagining-lampson.md

## Documentation Deliverables
- docs/requirements.md - 需求文档
- docs/user-guide.md - 使用手册
- docs/deployment.md - 部署手册

## Important User Preferences
- 用户之前用开源工具拼接(DocuCMS)失败过，所以这次从零构建整体方案
- 展示效果参考 Easy Vibe (VitePress 风格)
- ~20 个编辑者，各自编辑不同板块
- 项目需要分发给其他人使用，所以文档很重要
- 数据库怎么简单怎么来（SQLite）
