# Wineclouds04 Website

Wineclouds04 是一个自托管的全栈个人博客：公开站提供文章浏览、搜索、归档、评论和订阅；管理端覆盖内容写作、媒体管理、评论审核、运营统计与站点资料维护。

仓库仅保存运行所需的源码、容器与部署配置，以及不含真实凭据的环境变量示例。真实配置、构建产物、日志、测试文件和设计资料均不应提交。

## 技术栈

- 公开站：Nuxt 4、Vue 3、TypeScript
- 管理端：Vue 3、Vite、Pinia、Element Plus
- API：Java 25、Spring Boot、MyBatis、Flyway
- 数据服务：MySQL 8、Redis 8
- 运行与网关：Docker Compose、Nginx

## 功能

- 文章、分类、标签、搜索、归档、RSS、Sitemap 与 SEO 页面
- Markdown 写作、草稿保存、定时发布、置顶、归档与文章审核
- 分类、标签、媒体、评论、回复和操作日志管理
- 访客浏览统计、热门内容与管理仪表盘
- JWT 鉴权、刷新令牌轮换、限流、验证码、安全响应头
- 可选的腾讯云 COS 媒体存储与 SMTP 邮件通知

## 项目结构

```text
.
├── backend/                  Spring Boot API、迁移脚本和 MyBatis Mapper
├── frontend/
│   ├── web/                  Nuxt 公开站
│   ├── admin/                Vue 管理端
│   └── packages/api-client/  前端共享 API 客户端
├── deploy/                   Nginx 与备份、恢复、发布脚本
├── docker-compose.yml        本地完整栈
├── docker-compose.prod.yml   生产编排
├── .env.example              本地环境变量示例
└── .env.production.example   生产环境变量示例
```

## 使用 Docker 启动（推荐）

### 1. 准备本地配置

```powershell
Copy-Item .env.example .env
```

仅在本地 `.env` 中填写数据库密码、JWT 密钥、初始管理员账号、COS 或 SMTP 配置。该文件已被忽略，不能提交到 Git。

### 2. 构建并启动

```powershell
docker compose up --build --wait
```

启动完成后可访问：

| 服务 | 地址 |
| --- | --- |
| 公开站 | http://localhost/ |
| 管理端 | http://admin.localhost/ |
| 管理端兼容入口 | http://localhost/admin/ |
| API 状态 | http://localhost/api/v1/status |
| API 文档 | http://localhost/docs |
| 健康检查 | http://localhost/healthz |

停止服务：

```powershell
docker compose down
```

## 本地开发

依赖：Node.js 24+、npm 11+、JDK 25、Maven 3.9+、MySQL 和 Redis。

```powershell
# 安装前端 workspace 依赖
npm ci

# 在一个终端启动 API
Set-Location backend
mvn spring-boot:run

# 在另外两个终端启动公开站和管理端
npm run dev:web
npm run dev:admin
```

开发服务器默认端口：公开站 `3000`、管理端 `5173`、API `8080`。前端需要通过本地 Nginx 网关访问 `/api`，或自行调整开发代理。

## 环境变量

复制示例文件后再填写真实值：

```powershell
Copy-Item .env.example .env
Copy-Item .env.production.example .env.production
```

生产部署至少需要设置：

- `MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD` 与 `REDIS_PASSWORD`
- 至少 32 字节的随机 `JWT_SECRET`
- 首次启动使用的 `ADMIN_INITIAL_USERNAME` 和 `ADMIN_INITIAL_PASSWORD`
- `PUBLIC_HOST`、`PUBLIC_WWW_HOST`、`ADMIN_HOST` 和对应 TLS 证书

COS、SMTP 与公开社交链接均为可选配置。以 `NUXT_PUBLIC_` 开头的变量会暴露给浏览器，绝不能存放密码或密钥。

## 生产部署

在服务器上创建 `.env.production`，放置 Nginx TLS 证书到 `deploy/certs/`，再使用部署脚本：

```bash
chmod +x deploy/scripts/*.sh
./deploy/scripts/deploy.sh <version>
```

生产编排仅由 Nginx 暴露 `80/443`；数据库、Redis、API 与前端服务保留在 Docker 网络内。

## 仓库安全规则

禁止提交以下内容：

- `.env`、`.env.production`、私钥、证书、Token、密码和云服务密钥
- `node_modules/`、`target/`、`.nuxt/`、`dist/`、日志、备份、压缩包与数据库文件
- 测试文件、审计/设计文档和本地 IDE 配置

提交前建议执行：

```powershell
git status --ignored
git diff --check
```

确认暂存区只包含源码、运行配置和示例文件后再推送。
