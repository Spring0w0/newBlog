# NewBlog 全量 API 契约

> 目标：本文件是 `frontend/` 与 `backend/` 的长期接口边界，不只是当前 Controller 的说明。
> 状态标记：`已实现` 可直接联调；`实施中` 表示契约已确认、代码正在落地；`已设计` 是后续阶段必须遵守的接口契约，尚不代表已有实现。

## 1. 范围与阶段映射

| 阶段 | 模块 | API 契约状态 |
|---:|---|---|
| 0 | 前端 API Client、统一响应 | 已实现 |
| 1 | 账号密码登录、JWT、401/403 | 已实现 |
| 2 | 公开内容读取 | 已实现 |
| 3 | 图片/文件上传与静态资源映射 | 已实现 |
| 4 | 博客后台 CRUD、分类管理 | 已实现 |
| 5 | about、博主、项目、友链、相册、片段后台 CRUD | 已实现 |
| 6 | 站点配置、卡片样式后台保存 | 已实现 |
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
- API 中的 URL 字段一律使用可直接访问的公开绝对 URL。后端上传接口和公开读取接口依据 `app.upload.public-base-url` 生成该 URL，例如开发环境的 `http://localhost:8080/images/projects/a1b2.webp`，避免跨端口开发环境解析错误。后端数据库对受管文件仅保存 `/images/...` 相对公开路径，不持久化环境域名或 `localhost`。

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

文章管理端列表采用该分页结构。博主、项目、友链和相册属于当前规模较小的完整配置集合，管理端暂时返回完整数组；若未来引入分页，必须新增明确接口或先完成前端契约迁移，不能静默改变现有响应结构。

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
  name: string
  avatar: string
  url: string
  description: string
  stars: number
  status?: 'recent' | 'disconnected'
}

type Project = {
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

### 5.1 存储边界与文件元数据

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

文件元数据保存于 `file_assets` 表，不以 URL 直接操作物理文件：

| 字段 | 说明 |
|---|---|
| `id` | 稳定的文件 ID，用于删除和后续业务引用 |
| `scope` | 存储范围：`blog-images`、`site`、`bloggers`、`projects`、`shares`、`pictures` |
| `stored_filename` | 服务端生成的随机文件名，不使用客户端文件名作为路径 |
| `relative_path` | 相对 `uploads/` 的安全路径，例如 `blog-images/20260711-...webp` |
| `original_name`、`content_type`、`file_size`、`sha256` | 审计、校验与排障元数据；不暴露真实物理目录 |
| `width`、`height` | 可识别的栅格图片尺寸；当前格式无法可靠读取时允许为 `null` |

静态文件地址为 `GET /images/{scope}/{fileName}`。成功时返回文件流，不使用 `Result` 包装；文件不存在时按全局 `404` 规则返回，且不泄露磁盘路径。

### 5.2 `POST /api/admin/files/images` — 已实现

`multipart/form-data`：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `file` | 是 | 单个图片文件 |
| `scope` | 是 | `blog-images`、`site`、`bloggers`、`projects`、`shares`、`pictures` |

响应：

```json
{
  "fileId": 42,
  "url": "http://localhost:8080/images/projects/20260711-a1b2c3.webp",
  "fileName": "20260711-a1b2c3.webp",
  "originalName": "cover.webp",
  "size": 182304,
  "contentType": "image/webp"
}
```

规则：

- 单次仅接收一个 `file`，单个文件最大 **10MB**；超出限制返回 `413`。
- 仅支持 JPEG（`.jpg`/`.jpeg`）、PNG（`.png`）、GIF（`.gif`）和 WebP（`.webp`）。SVG 因脚本风险不接受；不支持的或 MIME/扩展名/实际文件签名不一致时返回 `415`。
- 服务端生成文件名，不使用客户端文件名作为磁盘路径；原始文件名只作为受限长度的元数据保存。
- 服务端同时校验声明 MIME、文件扩展名和文件签名，拒绝路径穿越、空文件和伪装图片。
- 成功响应新增 `fileId`，前端可直接使用响应中的绝对 `url` 进行预览和提交；后端保存业务数据时会将受管 URL 规范化为 `/images/...` 路径。删除始终使用 `fileId`。
- 编辑阶段主动从图片列表删除未引用文件时，前端使用 `fileId` 调用删除接口；因断网或业务保存失败产生的历史孤儿文件，后续阶段再补充定时清理策略。

### 5.3 `DELETE /api/admin/files/{fileId}` — 已实现

只允许管理员删除未被业务数据引用的文件；仍被文章、项目、友链、站点配置或相册引用时返回 `422`。引用检查同时覆盖文章封面/正文图片、站点配置、博主、项目、友链、相册、艺术图和背景图。文件不存在返回 `404`；删除成功的 `data` 为 `null`。具体 `fileId` 持久化方案随阶段 3 文件元数据表一并落地，不以 URL 直接删除文件。

## 6. 博客与分类管理接口（已实现）

所有接口均要求 `ADMIN`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/blogs` | 分页查询文章管理列表，返回数据库 `id`；支持可选 `slug` 精确筛选 |
| GET | `/api/admin/blogs/{id}` | 按数据库 ID 查询文章编辑详情 |
| POST | `/api/admin/blogs` | 新建文章 |
| PUT | `/api/admin/blogs/{id}` | 按数据库 ID 更新文章 |
| DELETE | `/api/admin/blogs/{id}` | 删除文章及关联 `blog_images` 关系 |
| DELETE | `/api/admin/blogs/batch` | 批量删除，body 为 `{ "ids": [1, 2] }` |
| PUT | `/api/admin/categories` | 整体保存分类及排序，body 为 `{ "categories": ["总结", "开源"] }` |

管理端文章列表使用统一分页结构：

```json
{
  "items": [{ "id": 42, "slug": "auto-tool", "title": "自动工具" }],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "totalPages": 1
}
```

`GET /api/admin/blogs` 的 `page` 默认 `1`、`pageSize` 默认 `20`，上限 `100`。管理端详情和创建/更新成功时返回以下结构：

```json
{
  "id": 42,
  "slug": "auto-tool",
  "title": "自动工具",
  "markdown": "# 自动工具",
  "tags": ["React"],
  "summary": "摘要",
  "category": "总结",
  "cover": "http://localhost:8080/images/blog-images/cover.webp",
  "hidden": false,
  "publishedAt": "2026-07-11T12:00:00",
  "imageUrls": ["http://localhost:8080/images/blog-images/content.webp"]
}
```

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
  "publishedAt": "2026-07-11T12:00:00"
}
```

- `title`、`slug`、`markdown` 必填；`slug` 仅允许小写字母、数字和连字符，长度 `3`–`120`，全局唯一，重复返回 `409`。
- 非空 `category` 必须存在于分类列表中；不存在返回 `422`。
- 正文图片引用以 `markdown` 中的 Markdown 图片语法为事实来源。服务端按图片首次出现顺序写入 `blog_images`；后端上传的 `blog-images` URL 会同步解析为 `file_assets` 引用。外部 URL 和历史静态 URL 保持兼容，但不会伪造文件引用。
- 删除单篇或批量删除文章会在同一事务中删除 `blog_images` 关联；随后可删除不再被任何业务数据引用的上传文件。
- 保存分类时，不允许直接删除仍被文章使用的分类，返回 `422`；调用方应先更新文章分类再保存分类顺序。
- 文章/分类写入成功后会失效后端的公开文章列表、文章详情和分类 Caffeine 缓存；前端管理页同时重新验证 `/api/blogs` 与 `/api/categories`。RSS、sitemap 目前直接消费公开 API，没有额外的应用内缓存副本。

## 7. 其他内容管理接口（迭代 D）

公开读取仍使用第 4.3 节接口；所有 `/api/admin/**` 写入接口均要求 `ADMIN` 与 Bearer Token。管理端列表项额外返回稳定数据库 `id`，前端不得再以名称、URL 或数组下标作为更新、删除依据。

| 资源 | 管理端接口 | 保存方式 | 状态 |
|---|---|---|---|
| about | `PUT /api/admin/about` | 单例整体替换 | 已实现 |
| snippets | `PUT /api/admin/snippets` | 列表整体替换及顺序保存 | 已实现 |
| bloggers | `GET/POST /api/admin/bloggers`、`PUT/DELETE /api/admin/bloggers/{id}` | 单项 CRUD，创建追加到末尾 | 已实现 |
| projects | `GET/POST /api/admin/projects`、`PUT/DELETE /api/admin/projects/{id}` | 单项 CRUD，创建追加到末尾 | 已实现 |
| shares | `GET/POST /api/admin/shares`、`PUT/DELETE /api/admin/shares/{id}` | 单项 CRUD，创建追加到末尾 | 已实现 |
| pictures | `GET/POST /api/admin/pictures`、`PUT/DELETE /api/admin/pictures/{id}` | 相册分组 CRUD；一组包含有序多图 | 已实现 |

### 7.1 关于页与片段

`PUT /api/admin/about` 请求与响应均为：

```json
{
  "title": "关于我",
  "description": "一句简介",
  "content": "# Markdown 正文"
}
```

`title`、`description`、`content` 均为必填文本。`PUT /api/admin/snippets` 使用：

```json
{ "items": ["第一句", "第二句"] }
```

片段列表至少一项，单项不能为空；请求数组顺序就是公开展示顺序。两个保存操作均在成功后失效对应公开缓存。

### 7.2 博主、项目与友链

管理端列表和创建/更新响应分别使用 `AdminBloggerVo`、`AdminProjectVo`、`AdminShareVo`，在公开字段基础上增加 `id`。请求体沿用公开字段名：

- 博主：`name`、`avatar`、`url`、`description`、`stars`、`status`；头像的受管 URL 必须属于 `bloggers` scope。
- 项目：`name`、`year`、`description`、`image`、`url`、`tags`、`github`、`npm`；图片的受管 URL 必须属于 `projects` scope。
- 友链：`name`、`logo`、`url`、`description`、`tags`、`stars`；Logo 的受管 URL 必须属于 `shares` scope。

名称、站点 URL、说明和必需图片不能为空；星级范围为 `0`–`5`，标签按数组顺序去重并保存。URL 可以是外部 HTTP(S) 地址、历史静态路径，或对应 scope 的受管 `/images/...` 地址；若提交受管地址，后端必须校验对应 `file_assets` 记录存在并建立引用。删除资源只删除业务记录与文件引用，不会直接删除上传文件。

### 7.3 相册

相册管理端响应使用 `AdminPictureVo`，在公开 `id`、`uploadedAt`、`description`、`images` 基础上返回稳定的分组 ID。创建请求不传 `id`，由后端生成；更新/删除使用路径中的 `id`。一组至少包含一张图片，最多 50 张，图片数组顺序就是展示顺序。受管图片必须属于 `pictures` scope；后端在 `picture_images` 关系表中保存图片 URL、排序和 `file_assets` 引用，删除分组或替换图片后会释放对应引用。

所有上述写入操作返回 `400`（参数不合法）、`401`、`403`、`404`（目标不存在）、`409`（唯一约束冲突）、`422`（受管文件 scope 不匹配或文件不存在）或 `500`；前端成功后必须重新验证对应公开 SWR key。

Flyway `V7__link_content_images_to_file_assets.sql` 为博主、项目、友链增加受管图片的 `file_assets` 外键，并创建 `picture_images` 关系表。迁移会回填能匹配到受管图片的历史 URL；旧静态路径或外部 URL 继续保留 URL 本身，但不会伪造文件引用。

## 8. 首页配置管理接口（已实现）

| 方法 | 路径 | 说明 |
|---|---|---|
| PUT | `/api/admin/site/config` | 保存完整站点配置，结构与 `GET /api/site/config` 相同 |
| PUT | `/api/admin/site/card-styles` | 保存完整卡片样式对象，结构与 `GET /api/site/card-styles` 相同 |
| PUT | `/api/admin/site/settings` | 原子保存站点配置与卡片样式，配置弹窗默认使用此接口 |

三个接口均要求 `ADMIN` 与 Bearer Token。`/config` 和 `/card-styles` 直接接收与各自公开读取接口相同的 JSON 对象；`/settings` 请求体为：

```json
{
  "config": { "meta": { "title": "我的站点", "description": "..." } },
  "cardStyles": { "hiCard": { "width": 360, "height": 288, "order": 1, "enabled": true } }
}
```

成功时 `/settings` 返回同结构。配置保存后必须：

1. 校验 `meta`、主题颜色、背景颜色、帽子索引、URL、图片引用、卡片 key 和数值范围；不支持的字段不应被静默丢弃。
2. 对 `faviconUrl`、`avatarUrl`、Art 图片、背景图片和社交二维码等受管 `/images/site/**` URL，校验 `file_assets` 元数据存在且 scope 为 `site`，数据库只保存规范的相对 URL；外部 URL 与历史静态 URL 保持兼容。
3. 在一个事务内更新站点 JSON、Art 图片、背景图片、社交按钮和卡片样式；图片/社交子表只作为站点配置中相应集合的关系镜像，避免旧引用阻止文件清理。
4. 失效公开站点配置与卡片样式缓存；响应和后续公开接口均返回环境对应的绝对受管图片 URL。

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

### 10.2 历史文章图片迁移

历史文章目录中的 `/blogs/{slug}/{file}` 图片通过显式迁移进入 `uploads/blog-images`，并在 `file_assets`、`blog_posts.cover_file_asset_id` 与 `blog_images` 中建立引用。迁移会将数据库中的历史 `/blogs/...` 和受管图片绝对 URL 规范化为 `/images/...`；公开 API 仍依据 `app.upload.public-base-url` 返回绝对 URL。

迁移不会自动执行。仅在确认源目录、数据库备份和上传目录可写后显式运行：

```powershell
cd D:\develop\newBlog\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.legacy-blog-image-migration.enabled=true --app.legacy-blog-image-migration.source-root=D:/develop/newBlog/frontend"
```

当前运行期上传只允许 JPEG、PNG、GIF 和 WebP；历史目录中的 SVG 会被迁移器跳过并保留原静态路径，不能通过此迁移绕过上传安全策略。

### 10.3 上传公开地址配置

`app.upload.public-base-url` 是必须配置的环境项，专门用于生成供浏览器访问的文件绝对 URL：

| 环境 | 配置 |
|---|---|
| 开发 | `http://localhost:8080` |
| 生产 | 环境变量 `PUBLIC_BASE_URL`，例如 `https://api.example.com` 或统一网关域名 |

生产环境缺少 `PUBLIC_BASE_URL` 会在启动时因配置校验失败，避免把内网地址或 `localhost` 写入 API 响应。反向代理无需依赖转发请求头来拼接文件地址。

### 10.4 缓存失效

| 修改资源 | 需要失效的读取数据 |
|---|---|
| 博客、分类 | 博客列表、文章详情、分类、RSS、sitemap |
| 站点配置、卡片样式 | 首页配置、metadata、RSS 站点信息 |
| 其他内容资源 | 对应公开列表 |
| 文件 | 文件元数据与引用资源缓存 |

### 10.5 契约变更

- 已公开字段不得删除或改名；新增字段必须可选并保持前端兼容。
- 改变数组/对象结构、身份认证方式、文件 URL 规则属于破坏性变更，必须先更新本文档和前端适配层。
- 当前阶段的旧静态文件仅作迁移来源、开发兜底和阶段 3 前的历史资源兼容，不再作为公开内容主数据源。
