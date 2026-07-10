# 2025 Blog — 项目介绍

> 一个基于 Next.js 的个人博客系统，支持通过 GitHub App 从前端直接管理网站内容（CMS），无需本地开发环境即可发布博客和管理站点配置。

---

## 📋 目录

- [技术栈](#技术栈)
- [核心架构](#核心架构)
- [页面与路由](#页面与路由)
- [功能模块详解](#功能模块详解)
- [项目结构](#项目结构)
- [部署方式](#部署方式)

---

## 技术栈

| 类别 | 技术 |
|------|------|
| **框架** | Next.js 16 (App Router) + Turbopack |
| **UI 库** | React 19 + TypeScript |
| **样式** | Tailwind CSS 4 + `tailwindcss-animate` |
| **动画** | Motion (原 Framer Motion) |
| **状态管理** | Zustand |
| **数据请求** | SWR |
| **Markdown** | Marked + Shiki (代码高亮) + KaTeX (数学公式) |
| **认证** | jsrsasign (RSA-SHA256 JWT) — GitHub App 鉴权 |
| **部署** | OpenNext (Cloudflare) / Vercel |
| **通知** | Sonner (Toast) |
| **工具** | dayjs, clsx, tailwind-merge, ts-debounce, lucide-react |

---

## 核心架构

### 内容管理机制（GitHub CMS）

这是本项目最核心的设计理念：**通过前端页面直接编辑网站内容，无需登录服务器或手动修改代码**。

```
┌──────────────┐     JWT (RSA-SHA256)      ┌──────────────┐
│   浏览器前端   │ ──────────────────────────▶ │  GitHub API   │
│  (编辑/发布)   │ ◀────────────────────────── │  (仓库读写)    │
└──────────────┘     返回结果                 └──────┬─────────┘
                                                     │
                                              ┌──────▼─────────┐
                                              │  GitHub 仓库    │
                                              │ (内容存储)      │
                                              └──────┬─────────┘
                                                     │ Webhook/部署
                                              ┌──────▼─────────┐
                                              │ Vercel/Cloudflare│
                                              │ (自动部署)       │
                                              └────────────────┘
```

1. 用户创建 GitHub App 并配置仓库读写权限
2. 在前端导入 Private Key（PEM 格式）
3. 前端使用 RSA-SHA256 签名生成 JWT，调用 GitHub API 写入内容
4. GitHub 仓库内容更新后触发 Vercel/Cloudflare 自动部署
5. 博客内容存储在 `/public/blogs/` 目录下，以文件夹形式组织（`slug/config.json` + `slug/index.md`）

### 认证体系

- **GitHub App JWT 认证**：使用 `jsrsasign` 库生成 RS256 JWT Token
- **PEM 密钥管理**：支持 IndexedDB 本地缓存（通过 `isCachePem` 配置控制）
- **Zustand Auth Store**：全局认证状态管理
- 敏感操作（保存、删除）均需要先导入密钥验证身份

---

## 页面与路由

### 前台页面（访客可见）

| 路由 | 页面 | 说明 |
|------|------|------|
| `/` | 首页 | 拖拽式卡片布局，展示个人信息与内容入口 |
| `/blog` | 博客列表 | 支持分类筛选、搜索、排序的博客文章列表 |
| `/blog/[id]` | 博客详情 | Markdown 渲染的文章页面，含目录导航、点赞、代码高亮 |
| `/about` | 关于页面 | Markdown 渲染的个人介绍 |
| `/bloggers` | 博主导航 | 博主收藏列表，带星级评分 |
| `/projects` | 项目展示 | 个人项目作品集 |
| `/share` | 分享链接 | 友情链接/资源分享页面 |
| `/pictures` | 图片画廊 | 随机布局的图片展示墙 |
| `/clock` | 时钟 | 数字时钟页面（七段数码管风格） |
| `/snippets` | 一言 | 随机句子/摘录展示 |
| `/svgs` | SVG 画廊 | SVG 图标浏览与复制导入语句 |
| `/live2d` | Live2D 看板娘 | Live2D 角色模型展示 |
| `/image-toolbox` | 图片工具箱 | PNG/JPG 转 WEBP 在线工具 |
| `/wuthering-waves` | 鸣潮抽卡分析 | 《鸣潮》游戏抽卡记录可视化分析 |
| `/rss.xml` | RSS 订阅源 | 自动生成的 RSS Feed |
| `/sitemap.xml` | 站点地图 | 自动生成的 Sitemap |

### 后台页面（需认证）

| 路由 | 页面 | 说明 |
|------|------|------|
| `/write` | 新建文章 | Markdown 编辑器，支持图片上传、封面设置、标签管理 |
| `/write/[slug]` | 编辑文章 | 编辑已有博客文章 |

---

## 功能模块详解

### 1. 首页（Home）

首页采用**自由拖拽布局**，所有内容以卡片形式呈现，用户可以自定义每个卡片的位置和大小。

**卡片列表：**
- **ArtCard**（首图）— 展示可配置的装饰图片，支持多图轮播
- **HiCard**（中心）— 个人问候语与简介
- **ClockCard**（时钟）— 实时数字时钟（七段数码管显示）
- **CalendarCard**（日历）— 日期显示
- **MusicCard**（音乐）— 音乐播放器卡片
- **SocialButtons**（联系）— 社交媒体链接按钮（GitHub、微信、掘金、微博、B站、知乎等 15+ 平台）
- **ShareCard**（分享）— 友情链接展示
- **ArticleCard**（文章）— 最新博客文章入口
- **WriteButtons**（写作）— 写作入口按钮
- **NavCard**（导航）— 全局导航栏
- **LikeButton**（点赞）— 文章点赞按钮
- **HatCard**（帽子）— 装饰性帽子卡片
- **BeianCard**（备案）— ICP 备案信息展示

**特色功能：**
- 拖拽式可视化布局编辑
- 每个卡片可独立控制：宽度、高度、显示顺序、偏移量、启用/禁用
- 雪花飘落背景动画（SnowfallBackground）
- Liquid Grass 动态草地动画

### 2. 博客系统

**博客列表（`/blog`）：**
- 按分类筛选文章
- 博客封面图片悬停预览
- 支持搜索、排序
- 分类管理弹窗（拖拽排序分类、为文章分配分类）
- 后台可批量删除博客

**博客详情（`/blog/[id]`）：**
- 完整的 Markdown 渲染
- **代码语法高亮**（Shiki，主题 `one-light`）
- **数学公式渲染**（KaTeX，支持行内 `$...$` 和块级 `$$...$$`）
- 自动生成**目录导航**（TOC，支持 h1-h3）
- **图片点击放大**查看
- **点赞功能**（对接 Cloudflare Worker 后端，支持频率限制）
- 代码块一键复制

**Markdown 编辑器（`/write`）：**
- 分屏编辑与预览
- 封面图片上传与管理
- 正文图片上传（支持预览 URL、去重）
- 文章元信息配置：标题、日期、标签、摘要、分类、是否隐藏
- 基于 `localStorage` 的草稿自动保存
- 发布时通过 GitHub API 写入仓库

### 3. 站点配置系统

通过首页的配置按钮（⚙️），打开配置对话框，包含三个标签页：

**网站设置（Site Settings）：**
- 网站元信息：标题、描述、favicon、头像
- 首页装饰图片管理（Art Images）
- 首页背景图片管理（Background Images）
- 社交按钮配置（类型、链接、自定义图标）
- 帽子卡片内容
- ICP 备案号
- PEM 密钥缓存开关

**色彩配置（Color Config）：**
- 8 个 CSS 变量实时调节：
  - `--color-brand`（品牌色）
  - `--color-brand-secondary`（品牌次要色）
  - `--color-primary`（主文字色）
  - `--color-secondary`（次要文字色）
  - `--color-bg`（背景色）
  - `--color-border`（边框色）
  - `--color-card`（卡片背景色）
  - `--color-article`（文章背景色）
- 支持预览和实时生效

**首页布局（Home Layout）：**
- 所有卡片的尺寸、偏移、顺序、启用状态表格化管理
- 支持"进入主页拖拽布局"模式
- 一键重置布局

### 4. 资源管理

| 模块 | 路由 | 功能 |
|------|------|------|
| **博主收藏** | `/bloggers` | 博主信息卡片（名称、头像、链接、简介、星级评分），支持添加/编辑/删除 |
| **项目展示** | `/projects` | 项目卡片（名称、描述、链接、图片），支持添加/编辑/删除 |
| **分享链接** | `/share` | 友情链接（名称、链接、Logo 图片），支持卡片视图和网格视图 |
| **图片画廊** | `/pictures` | 图片上传与展示，支持单图和组图，随机瀑布流布局 |
| **SVG 画廊** | `/svgs` | 自动扫描 `/src/svgs/` 目录下所有 SVG 文件，点击复制 import 语句 |

### 5. 工具集

**图片工具箱（`/image-toolbox`）：**
- 拖拽/点击上传 PNG/JPG 图片
- 调整质量参数（30%-100%）
- 可选限制最大宽度
- 一键/批量转换为 WEBP 格式
- 原图与转换后对比预览
- 单张或批量下载

**鸣潮抽卡分析（`/wuthering-waves`）：**
- 解析鸣潮游戏的抽卡记录 JSON 数据
- 可视化展示每个五星的保底抽数
- 柱子宽度与抽数成正比

### 6. 动画与特效

- **雪花飘落背景** — 随机生成雪花（圆点 + 图片），无限循环飘落动画
- **Liquid Grass（液体草地）** — Three.js 流体模拟背景
- **Live2D 看板娘** — 可交互的 Live2D 角色模型展示
- **Motion 动画** — 全站使用 Motion 库实现流畅的页面过渡和交互动画
- **点赞粒子特效** — 点击点赞按钮时的心形粒子爆发动画

### 7. SEO 与站点健康

- **Sitemap 自动生成** — 包含所有博客文章和首页，支持自定义域名
- **RSS Feed** — 自动生成 RSS 2.0 订阅源
- **Meta 标签** — 动态网站标题和描述
- **静态生成** — 博客文章部分使用 `force-static` 策略

---

## 项目结构

```
2025-blog-public/
├── public/blogs/                  # 📁 博客内容（Markdown + 配置文件）
│   ├── categories.json           # 分类列表
│   ├── index.json                # 博客索引
│   └── {slug}/                   # 单篇博客目录
│       ├── config.json           # 博客配置（标题、日期、标签等）
│       ├── index.md              # 博客正文（Markdown）
│       └── *.webp/jpg/png        # 博客配图
├── scripts/
│   └── gen-svgs-index.js         # SVG 自动索引生成脚本
├── src/
│   ├── app/                      # 📁 Next.js App Router 页面
│   │   ├── (home)/               # 首页（路由组）
│   │   │   ├── page.tsx          #   首页主组件
│   │   │   ├── *-card.tsx        #   各类卡片组件
│   │   │   ├── config-dialog/    #   站点配置对话框
│   │   │   │   ├── index.tsx     #     配置对话框主组件
│   │   │   │   ├── color-config.tsx    #   色彩配置
│   │   │   │   ├── home-layout.tsx     #   布局配置
│   │   │   │   └── site-settings/      #   网站设置
│   │   │   ├── stores/           #   Zustand 状态管理
│   │   │   │   ├── config-store.ts     #   站点配置 Store
│   │   │   │   └── layout-edit-store.ts #  布局编辑 Store
│   │   │   └── services/         #   API 服务
│   │   ├── about/                # 关于页面
│   │   ├── blog/                 # 博客列表 + 详情
│   │   │   ├── [id]/page.tsx     #   博客详情页
│   │   │   ├── page.tsx          #   博客列表页
│   │   │   └── components/       #   分类模态框等
│   │   ├── write/                # Markdown 编辑器
│   │   │   ├── [slug]/page.tsx   #   编辑已有文章
│   │   │   ├── page.tsx          #   新建文章
│   │   │   ├── components/       #   编辑器组件
│   │   │   ├── hooks/            #   编辑器 Hooks
│   │   │   ├── services/         #   发布/删除服务
│   │   │   └── stores/           #   编辑器状态
│   │   ├── bloggers/             # 博主收藏
│   │   ├── projects/             # 项目展示
│   │   ├── share/                # 分享链接
│   │   ├── pictures/             # 图片画廊
│   │   ├── clock/                # 时钟页面
│   │   ├── live2d/               # Live2D 看板娘
│   │   ├── music/                # 音乐播放器
│   │   ├── snippets/             # 随机句子
│   │   ├── svgs/                 # SVG 画廊
│   │   ├── image-toolbox/        # 图片转 WEBP 工具
│   │   ├── wuthering-waves/      # 鸣潮抽卡分析
│   │   ├── rss.xml/              # RSS 生成
│   │   ├── sitemap.ts            # Sitemap 生成
│   │   └── layout.tsx            # 根布局
│   ├── components/               # 📁 通用组件
│   │   ├── blog-preview.tsx      #   博客预览
│   │   ├── blog-sidebar.tsx      #   博客侧边栏（含 TOC）
│   │   ├── blog-toc.tsx          #   目录导航
│   │   ├── card.tsx              #   卡片容器
│   │   ├── code-block.tsx        #   代码块（含复制功能）
│   │   ├── color-picker.tsx      #   颜色选择器
│   │   ├── dialog-modal.tsx      #   模态对话框
│   │   ├── like-button.tsx       #   点赞按钮
│   │   ├── markdown-image.tsx    #   Markdown 图片（可放大）
│   │   ├── nav-card.tsx          #   导航卡片
│   │   ├── liquid-grass/         #   液体草地动画
│   │   └── ...
│   ├── hooks/                    # 📁 自定义 Hooks
│   │   ├── use-auth.ts           #   认证 Hook
│   │   ├── use-blog-index.ts     #   博客索引
│   │   ├── use-markdown-render.tsx # Markdown 渲染
│   │   └── use-size.ts           #   尺寸监听
│   ├── layout/                   # 📁 布局组件
│   │   └── backgrounds/          #   背景特效
│   │       └── snowfall.tsx      #     雪花飘落
│   ├── lib/                      # 📁 工具库
│   │   ├── auth.ts               #   GitHub App JWT 认证
│   │   ├── aes256-util.ts        #   AES-256 加密工具
│   │   ├── github-client.ts      #   GitHub API 客户端
│   │   ├── markdown-renderer.ts  #   Markdown 渲染引擎
│   │   ├── file-utils.ts         #   文件哈希工具
│   │   ├── load-blog.ts          #   博客加载
│   │   └── utils.ts              #   通用工具函数
│   ├── svgs/                     # 📁 SVG 图标资源
│   │   └── index.ts              #   自动生成的图标索引
│   └── consts.ts                 # 全局常量
├── next.config.ts                # Next.js 配置
├── open-next.config.ts           # OpenNext（Cloudflare）配置
├── postcss.config.mjs            # PostCSS 配置
└── package.json                  # 项目依赖
```

---

## 部署方式

### 快速部署（Vercel）

1. Fork/Import 本项目到自己的 GitHub
2. 在 Vercel 中 Import 该项目，直接部署
3. 创建 GitHub App，配置仓库 `Contents` 读写权限
4. 生成 Private Key，设置环境变量：
   - `NEXT_PUBLIC_GITHUB_OWNER` — GitHub 用户名
   - `NEXT_PUBLIC_GITHUB_APP_ID` — GitHub App ID
5. 重新部署使环境变量生效
6. 在前端导入 Private Key 即可开始管理内容

### Cloudflare 部署

项目也支持通过 OpenNext 部署到 Cloudflare Workers：

```bash
pnpm build:cf    # Cloudflare 构建
pnpm preview     # 本地预览
pnpm deploy      # 部署到 Cloudflare
```

---

## 总结

2025 Blog 是一个功能丰富的**全栈个人博客系统**，其最大的创新在于**通过 GitHub API 实现前端自主管理内容**，让非技术用户也能轻松维护自己的博客网站。项目涵盖了博客写作、资源展示、工具集、动画特效等多个维度，是一个成熟且可开箱即用的个人网站解决方案。
