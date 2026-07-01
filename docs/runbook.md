# 本地运行手册

## 前置条件

- Docker Desktop 29+，启用 Docker Compose
- 可选：JDK 25、Maven 3.9+、Node.js 24、npm 11（用于脱离容器开发）

## 启动整套环境

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
```

首次启动会拉取镜像、构建三个应用镜像，并由 Flyway 自动执行数据库迁移。

首次启动前必须修改 `.env` 中的 `ADMIN_INITIAL_PASSWORD` 和 `JWT_SECRET`。当
`sys_user` 为空时，后端会创建一次性初始化管理员；创建成功后应从 `.env`
移除 `ADMIN_INITIAL_PASSWORD` 并重建后端容器。

如需启用媒体上传，还需配置 `OSS_REGION`、`OSS_ENDPOINT`、`OSS_BUCKET`、
`OSS_ACCESS_KEY_ID` 和 `OSS_ACCESS_KEY_SECRET`。访问密钥必须属于仅允许指定
Bucket 与 `OSS_OBJECT_PREFIX` 前缀读写的 RAM 用户；未配置时其他功能仍可正常启动，
管理端会明确显示 OSS 不可用。

## 常用检查

```powershell
docker compose ps
docker compose logs -f backend
docker compose exec mysql mysql -ublog -p personal_blog
docker compose exec redis redis-cli -a $env:REDIS_PASSWORD ping
```

入口：

- `http://localhost/`：Nuxt 公开站
- `http://admin.localhost/`：Vite 管理端
- `http://localhost/api/v1/status`：后端状态接口
- `http://admin.localhost/login`：管理员登录
- `http://localhost/actuator/health`：聚合健康状态

若端口 80 被占用，在 `.env` 设置 `HTTP_PORT=8088`，然后使用 `http://localhost:8088/`。

## IDEA 开发

1. 使用 IDEA 打开仓库根目录。
2. 将 `backend/pom.xml` 作为 Maven 项目导入，项目 SDK 选择 Java 25。
3. 仅启动依赖：`docker compose up -d mysql redis`。
4. 在 `BlogApplication` 的运行配置中设置：

   ```text
   MYSQL_PASSWORD=blog_dev_password
   REDIS_PASSWORD=blog_redis_password
   ```

5. 运行 `BlogApplication`。

前端开发：

```powershell
npm ci
npm run dev:web
npm run dev:admin
```

## 故障排查

- 后端启动失败：先运行 `docker compose ps`，确认 MySQL 和 Redis 为 `healthy`。
- Flyway 校验失败：不要修改已执行的迁移，新建更高版本迁移修复。
- `admin.localhost` 无法解析：访问 `http://localhost/admin`，网关会跳转；也可在 hosts 中将其指向 `127.0.0.1`。
- 密码修改后无法连接旧数据卷：环境变量只在 MySQL 首次初始化时生效。开发环境确认可丢弃数据后，删除卷再启动。
- 启动提示 `ADMIN_INITIAL_PASSWORD must be changed`：仍在使用示例密码，修改为至少 12 位的非占位密码。
- 登录成功但刷新失败：确认浏览器允许同站 Cookie，且 `ADMIN_ALLOWED_ORIGINS` 包含管理端的完整 Origin。
- 媒体页提示 OSS 未配置：检查上述 OSS 环境变量，并确认 AccessKey 已授权
  `PutObject`、`GetObject`、`DeleteObject` 和 `HeadObject` 的最小权限。
