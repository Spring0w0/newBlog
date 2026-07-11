# NewBlog 全量 API 契约

> 目标：本文件是 `frontend/` 与 `backend/` 的长期接口边界，不只是当前 Controller 的说明。
> 状态标记：`已实现` 可直接联调；`已设计` 是后续阶段必须遵守的接口契约，尚不代表已有实现。

## 1. 范围与阶段映射

| 阶段 | 模块 | API 契约状态 |
|---:|---|---|
| 0 | 前端 API Client、统一响应 | 已实现 |
| 1 | 账号密码登录、JWT、401/403 | 已实现 |
| 2 | 公开内容读取 | 已实现 |
| 3 | 图片/文件上传与静态资源映射 | 已设计 |
| 4 | 博客后台 CRUD、分类管理 | 已设计 |
| 5 | about、博主、项目、友链、相册、片段后台 CRUD | 已设计 |
| 6 | 站点配置、卡片样式后台保存 | 已设计 |
| 7 | RSS、sitemap、SEO 数据消费 | 已设计 |
| 8 | 移除 GitHub CMS 旧链路 | 已设计 |

### 1.1 Swagger / OpenAPI 文档

- Swagger UI：`http://localhost:8080/swagger`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- 仅开发/联调环境启用 Swagger；生产 Profile 会同时关闭 Swagger UI 和 OpenAPI JSON，两个地址均返回统一的 `404` 响应。
- Swagger 只展示已实现的 `/api/**` 接口；本文件仍保留已设计但尚未实现的后续契约。
- 每个新增 Controller 必须添加中文 `@Tag`；每个接口必须添加 `@Operation`、成功响应说明，以及适用的 401、403、404、409、422 等错误响应说明。
- 请求 DTO、响应 VO 和统一 `Result` 必须使用 `@Schema` 说明字段语义；Entity 不进入 Swagger 的 HTTP 模型。
- 管理接口实现后，必须标记 `BearerAuth` 安全要求；Swagger 的 Authorize 输入格式为 `Bearer <accessToken>`。

## 2. 通用约定

### 2.1 基础地址与命名

- 所有业务接口以 `/api` 开头，不引入并行的 `/api/v1` 前缀。
- 本地后端：`http://localhost:8080`。
- 本地前端：`http://localhost:2025`。
- 前端通过 `NEXT_PUBLIC_API_BASE_URL` 配置后端基础地址；浏览器端不可依赖 Next.js `/api` 代理。
- JSON 字段使用 `lowerCamelCase`；时间使用 ISO-8601 字符串。
- URL 字段一律使用可直接访问的公开 URL。阶段 3 开始，后端上传接口返回**绝对 URL**，例如 `http://localhost:8080/images/projects/a1b2.webp`，避免跨端口开发环境解析错误。

### 2.2 统一响应

除静态文件流以外，所有 API 使用统一结构：

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

| HTTP / `code` | 默认语义 |
|---:|---|
| 200 | 请求成功 |
| 400 | 参数校验、请求体或文件字段不合法 |
| 401 | 未登录、Token 无效/过期、账号密码错误 |
| 403 | 账号禁用或权限不足 |
| 404 | 资源不存在，或公开接口不可访问隐藏资源 |
| 409 | 唯一键、slug 等数据冲突 |
| 413 | 上传文件超出限制 |
| 415 | 上传文件类型不支持 |
| 422 | 请求语义合法但无法处理，例如文件仍被业务数据引用 |
| 500 | 未预期服务端错误 |

业务错误必须由 `ResultCode` / `BusinessException` 返回，Controller 不直接拼装错误响应。

### 2.3 鉴权与权限

管理端请求必须附带：

```http
Authorization: Bearer <accessToken>
```

当前角色模型为单管理员角色 `ADMIN`：

| 路径 | 是否需要登录 | 权限 |
|---|---:|---|
| `/api/auth/**` | 否 | 公开 |
| `/api/site/**`、`/api/blogs/**` 等公开读取接口 | 否 | 公开 |
| `/api/admin/**` | 是 | `ADMIN` |
| `/images/**` | 否 | 公开静态资源 |

### 2.4 列表与分页

阶段 2 为兼容现有前端 UI，公开列表接口暂时返回完整数组。后续如文章数量增长需要分页，不能悄悄把现有数组改为对象；应新增明确的分页接口或完成前端契约迁移后再修改：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0,
  "totalPages": 0
}
```

管理端列表接口从设计之初采用该分页结构。

## 3. 认证与当前用户

### `POST /api/auth/login` — 已实现

请求体：

```json
{
  "username": "admin",
  "password": "admin"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJ..."
  }
}
```

### `GET /api/auth/me` — 已设计

用于前端刷新后确认 Token 仍然有效，并返回脱敏后的当前用户。

```json
{
  "id": 1,
  "username": "admin",
  "role": "ADMIN"
}
```

不返回 `passwordHash`、JWT 密钥、刷新令牌或其他凭据。

### `POST /api/auth/refresh` — 暂不纳入当前阶段

当前只签发 access token，不引入 refresh token、黑名单或多设备会话。若后续需要，必须先补充 refresh-token 持久化与吊销策略后再开放该接口。

## 4. 公开读取接口

公开读取接口不携带 Token，不暴露隐藏文章、密码字段、磁盘真实路径或管理审计信息。

### 4.1 站点与首页配置

| 方法 | 路径 | 状态 | `data` |
|---|---|---|---|
| GET | `/api/site/config` | 已实现 | 与原 `site-content.json` 兼容的站点配置 JSON |
| GET | `/api/site/card-styles` | 已实现 | 与原 `card-styles.json` 兼容的对象，key 为卡片名 |

站点配置保留 `meta`、`theme`、背景图、艺术图、社交按钮、备案、首页开关等前端展示字段。阶段 6 保存时直接使用相同结构，避免读写结构不一致。

### 4.2 文章与分类

| 方法 | 路径 | 状态 | 说明 |
|---|---|---|---|
| GET | `/api/blogs` | 已实现 | 所有公开文章，按发布时间倒序 |
| GET | `/api/blogs/{slug}` | 已实现 | 单篇公开文章详情；隐藏或不存在返回 404 |
| GET | `/api/categories` | 已实现 | 分类名称数组，按排序值返回 |

文章摘要结构：

```json
{
  "slug": "auto-tool",
  "title": "自动工具",
  "tags": ["python"],
  "date": "2026-05-09T22:31:00",
  "summary": "写一款自动工具",
  "cover": "http://localhost:8080/images/blog-images/cover.webp",
  "hidden": false,
  "category": "代码实现"
}
```

文章详情结构：

```json
{
  "slug": "auto-tool",
  "markdown": "# 自动工具\n\n文章正文……",
  "config": {
    "slug": "auto-tool",
    "title": "自动工具",
    "tags": ["python"],
    "date": "2026-05-09T22:31:00",
    "summary": "写一款自动工具",
    "cover": "http://localhost:8080/images/blog-images/cover.webp",
    "hidden": false,
    "category": "代码实现"
  }
}
```

阶段 2 迁移期间，历史 Markdown 里的 `/blogs/...`、`/images/...` 相对路径仍由前端静态资源兼容；阶段 3 后新上传文件必须使用后端上传接口返回的绝对 URL。

### 4.3 其他公开内容

| 方法 | 路径 | 状态 | 返回类型 |
|---|---|---|---|
| GET | `/api/about` | 已实现 | `{ title, description, content }` |
| GET | `/api/bloggers` | 已实现 | `Blogger[]` |
| GET | `/api/projects` | 已实现 | `Project[]` |
| GET | `/api/shares` | 已实现 | `Share[]` |
| GET | `/api/pictures` | 已实现 | `PictureGroup[]` |
| GET | `/api/snippets` | 已实现 | `string[]` |

字段契约：

```ts
type Blogger = {
  id?: number
  name: string
  avatar: string
  url: string
  description: string
  stars: number
  status?: 'recent' | 'disconnected'
}

type Project = {
  id?: number
  name: string
  year?: number
  description: string
  image?: string
  url?: string
  tags: string[]
  github?: string
  npm?: string
}

type Share = {
  id?: number
  name: string
  logo?: string
  url: string
  description?: string
  tags: string[]
  stars: number
}

type PictureGroup = {
  id: string
  uploadedAt?: string
  description?: string
  images: string[]
}
```

### 4.4 点赞 — 已设计

`likes` 表已存在，公开接口在实现时遵守以下契约：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/blogs/{slug}/likes` | 返回当前公开点赞计数 |
| POST | `/api/blogs/{slug}/likes` | 新增一次点赞；后端按 IP、User-Agent 和时间窗口限流 |

客户端不得自行传递 IP；后端从请求上下文获取，并避免在公开响应中暴露原始 IP。

## 5. 文件与图片接口（阶段 3）

### 5.1 存储边界

上传文件存储根目录必须位于工作区根目录，与 `backend/`、`frontend/` 同级：

```text
newBlog/
├── backend/
├── frontend/
└── uploads/
    ├── blog-images/
    ├── site/
    ├── bloggers/
    ├── projects/
    ├── shares/
    └── pictures/
```

- 运行期上传文件不保存到 `backend/`、`frontend/` 或 Git 仓库中。
- 开发环境物理根目录：`D:/develop/newBlog/uploads`。
- 当前文章图片配置 `D:/develop/newBlog/uploads/blog-images` 是上述根目录的子目录。
- 后端通过 `/images/**` 只读公开映射提供文件；物理路径、用户原始文件系统路径不进入 API 响应。

### 5.2 `POST /api/admin/files/images` — 已设计

`multipart/form-data`：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `file` | 是 | 单个图片文件 |
| `scope` | 是 | `blog-images`、`site`、`bloggers`、`projects`、`shares`、`pictures` |

响应：

```json
{
  "url": "http://localhost:8080/images/projects/20260711-a1b2c3.webp",
  "fileName": "20260711-a1b2c3.webp",
  "originalName": "cover.webp",
  "size": 182304,
  "contentType": "image/webp"
}
```

规则：

- 服务端生成文件名，不使用客户端文件名作为磁盘路径。
- 校验 MIME 类型、扩展名、文件大小与图片实际内容。
- 拒绝路径穿越、SVG 脚本风险和不支持的文件类型。
- 上传成功但业务保存失败时，后续阶段应有孤儿文件清理机制。

### 5.3 `DELETE /api/admin/files/{fileId}` — 已设计

只允许管理员删除未被业务数据引用的文件；仍被文章、项目、友链、站点配置或相册引用时返回 `422`。具体 `fileId` 持久化方案随阶段 3 文件元数据表一并落地，不以 URL 直接删除文件。

## 6. 博客与分类管理接口（阶段 4）

所有接口均要求 `ADMIN`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/blogs` | 新建文章 |
| PUT | `/api/admin/blogs/{id}` | 按数据库 ID 更新文章 |
| DELETE | `/api/admin/blogs/{id}` | 删除文章及关联 `blog_images` 关系 |
| POST | `/api/admin/blogs/batch-delete` | 批量删除，body 为 `{ "ids": [1, 2] }` |
| PUT | `/api/admin/categories` | 整体保存分类顺序，body 为 `{ "categories": ["总结", "开源"] }` |

文章写入 DTO：

```json
{
  "title": "文章标题",
  "slug": "article-slug",
  "markdown": "# Markdown",
  "summary": "摘要",
  "tags": ["React"],
  "category": "总结",
  "cover": "http://localhost:8080/images/blog-images/cover.webp",
  "hidden": false,
  "publishedAt": "2026-07-11T12:00:00",
  "imageUrls": ["http://localhost:8080/images/blog-images/content.webp"]
}
```

- `slug` 全局唯一，重复返回 `409`。
- `imageUrls` 用于维护 `blog_images` 关联，不从 Markdown 正则推断为唯一事实来源。
- 更新后公开列表、详情、RSS、sitemap 的缓存必须失效。

## 7. 其他内容管理接口（阶段 5）

读取仍使用第 4.3 节公开接口；写入统一位于 `/api/admin`。

| 资源 | 管理端接口 |
|---|---|
| about | `PUT /api/admin/about` |
| snippets | `PUT /api/admin/snippets`，body 为 `{ "items": ["..."] }` |
| bloggers | `POST /api/admin/bloggers`、`PUT /api/admin/bloggers/{id}`、`DELETE /api/admin/bloggers/{id}` |
| projects | `POST /api/admin/projects`、`PUT /api/admin/projects/{id}`、`DELETE /api/admin/projects/{id}` |
| shares | `POST /api/admin/shares`、`PUT /api/admin/shares/{id}`、`DELETE /api/admin/shares/{id}` |
| pictures | `POST /api/admin/pictures`、`PUT /api/admin/pictures/{id}`、`DELETE /api/admin/pictures/{id}` |

写入 DTO 与公开 DTO 保持同名字段；管理员创建后的响应必须额外返回稳定的数据库 `id`，供后续更新和删除使用。

## 8. 首页配置管理接口（阶段 6）

| 方法 | 路径 | 说明 |
|---|---|---|
| PUT | `/api/admin/site/config` | 保存完整站点配置，结构与 `GET /api/site/config` 相同 |
| PUT | `/api/admin/site/card-styles` | 保存完整卡片样式对象，结构与 `GET /api/site/card-styles` 相同 |

配置保存后必须：

1. 校验颜色、URL、图片引用、卡片 key 和数值范围。
2. 原子更新站点配置及关联图片/社交按钮/卡片样式。
3. 失效公开站点配置缓存与 SEO 元数据缓存。

## 9. RSS、sitemap 与 SEO（阶段 7）

阶段 7 不要求另造内容副本。前端服务端路由应消费已有公开接口：

| 前端产物 | 数据来源 |
|---|---|
| `/rss.xml` | `GET /api/blogs` 与 `GET /api/site/config` |
| `/sitemap.xml` | `GET /api/blogs` 中的公开 slug |
| 页面 `metadata` | `GET /api/site/config` 和文章详情 |

这类服务端读取应配置可控缓存和失败兜底；不能重新读取 `frontend/public/blogs` 作为主数据源。

## 10. 迁移、缓存与兼容性规则

### 10.1 旧内容导入

静态 JSON、文章 Markdown 和文章索引通过显式导入工具迁移到数据库。正常启动不会自动导入：

```powershell
cd D:\develop\newBlog\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.legacy-import.enabled=true --app.legacy-import.source-root=D:/develop/newBlog/frontend"
```

导入器仅用于迁移；后续管理端写入必须使用 `/api/admin/**`，不能继续修改 GitHub 或本地 JSON。

### 10.2 缓存失效

| 修改资源 | 需要失效的读取数据 |
|---|---|
| 博客、分类 | 博客列表、文章详情、分类、RSS、sitemap |
| 站点配置、卡片样式 | 首页配置、metadata、RSS 站点信息 |
| 其他内容资源 | 对应公开列表 |
| 文件 | 文件元数据与引用资源缓存 |

### 10.3 契约变更

- 已公开字段不得删除或改名；新增字段必须可选并保持前端兼容。
- 改变数组/对象结构、身份认证方式、文件 URL 规则属于破坏性变更，必须先更新本文档和前端适配层。
- 当前阶段的旧静态文件仅作迁移来源、开发兜底和阶段 3 前的历史资源兼容，不再作为公开内容主数据源。
