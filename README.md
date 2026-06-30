# Personal Blog

依据《个人博客系统设计文档》实现的前后端分离个人博客。当前完成阶段一：工程与基础设施。

## 已完成

- Spring Boot 4.1 / Java 25 / MyBatis / Flyway 后端骨架
- Nuxt 4 公开站和 Vue 3 + Vite 管理端
- MySQL 9.7、Redis 8.8、Nginx 的 Docker Compose 编排
- V1 数据库基线、OpenAPI 3.1 骨架和 CI 流水线
- Actuator、容器健康检查和统一 `/api/` 反向代理

认证、文章、Markdown 和 OSS 属于阶段二，当前不提供业务接口。

功能范围与视觉方向参考 MIT 许可的 FeiTwnd-Website；本阶段未直接复用其代码或资源。

## 一键启动

1. 复制环境变量：

   ```powershell
   Copy-Item .env.example .env
   ```

2. 修改 `.env` 中的三个密码。
3. 构建并启动：

   ```powershell
   docker compose up --build -d
   ```

4. 访问：

   - 公开站：<http://localhost/>
   - 管理端：<http://admin.localhost/>
   - API 状态：<http://localhost/api/v1/status>
   - 健康检查：<http://localhost/actuator/health>

停止环境：

```powershell
docker compose down
```

如需连同本地数据库卷一起清除，明确确认数据不再需要后再执行 `docker compose down -v`。

更多说明见 [本地运行手册](docs/runbook.md) 和 [数据库说明](docs/database.md)。
