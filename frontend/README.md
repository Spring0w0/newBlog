# NewBlog

NewBlog 是一个由 Next.js 前端、Spring Boot 后端、MySQL 和本地上传目录组成的个人博客。文章、站点配置、其他公开内容和图片元数据均以后端数据库为唯一事实来源；前端不再通过 GitHub App 或 GitHub API 读写内容。

## 本地运行

先准备 Java 21、Node.js（建议 20+）、pnpm 和 MySQL 8。创建空数据库 `newblog` 后，在项目根目录分别启动两个应用：

```bash
cd backend
./mvnw spring-boot:run

cd ../frontend
pnpm install
pnpm dev
```

默认地址为：前端 `http://localhost:2025`，后端 `http://localhost:8080`。后端开发配置会运行 Flyway 迁移，并将上传文件写入项目根目录的 `uploads/`。

开发环境初始管理员账号为 `admin`，密码为 `admin`，仅用于本地调试。生产环境不提供固定默认账号；首次启动时会使用环境变量创建首个管理员。

## 前端环境变量

复制 [`.env.example`](.env.example) 为 `.env.local` 后按环境填写：

| 变量 | 用途 |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | 浏览器访问后端的公开地址。|
| `INTERNAL_API_BASE_URL` | Next.js 服务端生成 RSS、sitemap 和 metadata 时访问后端的地址；生产环境可用内网地址。|
| `SITE_URL` | 用户实际访问博客的公开根地址，用于 canonical URL、RSS 与 sitemap。|

不要在前端环境变量中保存数据库密码、JWT 密钥或任何私钥。

## 生产部署

完整的容器化部署、GitHub Actions CI/CD、GitHub Environment、备份和回滚说明见项目根目录的 [DEPLOYMENT.md](../DEPLOYMENT.md)。

后端使用 `prod` 配置启动。必须提供以下环境变量：

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/newblog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=至少使用高强度随机密钥
INITIAL_ADMIN_USERNAME=你的管理员账号
INITIAL_ADMIN_PASSWORD=高强度管理员密码
CORS_ALLOWED_ORIGINS=http://www.spring0w04j.top
UPLOAD_ROOT_DIR=/data/newblog/uploads
PUBLIC_BASE_URL=http://www.spring0w04j.top
```

`SERVER_PORT`、`DB_POOL_MAX` 和 `DB_POOL_MIN_IDLE` 可按部署环境选填。首次启动空数据库时，Flyway 会创建空内容表、通用站点配置、默认卡片布局和由 `INITIAL_ADMIN_*` 指定的首个管理员；不会导入任何示例文章或其他示例内容。生产环境默认关闭 Swagger；健康检查仍可访问 `/actuator/health`。启动命令示例：

```bash
java -jar backend.jar --spring.profiles.active=prod
```

建议用 Nginx 或同类反向代理把前端作为默认站点，并将 `/api/`、`/images/` 和 `/actuator/health` 转发到后端。这样浏览器和后端使用同一个公开域名，`NEXT_PUBLIC_API_BASE_URL` 可以设置为该站点根地址；`INTERNAL_API_BASE_URL` 仍可指向后端内网地址。上线 HTTPS 时，应同步将 `SITE_URL`、`PUBLIC_BASE_URL` 和 `CORS_ALLOWED_ORIGINS` 改为 HTTPS 域名。

## 数据与备份

运行期上传文件位于项目根目录的 `uploads/`，与 `backend/`、`frontend/` 平级，且不会进入 Git。恢复完整网站需要同时恢复：

1. MySQL 数据库备份（含 Flyway 历史、内容、用户和文件元数据）；
2. 与该备份时间点匹配的整个 `uploads/` 目录。

只恢复数据库而不恢复上传目录会产生失效图片；只恢复上传目录而不恢复数据库会留下不可引用的文件。不要提交 `.env*`（示例文件除外）、`uploads/`、日志、构建产物或数据库备份。

## 公开衍生产物

`/rss.xml`、`/sitemap.xml`、`/robots.txt` 与页面 SEO metadata 均在 Next.js 服务端通过公开后端 API 生成，并使用短期缓存和故障降级。项目不再携带历史静态 JSON、文章目录或历史导入器；所有新的业务内容都由管理端 API 写入数据库。
