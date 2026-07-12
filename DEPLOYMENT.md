# NewBlog 部署与 CI/CD

本文对应仓库内的 `compose.prod.yaml`、`deploy/` 和 `.github/workflows/`。它将个人博客部署为：一台 Linux 服务器上的 Nginx、Docker Compose、MySQL、Spring Boot 和 Next.js。

## 1. 发布流程

```text
功能分支 / Pull Request
        ↓
CI：后端测试与打包、前端类型检查与构建
        ↓
合并 master
        ↓
推送版本标签 vX.Y.Z
        ↓
构建并推送 GHCR 前端/后端镜像
        ↓
手动触发 production 部署并通过 GitHub Environment 审批
        ↓
服务器备份 → 拉取指定版本 → Flyway 迁移 → 容器健康检查 → 公开地址检查
```

`master` 的每次提交只会触发 CI；不会自动改动公开网站。只有已经存在的版本标签才能进入生产部署。发布镜像同时拥有 `vX.Y.Z` 和 `sha-<Git SHA>` 两个不可变标签。

## 2. 仓库内已经提供的文件

| 文件 | 作用 |
|---|---|
| `backend/Dockerfile` | 使用 Java 21 构建并运行 Spring Boot 后端。 |
| `frontend/Dockerfile` | 在 Linux 构建阶段使用 Next.js standalone 输出，运行镜像仅带运行所需文件。 |
| `compose.prod.yaml` | 启动 MySQL、后端与前端；MySQL 不开放公网端口。 |
| `deploy/.env.production.example` | 生产 `.env` 模板，不含任何真实密钥。 |
| `deploy/nginx/newblog.conf` | Nginx 反向代理模板；后端仅监听服务器 loopback。 |
| `deploy/scripts/backup.sh` | 备份数据库和上传目录。 |
| `deploy/scripts/deploy.sh` | 部署新版本，失败时自动回滚到上一版本。 |
| `deploy/scripts/rollback.sh` | 手动回滚到指定已发布版本。 |
| `.github/workflows/ci.yml` | PR 与 `master` 的自动校验。 |
| `.github/workflows/release.yml` | 版本标签构建并发布镜像到 GHCR。 |
| `.github/workflows/deploy-production.yml` | 手动触发、经 production Environment 保护的生产部署。 |

## 3. 一次性准备服务器

以下示例以 Ubuntu LTS 为例。服务器需要已安装 Docker Engine、Docker Compose Plugin、Nginx、Certbot、Git 和 `curl`。

创建专用部署用户和持久化目录：

```bash
sudo useradd --create-home --shell /bin/bash newblog-deploy
sudo usermod -aG docker newblog-deploy
sudo install -d -o newblog-deploy -g newblog-deploy -m 750 \
  /srv/newblog /srv/newblog/uploads /srv/newblog/backups
```

将仓库中的生产环境模板复制到服务器，填写真实值后限制权限：

```bash
sudo -u newblog-deploy cp /path/to/newBlog/deploy/.env.production.example /srv/newblog/.env
sudo -u newblog-deploy chmod 600 /srv/newblog/.env
sudo -u newblog-deploy editor /srv/newblog/.env
```

其中必须替换的值包括：

- `MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `INITIAL_ADMIN_USERNAME`、`INITIAL_ADMIN_PASSWORD`
- 所有 `https://www.spring0w04j.top` 相关值（若最终域名不同则统一替换）

`/srv/newblog/uploads` 保存真实图片；MySQL 数据位于 Docker 名为 `newblog_mysql-data` 的卷中。**不要执行 `docker compose down -v`**，否则会删除数据库卷。代码镜像更新不会覆盖这两类持久化数据。

如果 GHCR 镜像保持私有，服务器需要使用仅有 `read:packages` 权限的专用令牌登录一次：

```bash
sudo -u newblog-deploy docker login ghcr.io -u spring0w0
```

如果将两个镜像包设为公开，则不需要服务器拉取凭据。无论哪种方式，服务器都不应保存 GitHub 代码仓库的写权限凭据。

## 4. 配置 Nginx 与 HTTPS

将 `deploy/nginx/newblog.conf` 复制为 `/etc/nginx/sites-available/newblog`，启用站点并检查配置：

```bash
sudo cp deploy/nginx/newblog.conf /etc/nginx/sites-available/newblog
sudo ln -s /etc/nginx/sites-available/newblog /etc/nginx/sites-enabled/newblog
sudo nginx -t
sudo systemctl reload nginx
```

在 DNS 将 `www.spring0w04j.top` 指向服务器公网 IP 后，使用 Certbot 为该站点申请证书：

```bash
sudo certbot --nginx -d www.spring0w04j.top
```

Certbot 会将 HTTP 站点改为 HTTPS 并设置重定向。Nginx 只会把 `/api/` 和 `/images/` 转发到后端，默认页面转发到前端；`/actuator/health` 只允许服务器本机访问，不对公网开放。

## 5. 配置 GitHub Actions

### 5.1 仓库 Variables

在 GitHub 仓库的 **Settings → Secrets and variables → Actions → Variables** 中创建：

| Variable | 示例 |
|---|---|
| `PRODUCTION_SITE_URL` | `https://www.spring0w04j.top` |
| `PRODUCTION_API_BASE_URL` | `https://www.spring0w04j.top` |
| `PRODUCTION_DEPLOY_HOST` | 服务器公网 IP 或主机名 |
| `PRODUCTION_DEPLOY_PORT` | `22` |
| `PRODUCTION_DEPLOY_USER` | `newblog-deploy` |

其中前两个值会在前端镜像构建时写入公开配置，因此只能放公开域名，绝不能放数据库密码、JWT 密钥或管理员密码。

发布构建阶段不会请求线上后端；构建时服务端公开读取会走已有的降级逻辑，容器运行后则使用 Compose 注入的 `http://backend:8080`，并按既有 300 秒缓存窗口重新校验公开数据。

### 5.2 production Environment

在 **Settings → Environments** 创建 `production`。建议限制仅版本标签可部署，并开启部署前人工确认（可用性取决于仓库可见性和 GitHub 套餐）。在该 Environment 内添加：

| Secret | 内容 |
|---|---|
| `PROD_SSH_KEY` | `newblog-deploy` 用户的专用 Ed25519 私钥。 |
| `PROD_SSH_KNOWN_HOSTS` | 服务器已核验的 SSH host key 行。 |

先在可信网络中核验服务器 SSH 指纹，再生成 `PROD_SSH_KNOWN_HOSTS`；不要让工作流临时执行未经验证的 `ssh-keyscan`。服务器端则只将该密钥的公钥写入 `newblog-deploy` 的 `~/.ssh/authorized_keys`。

最后在 `master` 开启分支保护，至少要求 `Backend test and package` 与 `Frontend type check and build` 两个 CI 检查通过。

## 6. 首次发布与日常发布

首次发布前，先确认 GitHub Variables、production Secrets、服务器 `.env`、Nginx 和 HTTPS 都已就绪。创建并推送标签：

```bash
git switch master
git pull --ff-only
git tag -a v1.0.0 -m "v1.0.0"
git push origin v1.0.0
```

`Publish release images` 工作流会重新运行 CI，通过后向 GHCR 推送前端和后端镜像。随后在 GitHub Actions 中手动运行 `Deploy production`，输入同一个 `v1.0.0` 标签。部署脚本会：

1. 备份当前 MySQL 和整个 `uploads/` 目录；
2. 拉取指定的两个应用镜像；
3. 启动 Compose 服务，后端会自动运行 Flyway；
4. 等待后端 `/actuator/health` 与前端首页容器检查通过；
5. 从 GitHub Runner 检查首页、公开站点配置、RSS 和 sitemap；
6. 若容器健康检查失败，自动拉回上一版镜像。

第一次空库启动时会生成通用站点配置和首个管理员，但不会创建任何示例文章或示例内容。

## 7. 回滚、迁移和备份

在服务器上手动回滚代码：

```bash
cd /srv/newblog
./deploy/scripts/rollback.sh v1.0.0
```

脚本回滚的是前后端镜像。Flyway 迁移必须遵守向前兼容原则：常规发布不要编写破坏旧代码运行的迁移；若必须恢复数据库内容，应使用部署前创建的 MySQL 备份，并同时恢复同一时间点的 `uploads/` 归档。

建议再添加每日备份任务，例如：

```bash
0 3 * * * /srv/newblog/deploy/scripts/backup.sh >> /srv/newblog/backups/backup.log 2>&1
```

部署前备份保留 14 天。重要数据还应定期复制到服务器外的对象存储或另一台机器；仅把备份留在同一块服务器磁盘不能应对服务器丢失。

## 8. 本地验证生产编排

不使用真实生产密钥时，可先复制模板并替换为临时测试值，然后只检查 Compose 插值结果：

```bash
docker compose --env-file deploy/.env.production.example -f compose.prod.yaml config
```

容器镜像验证使用：

```bash
docker build -t newblog-backend:test backend
docker build \
  --build-arg NEXT_PUBLIC_API_BASE_URL=https://example.invalid \
  --build-arg SITE_URL=https://example.invalid \
  -t newblog-frontend:test frontend
```

生产 `.env`、上传目录、数据库卷、备份和任何 SSH 私钥都不进入 Git。
