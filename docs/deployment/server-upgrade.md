# 既有服务器升级手册

本文适用于已经通过 `docker-compose.yml` 与
`docker-compose.prod.yml` 运行 Wineclouds04 Website 的 Linux 服务器。
升级过程会保留 MySQL、Redis 数据卷、`.env.production`、TLS 证书和外部备份。

## 本次升级说明

- 建议版本：`2026.07.25.1`
- 数据库迁移：无新增 Flyway 版本，现有数据库可以直接升级。
- 新增可选配置：`COS_MAX_AUDIO_SIZE=10485760`，缺省值为 10 MB。
- COS 已配置时，管理端可直接上传和替换 MP3。
- COS 未配置时，仍可填写站内路径或 HTTPS 音乐地址。
- 播放按钮固定显示在公共站顶栏；没有音乐时显示为禁用状态。

## 目录约定

以下示例假设：

- 项目目录：`/srv/wineclouds`
- 临时上传目录：`/tmp/wineclouds-release`
- 生产配置：`/srv/wineclouds/.env.production`
- TLS 证书：`/srv/wineclouds/deploy/certs`
- 数据库备份目录：由 `.env.production` 中的 `BACKUP_DIR` 指定，且位于项目目录外。

请按服务器实际路径替换这些示例。

## 1. 在开发机生成发布包

从项目根目录执行：

```powershell
$version = "2026.07.25.1"
pwsh .\deploy\scripts\package-release.ps1 -Version $version
Get-Content ".\artifacts\releases\wineclouds-deploy-$version.tar.gz.sha256"
```

发布脚本只打包白名单内的源码和部署文件，并拒绝常见密钥格式。
`.env.production`、证书、数据库备份、构建目录和 `node_modules` 不会进入发布包。

上传两个文件：

```powershell
scp ".\artifacts\releases\wineclouds-deploy-$version.tar.gz" `
  user@server:/tmp/wineclouds-release/
scp ".\artifacts\releases\wineclouds-deploy-$version.tar.gz.sha256" `
  user@server:/tmp/wineclouds-release/
```

## 2. 在服务器上检查当前状态

```bash
cd /srv/wineclouds
cat .release-version 2>/dev/null || true

docker compose \
  --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  ps

curl --fail --silent --show-error https://YOUR_PUBLIC_HOST/healthz
curl --fail --silent --show-error https://YOUR_PUBLIC_HOST/actuator/health
```

记录 `.release-version` 中的旧版本。回滚脚本需要旧版本镜像仍保留在本机或镜像仓库中。

确认以下持久化内容存在且不在待覆盖的发布包中：

```bash
test -s .env.production
test -s deploy/certs/public-fullchain.pem
test -s deploy/certs/public-privkey.pem
test -s deploy/certs/admin-fullchain.pem
test -s deploy/certs/admin-privkey.pem
```

## 3. 校验并同步发布包

```bash
version=2026.07.25.1
upload_dir=/tmp/wineclouds-release
stage_dir="$upload_dir/$version"

cd "$upload_dir"
sha256sum --check "wineclouds-deploy-$version.tar.gz.sha256"

rm -rf "$stage_dir"
mkdir -p "$stage_dir"
tar -xzf "wineclouds-deploy-$version.tar.gz" -C "$stage_dir"
```

先检查发布包内没有运行时机密：

```bash
test ! -e "$stage_dir/.env.production"
test ! -e "$stage_dir/deploy/certs"
```

同步源码。以下排除项会保留服务器上的生产配置、证书和当前成功版本记录：

```bash
rsync -a --delete \
  --exclude='.env.production' \
  --exclude='deploy/certs/' \
  --exclude='.release-version' \
  "$stage_dir/" /srv/wineclouds/
```

不要使用未经排除配置和证书的 `rm -rf` 或全目录覆盖。

## 4. 补充本次可选配置

如果 `.env.production` 中尚无该字段，可以添加：

```dotenv
COS_MAX_AUDIO_SIZE=10485760
```

该字段缺失不会阻止升级，Compose 会使用 10 MB 默认值。

要使用管理端 MP3 文件上传，还必须已有以下真实 COS 配置：

```dotenv
COS_REGION=ap-shanghai
COS_BUCKET=your-bucket-appid
COS_SECRET_ID=...
COS_SECRET_KEY=...
COS_OBJECT_PREFIX=blog/prod
```

## 5. 执行升级

```bash
cd /srv/wineclouds
chmod +x deploy/scripts/*.sh
./deploy/scripts/preflight.sh 2026.07.25.1
./deploy/scripts/deploy.sh 2026.07.25.1
```

`deploy.sh` 会依次完成：

1. 生产配置与证书预检；
2. 对现有 MySQL 创建加密备份；
3. 构建带不可变版本号的三个应用镜像；
4. 重新创建服务，并强制重建 Nginx 以重新渲染绑定挂载的配置模板；
5. 仅在成功后更新 `.release-version`。

发布期间 MySQL 与 Redis 数据卷不会删除。不要执行 `docker compose down -v`。

## 6. 发布后验收

```bash
docker compose \
  --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  ps

curl --fail --silent --show-error https://YOUR_PUBLIC_HOST/healthz
curl --fail --silent --show-error https://YOUR_PUBLIC_HOST/actuator/health
curl --fail --silent --show-error https://YOUR_PUBLIC_HOST/api/v1/public/profile
curl --fail --silent --show-error https://YOUR_PUBLIC_HOST/archive >/dev/null
```

人工检查：

1. 公开站首页和归档页正常打开；
2. 顶栏存在圆形播放按钮；
3. 管理端可以登录；
4. “站点资料 → 右侧播放器”可保存音乐配置；
5. COS 已配置时，可上传或替换 MP3；
6. 配置音乐后，顶栏按钮可以播放和暂停；
7. 浏览器阻止首次有声自动播放时，点击按钮可以开始播放。

查看异常日志：

```bash
docker compose \
  --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  logs --tail=200 nginx web admin-static backend mysql redis
```

## 7. 应用版本回滚

本次发布没有数据库迁移，可直接回滚三个应用镜像。假设升级前版本为
`2026.07.22.1`：

```bash
cd /srv/wineclouds
ROLLBACK_CONFIRM=2026.07.22.1 \
  ./deploy/scripts/rollback.sh 2026.07.22.1
```

回滚脚本只切换应用镜像，不会恢复数据库。对于未来包含不兼容数据库迁移的版本，
必须先完成隔离恢复演练，再决定数据库恢复或前向修复方案。

回滚后重新执行健康检查和关键页面验收。

## 8. 数据库恢复

仅在确认数据库本身需要恢复时使用。先在隔离数据库中演练：

```bash
./deploy/scripts/restore-drill.sh \
  /absolute/path/to/personal-blog-TIMESTAMP.sql.gz.enc
```

不要在未演练、未核对备份时间点和未安排停机窗口时覆盖生产数据库。
