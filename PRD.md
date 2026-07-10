# PRD：个人博客后端改造

> 版本: v1.0 | 日期: 2026-07-07 | 作者: hongxiamiao

---

## 一、项目背景与目标

### 1.1 现状

当前博客（2025-blog）的架构为 **Next.js 前端直连 GitHub API** 实现 CMS —— 博客文章的增删改全部通过 GitHub REST API 写入私有仓库，再依赖 Vercel/Cloudflare 自动部署生效。

**痛点：**
- 发布一篇文章需要 5~8 次 GitHub API 调用，耗时 3~5 秒
- 发布后需等待部署平台构建（1~3 分钟），内容才能生效
- 修改站点配置等同理，链路长、黑盒多
- GitHub API 有速率限制，遇到限流直接不可用
- 数据与 GitHub 仓库耦合，无法独立演进

### 1.2 目标

将数据写入链路从 **前端 → GitHub API** 替换为 **前端 → Java REST API → MySQL**，实现：

- 内容发布**即时生效**（无部署等待）
- 数据完全**自主可控**（不依赖第三方 API）
- 后端服务跑在一台 **2核2G 云服务器**上，不额外购买 OSS/Redis 等服务
- 前端改动量控制在 **10% 以内**，所有页面和 UI 组件完整保留

---

## 二、用户与场景

### 2.1 用户画像

| 角色 | 人数 | 说明 |
|------|------|------|
| **博客所有者（Admin）** | 1 人 | 写博客、管理站点配置、管理各内容模块 |
| **访客** | N 人 | 浏览博客、点赞 |

### 2.2 核心场景

| 场景 | 频次 | 当前痛点 |
|------|------|---------|
| 发布/编辑博客 | 每周 1~3 次 | GitHub API 链路长，需等部署 |
| 修改站点配置 | 每月 1~2 次 | 字段多，保存流程繁琐 |
| 管理收藏/项目/分享等内容 | 每月 2~5 次 | 每次修改都走 Git 提交 |
| 上传图片 | 附属于写博客时 | GitHub Blob API 间接上传 |
| 浏览博客 | 每天 | 无痛点（静态文件加载快） |
| 点赞 | 每天 | 依赖独立 Cloudflare Worker |

---

## 三、功能需求

### 3.1 认证模块

```
P0  管理员登录（用户名 + 密码 → JWT Token）
P0  Token 刷新（24h 过期，支持续期）
P0  接口鉴权（除 GET 和点赞外，所有写操作需登录）
```

**说明：** 从当前的"导入 GitHub PEM 私钥文件"改为标准用户名密码登录。首次部署时自动创建默认管理员账号，登录后可修改密码。

### 3.2 博客模块

```
P0  创建博客（标题 / slug / Markdown 正文 / 摘要 / 标签 / 分类 / 封面图 / 配图 / 是否隐藏）
P0  编辑博客
P0  删除博客（单个 + 批量）
P0  博客列表（支持 ?category=&tag=&search=&page=&size= 查询）
P0  博客详情（返回元信息 + Markdown 正文）
P0  上传博客配图（混排图片，插入 Markdown 时实时上传）
```

**交互流程：** 前端写博客页面当前先上传图片到 GitHub → 获得 URL → 插入 Markdown。改造后改为上传到 Java 后端 → 返回本地 URL → 插入 Markdown，其余交互完全不变。

### 3.3 分类管理

```
P0  获取分类列表
P1  新增/编辑/删除分类
P1  分类排序
```

### 3.4 站点配置

```
P0  获取站点配置（meta 信息 / 头像 / Favicon / ICP备案号 / 彩色主题 / 社交按钮等）
P0  更新站点配置
P0  上传 Favicon
P0  上传头像
```

**当前站点配置字段清单**（对齐现有 `site-content.json`）：
- `meta_title` / `meta_description`
- `favicon_url` / `avatar_url`
- `beian`（ICP 备案号）
- `hat_content`（首页顶部帽子卡片的 Markdown 内容）
- `hide_edit_button` / `is_cache_pem`
- `theme`（JSON：主色、背景色等）
- 社交按钮列表（type + value + label）
- 卡片布局样式列表（宽高、偏移量、排序、启用状态）
- 首页装饰图片列表
- 背景图片列表

### 3.5 内容模块（统一 CRUD）

以下模块遵循相同模式：列表查询 + 新增 + 编辑 + 删除，大部分支持图片上传。

| 模块 | 读 | 写 | 支持图片上传 |
|------|----|----|------------|
| **博主收藏** | 列表 | 增删改 | 头像 |
| **项目展示** | 列表 | 增删改 | 项目截图 |
| **分享链接** | 列表 | 增删改 | Logo |
| **图片画廊** | 列表 | 增删改 | 多图 |
| **代码片段** | 列表 | 批量更新 | 否 |
| **关于页面** | 单条 | 更新 | 否 |

### 3.6 点赞模块

```
P1  获取某篇博客的点赞数（GET  /api/v1/likes/{slug}）
P1  点赞（POST /api/v1/likes/{slug}，基于 IP + 时间窗口防刷）
```

从 Cloudflare Worker 迁移到 Java 后端，不再需要独立的 Worker 服务。

### 3.7 图片上传（通用）

```
P0  单图上传 → 返回可访问 URL
P0  批量上传
P1  根据 SHA256 去重（上传同一张图不重复存储）
```

**存储路径约定：** `/data/blog-images/{year}/{month}/{hash}.{ext}`

### 3.8 SEO 相关

```
P1  Sitemap 动态生成（从 API 获取博客列表）
P1  RSS Feed 动态生成
```

### 3.9 不作为本期范围（Out of Scope）

- 评论系统
- 全文搜索（先用数据库 LIKE，后续可加 Elasticsearch）
- 文章版本历史/回滚
- 草稿箱
- 多用户 / RBAC
- 访问统计面板
- 自动化数据迁移脚本（手动迁移即可）

---

## 四、非功能需求

### 4.1 性能

| 指标 | 目标 | 说明 |
|------|------|------|
| 博客列表 API 响应 | < 100ms | 含分类/标签筛选 |
| 博客详情 API 响应 | < 100ms | Markdown 正文可能较大 |
| 图片上传 | < 500ms | 单张 < 5MB |
| 博客发布（含图片） | < 1s | 对比当前 3~5s + 3min 部署等待 |
| 并发读 | 50 QPS | 个人博客足够了 |
| 并发写 | 不要求 | 单用户，无并发写入场景 |

### 4.2 资源约束

| 资源 | 限制 | 措施 |
|------|------|------|
| CPU | 2 核 | 单 Jar 部署，无需额外进程 |
| 内存 | 2 GB | MySQL BP → 64MB，JVM -Xmx256m |
| 磁盘 | 系统盘 | 图片 < 50 张 × 2MB ≈ 100MB，数据库预估 < 50MB |
| 图片存储 | 本地文件系统 | `/data/blog-images/`，Nginx 直接 serve 或 Spring 静态资源映射 |

### 4.3 安全

| 需求 | 方案 |
|------|------|
| 密码存储 | BCrypt 加密 |
| JWT 签名 | HMAC-SHA256，secret 配置在 application.yml |
| Token 有效期 | 24h access + 7d refresh |
| 上传限制 | 单文件 10MB，仅允许图片格式（jpg/png/webp/gif/svg） |
| 点赞防刷 | 同一 IP 对同一文章 1 小时内只能点赞一次 |
| CORS | 仅允许前端域名 |

### 4.4 可维护性

- API 文档通过 Knife4j 自动生成，开发时可浏览器访问 `/doc.html` 调试
- 数据库 DDL 脚本纳入版本管理（`src/main/resources/db/`）
- 后端日志输出到文件 + 控制台，按天滚动

---

## 五、技术方案

### 5.1 最终选型

| 层次 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 17 (LTS) |
| 框架 | Spring Boot | 3.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.x |
| 认证 | Spring Security + jjwt | — |
| 缓存 | Caffeine | — |
| 图片存储 | 本地文件系统 | — |
| API 文档 | Knife4j + SpringDoc | — |
| 构建 | Maven | — |

### 5.2 为什么不选的其他方案

| 不选 | 原因 |
|------|------|
| H2 | 用户选择 MySQL，贴近生产环境 |
| JPA | SQL 场景灵活（JSON 字段、全文模糊搜索），MyBatis 更透明 |
| MinIO | < 50 张图片，额外进程浪费内存 |
| Redis | 2G 内存不够，Caffeine 替代 |
| Docker | 2G 内存跑 Docker 比较勉强，直接 jar 部署更省 |

### 5.3 部署架构

```
用户 → Nginx (:80/443)
         │
         ├── /api/*  → 反代到 localhost:8080  (Spring Boot)
         ├── /images/* → 反代到 localhost:8080/images  (Spring Boot 静态资源)
         └── 其他    → 反代到 Next.js (:3000)
```

### 5.4 MySQL 内存压榨配置

```ini
[mysqld]
innodb_buffer_pool_size = 64M
max_connections = 20
performance_schema = OFF
skip_name_resolve = ON
innodb_flush_log_at_trx_commit = 2   # 个人博客可以接受少量数据丢失
```

### 5.5 数据库表清单（共 16 张）

| 表名 | 说明 | 预估数据量 |
|------|------|-----------|
| `users` | 管理员用户 | 1 行 |
| `blog_posts` | 博客文章 | < 200 行 |
| `blog_images` | 博客配图 | < 100 行 |
| `categories` | 分类 | < 20 行 |
| `site_config` | 站点配置 | 1 行 |
| `card_styles` | 卡片布局 | < 20 行 |
| `art_images` | 首页装饰图 | < 10 行 |
| `background_images` | 背景图 | < 10 行 |
| `social_buttons` | 社交按钮 | < 10 行 |
| `bloggers` | 博主收藏 | < 50 行 |
| `projects` | 项目展示 | < 20 行 |
| `shares` | 分享链接 | < 30 行 |
| `pictures` | 图片画廊 | < 20 行 |
| `snippets` | 代码片段 | < 50 行 |
| `about` | 关于页面 | 1 行 |
| `likes` | 点赞记录 | < 5000 行 |

### 5.6 API 契约（共 40+ 接口）

详见可行性分析 §4.3（已在审查中确认）。核心原则：

- **Base URL:** `/api/v1`
- **认证方式:** `Authorization: Bearer <jwt_token>`
- **创建/更新博客:** `multipart/form-data`（config JSON + markdown 文件 + 图片文件一次性提交）
- **其他写操作:** `application/json`
- **列表接口:** 支持 `?page=&size=` 分页

---

## 六、前端改造范围（约 10%）

### 6.1 需要改的文件（16 个）

| 文件 | 改造内容 | 风险 |
|------|---------|------|
| `src/lib/api-client.ts` (新增) | Java 后端 API 封装，替代 github-client.ts | 低 |
| `src/lib/auth-api.ts` (新增) | 登录/Token 管理 | 低 |
| `src/consts.ts` | 环境变量从 GITHUB_CONFIG → API_CONFIG | 极低 |
| `src/hooks/use-auth.ts` | 密钥管理 → 登录状态管理 | 低 |
| `src/app/(home)/config-dialog/index.tsx` | 密钥导入 UI → 登录表单 | 中 |
| `src/app/write/services/push-blog.ts` | GitHub API → Java API | 低 |
| `src/app/write/services/delete-blog.ts` | 同上 | 低 |
| `src/app/blog/services/batch-delete-blogs.ts` | 同上 | 低 |
| `src/app/blog/services/save-blog-edits.ts` | 同上 | 低 |
| `src/app/bloggers/services/push-bloggers.ts` | 同上 | 低 |
| `src/app/projects/services/push-projects.ts` | 同上 | 低 |
| `src/app/share/services/push-shares.ts` | 同上 | 低 |
| `src/app/pictures/services/push-pictures.ts` | 同上 | 低 |
| `src/app/snippets/services/push-snippets.ts` | 同上 | 低 |
| `src/app/about/services/push-about.ts` | 同上 | 低 |
| `src/app/(home)/services/push-site-content.ts` | 同上 | 低 |
| `src/components/like-button.tsx` | ENDPOINT 改为自己的后端 | 极低 |
| `src/app/sitemap.ts` | 数据源改为 API | 低 |
| `src/app/rss.xml/route.ts` | 数据源改为 API | 低 |
| `src/lib/load-blog.ts` | 数据源改为 API | 低 |

### 6.2 不需要改的部分（约 90%）

- 所有页面组件、UI 组件、布局组件
- Tailwind CSS 配置、动画
- Markdown 渲染管线（marked + Shiki + KaTeX）
- Zustand Store（仅修改数据来源，Store 结构不变）
- 工具函数、常量、类型定义

---

## 七、实施计划

### Phase 1：后端基础 + 博客 CRUD（预计 5~7 天）

```
□ Spring Boot 项目初始化（Maven、依赖、多环境配置）
□ 数据库 DDL 脚本 + 初始化数据
□ MyBatis-Plus 代码生成器生成 Mapper/Service/Controller
□ 认证模块（Spring Security + JWT 登录/刷新）
□ 图片上传（本地存储 + 静态资源映射）
□ 博客 CRUD API（创建/读取/更新/删除/列表）
□ 博客配图管理
□ Knife4j 接口文档
```

### Phase 2：内容模块 + 站点配置（预计 3~5 天）

```
□ 分类管理 API
□ 站点配置 API（含社交按钮、卡片布局、装饰图、背景图）
□ 博主收藏 API
□ 项目展示 API
□ 分享链接 API
□ 图片画廊 API
□ 代码片段 API
□ 关于页面 API
```

### Phase 3：点赞 + 前端联调（预计 2~3 天）

```
□ 点赞 API（含 IP 防刷）
□ 前端所有 service 改造
□ 前端认证改造（登录页）
□ 前端图片上传改造
□ LikeButton 组件改造
```

### Phase 4：SEO + 部署上线（预计 2~3 天）

```
□ Sitemap 动态生成
□ RSS Feed 动态生成
□ MySQL 安装 + 调优
□ Nginx 配置（HTTPS + 反代）
□ 后端 Jar 包部署 + systemd 服务
□ 冒烟测试
```

---

## 八、风险与对策

| 风险 | 概率 | 影响 | 对策 |
|------|------|------|------|
| MySQL 内存超预期 | 中 | 服务器 OOM | 预留 my.cnf 压榨配置，极端情况可降级到 H2 |
| Markdown 图片路径迁移 | 低 | 已有博客图片 404 | 后端返回时做 URL 兼容映射 |
| 前端改动影响现有页面 | 低 | 页面报错 | 仅替换 data layer，不改 UI 组件 |
| 原项目持续更新 | 中 | 无法合并上游 | Fork 后独立维护，改动面小不频繁冲突 |
| JVM 内存不足 | 低 | 服务崩溃 | -Xmx 参数预留调整空间 |

---

## 九、成功标准

1. 管理员可以通过前端登录、写博客、上传图片、发布，**发布后即时可见**
2. 所有现有模块（博主/项目/分享/图片/句子/关于）的编辑功能正常运行
3. 访客浏览博客体验与改造前**无差别**
4. 点赞功能正常，IP 防刷生效
5. Sitemap 和 RSS 正常输出
6. 服务器日常内存占用 **< 1.5GB**（留 500MB 余量）
7. 单篇博客发布耗时 **< 1 秒**
