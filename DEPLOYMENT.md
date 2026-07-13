# NewBlog 简化生产部署

本方案面向一台低配置 Linux ECS 和一个维护者。GitHub Actions 只负责代码质量检查；生产服务器从公开 Git 仓库获取明确的版本标签，在本机顺序构建前后端镜像，再由 Docker Compose 启动服务。

## 1. 发布流程

```text
本地开发与推送
      ↓
GitHub CI：部署配置、后端测试、前端类型检查与构建
      ↓
创建 vX.Y.Z 标签
      ↓
ECS 获取标签源码
      ↓
顺序构建后端和前端本地镜像
      ↓
备份 → Compose 更新 → 健康检查 → 公网检查
```

本方案不依赖 GHCR、ACR、GitHub Environment、生产 SSH Secret 或 GitHub 自动 SSH 部署。

## 2. 持久化目录

```text
/srv/newblog/
├── .env             # 生产配置与密钥，权限 600
├── .release-state   # 最近一次验证成功的版本
├── source/          # 公开 GitHub 仓库工作区
├── uploads/         # 上传图片
└── backups/         # 数据库与图片备份
```

MySQL 数据保存在 Docker 卷 `newblog_mysql-data` 中。上传文件、数据库卷和 `.env` 都不进入 Git。Compose 同时为 MySQL、后端和前端设置了适合当前小内存 ECS 的运行上限，2GB Swap 只用于吸收短时峰值。

后端容器会自动使用执行部署命令的 Linux 用户 UID/GID，因此它能写入归属 `newblog-deploy` 的 `/srv/newblog/uploads`，又不会以 root 身份运行。健康检查也会验证上传目录可写。

## 3. 一次性服务器准备

服务器需要 Docker Engine、Docker Compose、Git 和 curl。创建并授权运行目录：

```bash
sudo install -d -o newblog-deploy -g newblog-deploy -m 750 \
  /srv/newblog /srv/newblog/uploads /srv/newblog/backups
```

生产 `.env` 以 `deploy/.env.production.example` 为模板，必须设置两个不同的 MySQL 密码、JWT 密钥和首个管理员凭据，并限制为 `600`：

```bash
chmod 600 /srv/newblog/.env
```

不要在终端日志、GitHub Issue 或聊天中输出 `.env` 内容。

使用部署用户克隆公开仓库：

```bash
git clone https://github.com/Spring0w0/newBlog.git /srv/newblog/source
install -m 700 /srv/newblog/source/deploy/scripts/deploy-version.sh /srv/newblog/deploy-version
ln -s /srv/newblog/source/deploy/scripts/rollback.sh /srv/newblog/rollback-version
```

`deploy-version` 被复制到工作区之外，切换 Git 标签时入口不会失效。仓库中的部署脚本本身以可执行模式提交；不要在服务器源码目录中直接修改、生成或保存其他文件，否则版本切换会因工作区不干净而停止。

服务器现有 `.env` 若仍保存旧 GHCR 镜像名，只需把两个非敏感值调整为：

```dotenv
BACKEND_IMAGE=newblog-backend
FRONTEND_IMAGE=newblog-frontend
```

部署脚本也会显式覆盖这两个值，确保只使用服务器本地镜像。

如果 ECS 运行了仅监听 loopback 的本机 HTTP 代理，可选设置：

```dotenv
BUILD_PROXY_URL=http://127.0.0.1:7890
```

部署脚本会先验证代理，再只为前端 `docker build` 的 RUN 步骤使用 host 网络和标准代理构建参数。该值不会进入前后端运行容器；未设置时仍直接使用 Maven/npm 国内镜像。因为 host 网络会让构建步骤访问宿主机 loopback，只应部署信任的仓库标签。

## 4. 首次发布

本地 `master` 的 CI 通过后创建标签：

```bash
git tag -a v1.0.2 -m "v1.0.2"
git push origin v1.0.2
```

然后在 ECS 执行一条命令：

```bash
/srv/newblog/deploy-version v1.0.2
```

脚本会依次执行：

1. 获取远程标签并确认它属于 `master`；
2. 确认标签使用兼容的部署接口，然后检出标签源码；
3. 若本机没有 `mysql:8.4`，通过 Docker Hub 镜像加速下载一次；
4. 通过阿里云 Maven 公共仓库、在 512MB Maven 堆限制下构建后端镜像；
5. 通过国内 npm 镜像、在 768MB Node.js 堆限制下构建前端镜像；
6. 确认两个本地镜像都存在；
7. 备份当前数据库和上传目录；
8. 使用 `--pull never` 启动 Compose，禁止尝试下载应用镜像；
9. 检查后端、前端和公开 HTTPS 地址；
10. 全部通过后记录当前版本。

两个构建是严格顺序执行的。部署前会确认 `/srv/newblog` 所在文件系统至少有 8GB 可用空间。构建失败发生在服务切换前，不会删除数据库卷或上传文件。已有站点在 Compose 启动、容器健康检查或公网检查任一环节失败时，脚本会同时恢复上一版本的源码配置和本地应用镜像；它不会逆向修改数据库迁移。

旧标签 `v1.0.0` 使用的是上一套自动镜像部署接口，简化流程会明确拒绝部署或回滚到它。`v1.0.1` 引入简化部署接口，`v1.0.2` 增加构建期本机代理支持。

## 5. 日常发布

每次发布：

1. 推送代码并等待 GitHub CI 通过；
2. 创建下一个版本标签；
3. 在服务器执行 `/srv/newblog/deploy-version <标签>`。

Git 只传输代码差异。Docker 会复用服务器上的基础镜像、Maven 依赖层和 pnpm 依赖层；小改动通常只消耗本地构建时间，不再依赖 ECS 到海外镜像仓库的大文件下载。

## 6. 备份

手动备份：

```bash
/srv/newblog/source/deploy/scripts/backup.sh
```

部署脚本也会在切换应用版本前自动调用备份。首次部署尚无 MySQL 容器时只归档上传目录；服务器一旦记录了在用版本，MySQL 未运行或数据库导出失败都会使备份返回失败并停止升级。备份默认保留 14 天，失败的临时 SQL 文件会自动清理。

每日 03:00 备份示例：

```text
0 3 * * * /srv/newblog/source/deploy/scripts/backup.sh >> /srv/newblog/backups/backup.log 2>&1
```

重要数据还应定期复制到服务器外。仅在同一块系统盘保存备份不能应对整台 ECS 丢失。

## 7. 本地镜像回滚

查看已构建的应用版本：

```bash
docker image ls newblog-backend
docker image ls newblog-frontend
```

回滚到仍保存在服务器上的版本：

```bash
/srv/newblog/rollback-version v1.0.2
```

回滚脚本不会访问远程镜像仓库，会在切换前再次备份，同时检出目标标签对应的 Compose 配置，然后验证容器和公网地址。若目标版本验证失败，它会恢复回滚前的源码配置和应用镜像。它不会逆向修改 Flyway 数据库迁移；数据库变更必须保持向前兼容。

## 8. Nginx 与网络

Nginx 对公网只提供 80 和 443，并反向代理：

- `/` → `127.0.0.1:3000`
- `/api/`、`/images/` → `127.0.0.1:8080`

MySQL 不映射宿主机端口。3000 和 8080 只绑定 loopback，不在 UFW 或云安全组中开放。

## 9. 数据安全红线

绝不要执行：

```bash
docker compose down -v
```

`-v` 会删除 `newblog_mysql-data` 数据卷。正常更新只使用 `docker compose up -d`，不会删除数据库数据。
