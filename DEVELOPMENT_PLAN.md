# NewBlog 全栈迭代开发计划

## 1. 计划定位

本计划取代已删除的 `FRONTEND_MIGRATION_PLAN.md`。项目不再按“前端先迁移完、后端再补齐”的方式推进，而是按一个可交付的业务闭环交替开发：

```text
接口与数据契约
      ↓
后端：迁移 → `pojo/entity` + Mapper → Service → Controller
      ↓
前端：API 适配 → 页面/交互 → 状态与错误处理
      ↓
联调、自动化测试、验收与旧链路清理
```

`backend/docs/API.md` 是跨端接口的唯一契约来源：先更新契约，再实现代码；接口实际实现状态也必须同步回写到该文档。

## 2. 当前基线

| 能力 | 状态 | 备注 |
|---|---|---|
| 统一响应、业务异常、JWT 鉴权、管理员登录 | 已完成 | 管理员初始账号为 `admin` / `admin` |
| Swagger / OpenAPI 接口文档 | 已完成 | 开发环境通过 `/swagger` 与 `/v3/api-docs` 提供文档；生产环境默认关闭 |
| 前端 API Client 与登录联调 | 已完成 | 浏览器端通过后端 API 访问数据 |
| 公开站点配置、文章、分类和其他内容读取 | 已完成 | 已按根级 `controller → service → mapper → pojo/entity + pojo/vo` 拆分并完成真实数据库联调 |
| 静态内容导入数据库 | 已完成 | 仅作为显式迁移工具，不参与正常运行时写入 |
| 图片上传与公开静态资源映射 | 已完成 | 六种 scope 的上传、公开访问、删除与前端各图片选择入口均已联调 |
| 管理端内容写入、GitHub CMS 移除 | 未开始 | 依赖文章及其他内容写入闭环 |

公开读取链路已完成规范化：Controller 只处理 HTTP 与参数边界并记录请求日志，Service 承载业务规则与 VO 组装，Mapper 只负责数据库访问。后续新增的管理端接口也必须遵循同一结构。

## 3. 全局开发规则

### 3.1 每个迭代的固定步骤

1. **更新契约**：明确 URL、权限、请求 DTO / 响应 VO、错误码、分页、缓存和兼容规则。
2. **确定数据变更**：使用 Flyway 新增迁移；不手工修改不同环境的表结构。
3. **后端实现**：按三层结构实现，并补充参数校验、权限控制、异常映射和服务层测试。
4. **前端接入**：所有请求经过统一 API Client；页面不直接访问数据库、文件系统、GitHub API 或新的散落 `fetch`。
5. **联调验收**：验证成功、未登录、无权限、参数错误、资源不存在和刷新后的数据一致性。
6. **收口旧逻辑**：新链路稳定后删除同一功能的旧静态/GitHub 写入分支，不长期保留双写。

### 3.2 数据、文件与安全边界

- 业务主数据以 MySQL 为唯一事实来源；旧 JSON 和 Markdown 只用于导入、历史资源兼容或短期开发兜底。
- 运行期上传文件位于工作区根目录 `uploads/`，与 `backend/`、`frontend/` 平级，不写入源码目录或 Git。
- 文件由后端以 `/images/**` 只读公开映射；上传、删除和引用检查只能走管理员 API。
- 管理写接口统一使用 `/api/admin/**`，要求 `ADMIN` 和 Bearer Token；公开读取接口不暴露管理字段、真实物理路径或凭据。
- `Result`、`ResultCode`、`BusinessException`、全局异常处理与 SLF4J 日志是所有新后端模块的共同基础，不新增各模块私有响应格式。

### 3.3 后端包结构与对象职责

后端按技术层统一归类；不保留 `publiccontent`、`user` 等领域顶级包，也不在领域子目录中重复创建 Controller、Service、Mapper 或 POJO：

```text
com.spring0w0.backend/
├── controller/       # 所有 HTTP Controller，按资源拆成 BlogController、SiteController 等
├── service/          # 所有业务 Service，按资源拆成 BlogService、SiteService 等
├── mapper/           # 所有 MyBatis-Plus Mapper
├── pojo/
│   ├── entity/       # 与数据库表映射的持久化对象
│   ├── dto/          # Controller 接收的请求对象
│   └── vo/           # Controller 返回给前端的响应对象
├── importer/         # 显式历史数据导入工具
├── config/           # OpenAPI 等全局框架配置
├── auth/
│   ├── config/       # JWT 与安全配置
│   └── security/     # 认证过滤器和安全响应处理器
├── common/           # 通用响应、JSON 解析等共享能力
└── exception/        # 统一异常定义与处理
```

- 同一资源的 Controller 与 Service 使用资源名配对，例如 `BlogController` / `BlogService`；不能再以 `PublicContentController` 或 `PublicContentService` 聚合无关资源。
- 所有 Controller 必须使用 `@Slf4j`，以中文记录请求参数和结果摘要；密码、Token、Cookie、Authorization 等敏感参数必须脱敏或省略。
- 所有 Controller 必须使用 Swagger 注解：Controller 标注中文 `@Tag`，接口标注 `@Operation` 和状态码说明；请求 DTO、响应 VO、统一 `Result` 标注 `@Schema`。新增管理接口时还必须标注 `BearerAuth` 安全要求。

- **Entity** 只能表达表结构和持久化字段，不能向前端直接返回。
- **DTO** 只表达输入，例如登录请求、上传请求、创建/更新命令；纯 GET 且没有请求体的接口不创建空 DTO。
- **VO** 只表达输出，例如登录结果、文章摘要和文章详情；返回字段以 API 契约为准，不能泄露 Entity 中的内部字段。
- 配置、异常、安全过滤器和显式迁移导入工具属于横切或基础设施代码，可保留在 `auth/config`、`auth/security`、`exception`、`importer` 等包中。

### 3.4 验收与提交原则

- 后端至少执行 Maven 测试；前端至少执行类型检查和生产构建；涉及页面或上传时补充实际联调验证。
- 一次提交只收口一个可验证闭环，迁移脚本、后端、前端和相关文档应在同一提交中保持一致。
- 不提交 `uploads/`、本地环境变量、日志、构建产物或数据库备份。
- 删除旧 GitHub CMS 代码只能在对应新写入链路通过联调后进行。

## 4. 迭代路线图

### 迭代 A：公开读取链路规范化（已完成，2026-07-11）

**目标**：保持现有前台功能不回退，将公开内容读取从临时 JDBC 聚合实现整理为可维护的后端三层架构。

完成记录：

- 已取消 `publiccontent` 目录；公开读取拆分为根级 `SiteController/SiteService`、`BlogController/BlogService`、`About`、`Blogger`、`Project`、`Share`、`Picture`、`Snippet` 等资源对，数据对象统一位于 `pojo/entity` 和 `pojo/vo`，线上请求链路不再使用 `JdbcTemplate`。
- 所有 Controller 已采用 `@Slf4j`，以中文记录请求参数和成功结果摘要；认证拒绝、权限拒绝、参数校验、业务异常和服务端异常均有安全日志，且密码、Token 等敏感数据不写入日志。
- 已通过后端 11 项测试、真实开发数据库的 11 个公开接口/登录/隐藏文章回归，以及前端类型检查和生产构建。

后端工作：

- 按资源拆分根级 Mapper、Entity、Service 和 VO；移除请求链路中 Service 直接拼接 SQL 的做法。
- 保持 [API 契约](backend/docs/API.md) 中已实现公开接口的路径和响应结构兼容。
- 校验隐藏文章不可公开读取、缺失资源返回 404、JSON 字段序列化和列表排序。
- 为公开文章、站点配置和内容列表补足服务层或 Web 层测试。

前端工作：

- 保持现有 `usePublicResource`、文章加载和配置 Store 的调用方式；只在 VO 契约对齐时做最小适配。
- 验证首页、文章列表、文章详情、关于、博主、项目、友链、相册、片段在刷新后均来自后端。

验收：

- 公开页面可访问，隐藏文章返回 404。
- 后端请求链路满足 `Controller → Service → Mapper`。
- Maven 测试、前端类型检查与生产构建通过。

### 迭代 B：图片与文件闭环（已完成，2026-07-11）

**目标**：让所有管理端图片都能安全上传、预览、引用和删除，不再通过 GitHub 提交二进制文件。

本轮完成记录：

- 已通过 Flyway `V5__create_file_assets.sql` 建立文件元数据表；文件只保存受限 `scope`、随机服务端文件名、相对路径、原始名、MIME、大小、SHA-256 与可读取尺寸，不保存或返回物理绝对路径。
- 已实现根级 `FileController` / `FileService` / `FileAssetMapper` / `FileReferenceMapper`，提供受 `ADMIN` 与 Bearer Token 保护的上传、删除接口，以及工作区根目录 `uploads/` 的 `/images/**` 只读映射。
- 已限制六种业务 scope，单文件最大 10MB，仅接受 JPEG、PNG、GIF、WebP；服务端同时核验声明 MIME、扩展名、真实文件签名与可读取图片内容，拒绝 SVG、空文件、伪装类型和路径穿越。
- 删除前会检查文章封面/正文、站点配置、艺术图、背景图、博主、项目、友链和相册引用；存在引用时返回 `422`，未引用文件同时删除元数据与物理文件。
- 前端已在统一 `file-api` 适配层封装 multipart 上传和删除；文章封面与正文图片选择后立即上传到 `blog-images`，编辑器只保留后端返回的绝对 URL / `fileId`，外部 URL 输入仍兼容。
- 已执行后端 20 项测试、前端类型检查和生产构建；独立 8082 联调验证了“管理员登录 → 上传 200 → 公开访问 200 → 删除 200 → 原 URL 404”。实际上传文件已删除，运行期文件未进入 Git。
- 已将同一上传能力复用到站点设置（Favicon、头像、艺术图、背景图和社交二维码）、博主、项目、友链和相册；它们分别使用 `site`、`bloggers`、`projects`、`shares`、`pictures` scope，保存后端绝对 URL，并在编辑状态保留 `fileId` 用于取消编辑或删除未保存条目时清理文件。
- 已对 `blog-images`、`site`、`bloggers`、`projects`、`shares`、`pictures` 六种 scope 完成真实联调：每种均上传 200、公开访问 200，删除后原 URL 返回 404；测试文件均已清理。

后端工作：

- 按 API 契约实现文件元数据表、Flyway 迁移、`FileAsset` Entity/Mapper/Service/Controller。
- 实现 `POST /api/admin/files/images`、`DELETE /api/admin/files/{fileId}` 和 `/images/**` 映射。
- 对 scope、图片真实类型、大小、文件名、路径穿越、引用状态和孤儿文件进行校验与处理。
- 将物理文件写入 `uploads/blog-images`、`uploads/site`、`uploads/bloggers`、`uploads/projects`、`uploads/shares`、`uploads/pictures`。

前端工作：

- 已在统一 API 层增加 multipart 上传能力和错误提示。
- 已接通文章封面与正文图片上传，并已复用到站点配置、博主、项目、友链和相册的上传组件。
- 所有新保存的业务数据只保存后端返回的公开 URL / 文件 ID，不自行拼接磁盘路径。

验收：

- 管理员可上传、立即预览并在刷新后正常访问图片。
- 非管理员无法上传或删除；被引用文件不可直接删除。
- 运行期文件不出现在 `backend/`、`frontend/` 或 Git 状态中。

### 迭代 C：文章与分类管理闭环

**目标**：完成 CMS 的核心写入能力——新建、编辑、删除和批量删除文章，以及分类排序。

后端工作：

- 实现文章和分类的管理员 DTO、校验、Mapper、Service 和 Controller。
- 实现文章 `slug` 唯一性、可见性、发布时间、标签、封面、Markdown、图片引用关系和批量删除事务。
- 提供 `POST/PUT/DELETE /api/admin/blogs`、批量删除与分类保存接口；写操作后失效相关文章、分类、RSS 和 sitemap 缓存。

前端工作：

- 将写文章、加载编辑文章、保存编辑、单篇删除、批量删除和分类保存服务从 GitHub API 切换到管理端 API。
- 编辑器和管理页使用数据库稳定 `id` 进行更新/删除，继续使用 `slug` 作为公开路由。
- 成功后更新或失效对应 SWR 数据，确保前台可立即看到变更。

验收：

- 管理员可完整创建、编辑、删除和批量删除文章，普通访客只能读取公开文章。
- slug 冲突、非法分类、未登录和无权限均有正确响应。
- 新文章中的上传图片可展示，删除文章后文件引用关系正确更新。

### 迭代 D：其他内容管理闭环

**目标**：将 about、片段、博主、项目、友链和相册从 GitHub/静态 JSON 写入迁移到后端 CRUD。

实施顺序：

1. 片段与关于页（结构简单、用于验证通用保存模式）。
2. 博主、项目、友链（单项 CRUD，含图片 URL）。
3. 相册（分组、排序和多图片引用）。

后端工作：

- 每类资源在根级技术层中定义对应的 Entity、Mapper、Service、管理 DTO 和公开 VO，避免重新引入单一 PublicContent 模块。
- 实现 API 契约中对应的 `/api/admin/**` 接口与事务、排序和文件引用检查。

前端工作：

- 将每个 `push-*` 服务替换为管理员 API 调用。
- 统一处理编辑态、本地预览、保存中状态、失败回滚和公开列表刷新。

验收：

- 六类内容均可由管理员增删改并在公开页面刷新后正确显示。
- 任何页面不再通过 GitHub commit 写入同类数据。

### 迭代 E：首页配置管理闭环

**目标**：将站点配置、卡片样式、背景图、艺术图和社交按钮的保存能力迁入数据库。

后端工作：

- 以现有公开配置 JSON 作为响应 VO，并为保存操作定义独立 DTO；先保持前端结构稳定，内部再按需要拆分持久化。
- 实现站点配置与卡片样式管理接口、字段校验、文件引用校验、原子保存和缓存失效。

前端工作：

- 将配置弹窗的 GitHub 保存逻辑切换到管理端接口。
- 支持上传后的图片 URL 写回配置；保存后同步 Store、CSS 变量和页面预览。

验收：

- 修改标题、描述、主题、卡片、社交项和图片后，刷新页面仍然生效。
- 非法颜色、URL、卡片 key 或无效文件引用被后端拒绝并向用户展示错误。

### 迭代 F：衍生产物、可观测性与旧链路清理

**目标**：让所有生产读取依赖后端数据，并彻底移除 GitHub CMS 运行时依赖。

工作内容：

- 实现或完善 `GET /api/auth/me`，用于前端恢复登录态。
- 让 RSS、sitemap 和页面 SEO metadata 在服务端消费公开 API，并配置缓存和失败兜底。
- 为管理写入、上传失败、权限拒绝和迁移工具补充结构化 SLF4J 日志；不得记录密码、Token 或文件物理路径。
- 清理 `github-client`、GitHub App 私钥入口、旧 `push-*` 实现和作为主数据源的静态 JSON 读取。
- 更新 README、部署说明、环境变量示例和数据备份/上传目录说明。

验收：

- 生产运行时不再调用 GitHub API，也不依赖 `frontend/public/blogs` 作为内容主数据源。
- `/rss.xml`、`/sitemap.xml`、站点 metadata 与公开文章数据一致。
- 全量测试、构建、关键用户路径回归和部署检查通过。

## 5. 依赖顺序与里程碑

| 里程碑 | 前置条件 | 完成标志 |
|---|---|---|
| M1：公开读取可维护 | 迭代 A | 公开读取三层化且联调通过 |
| M2：媒体可管理 | M1 | 上传、访问、引用、删除规则完整 |
| M3：文章 CMS 可用 | M2 | 文章和分类可独立于 GitHub 维护 |
| M4：全内容 CMS 可用 | M3 | 其他内容及首页配置均可后台维护 |
| M5：生产链路收口 | M4 | SEO/RSS/sitemap 完成，GitHub CMS 移除 |

不能跳过 M2 直接大规模实现带图片的内容写入；也不能在 M3/M4 未验证前删除对应的旧写入链路。

## 6. 每次开始新需求前的检查清单

- 该需求是否已在 `backend/docs/API.md` 定义？若没有，先补契约。
- 是否需要新的 Flyway 迁移、索引、唯一约束或文件引用关系？
- 是否能落在既有领域的 Mapper/Service 中，而不是继续堆进公共 Controller？
- 前端是否可复用统一 API Client、上传能力和缓存刷新机制？
- 是否需要兼容既有静态资源或导入数据？兼容期限是什么？
- 是否已定义成功、401、403、404、409、422 和 500 的用户可见行为？
- 是否有对应后端测试、前端构建和实际联调验收步骤？

## 7. 下一步

进入 **迭代 C：文章与分类管理闭环**。开始编码前，先在 [API 契约](backend/docs/API.md) 细化文章和分类管理 DTO、管理端分页结构、slug 冲突、图片 `fileId` / URL 引用关系、事务与缓存失效规则；随后按“Flyway 引用关系 → 管理端文章/分类接口 → 前端写作与管理页替换 GitHub 写入 → 成功、401、403、404、409、422 联调”的顺序实施。只有对应数据库写入链路通过完整联调后，才能移除同功能的 GitHub CMS 代码。
