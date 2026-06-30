# 数据库基线

数据库迁移由 Flyway 管理，入口为：

```text
backend/src/main/resources/db/migration
```

`V1__baseline.sql` 建立设计文档中的第一版核心表、外键和查询索引。约定：

- 字符集 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`
- 时间使用 UTC `DATETIME(3)`
- 主键使用 `BIGINT`
- 迁移文件一旦被共享或部署不得修改，只能追加新版本
- Redis 不作为业务数据的最终来源

开发环境可查看迁移状态：

```powershell
docker compose exec mysql mysql -ublog -p personal_blog -e "select installed_rank, version, description, success from flyway_schema_history order by installed_rank;"
```

