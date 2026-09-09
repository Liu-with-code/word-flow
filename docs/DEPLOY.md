# WordFlow 部署指南（Docker）

## 架构

```text
浏览器 / 未来 App
      │
      ▼
┌─────────────┐   /api/*、/uploads/* 反向代理   ┌──────────────────────┐
│  frontend   │ ─────────────────────────────▶ │  backend (Java 17)   │
│  Nginx 静态  │                                │  Spring Boot :8080    │
└─────────────┘                                └──────────┬───────────┘
                                                          │
                                                          ▼
                                              ┌──────────────────────┐
                                              │  mysql:8.0           │
                                              │  库名 wordflow        │
                                              └──────────────────────┘
```

首次启动时 MySQL 容器会自动执行 `database/schema.sql`（建库建表 + 词书元数据），
真实词条由一次性任务 `import` 从 `database/raw/*.jsonl` 导入（约 4.5 万词条）。

## 一、快速开始

前置条件：安装 Docker Desktop（Windows）并确保可用；命令行执行 `docker --version` 有输出。

```bash
cd D:\Pogame Files(x86)\JavaCode\wordflow

# 1. 准备环境变量（生产务必修改 JWT_SECRET / 数据库口令 / AI Key）
copy .env.example .env

# 2. 构建并启动 MySQL + 后端 + 前端
docker compose up -d --build

# 3. 导入真实词库（一次性；幂等可重复执行）
docker compose --profile tools run --rm import

# 4. 打开
#    前端页面：http://localhost        （后端 API：http://localhost:8080）
```

`docker compose logs -f backend` 可查看后端日志。

## 二、环境变量（.env）

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | root | MySQL root 口令（生产必改） |
| `DB_USERNAME` / `DB_PASSWORD` | wordflow / wordflow | 应用数据库账号（生产必改） |
| `MYSQL_PORT` | 3306 | 宿主机映射端口（可改 13306 避免冲突） |
| `BACKEND_PORT` | 8080 | 后端 API 映射端口 |
| `FRONTEND_PORT` | 80 | 前端页面映射端口 |
| `JWT_SECRET` | 开发默认值 | **生产必须替换**，长度 ≥32 字节 |
| `JWT_EXPIRE_HOURS` | 168 | 登录有效期（小时） |
| `AI_PROVIDER` | mock | `mock`=离线模拟；`openai`=真实大模型 |
| `AI_API_KEY` | 空 | 大模型密钥（mock 模式可不填） |
| `AI_BASE_URL` | https://api.deepseek.com | OpenAI 兼容接口地址 |
| `AI_MODEL` | deepseek-v4-flash | 模型名 |
| `AI_TIMEOUT_SECONDS` | 60 | AI 请求超时 |

接入真实大模型示例（`.env`）：

```dotenv
AI_PROVIDER=openai
AI_API_KEY=sk-你的密钥
AI_BASE_URL=https://api.deepseek.com
AI_MODEL=deepseek-v4-flash
```

## 三、数据库脚本说明

- 全新部署：MySQL 数据卷为空时，容器自动执行
  [database/schema.sql](database/schema.sql)（一次建全 10 张表 + 8 本词书元数据）；
- 词条数据：`docker compose --profile tools run --rm import` 把
  `database/raw/*.jsonl`（KyleBing 词库）解析入库，可重复执行（upsert，不产生重复）；
- 若要把“本地已有库”迁进 Docker：先 `mysqldump` 导出再导入（见第五节）；
- 若升级的是**旧版已有库**（非新容器初始化），请按时间顺序执行 `database/` 下的
  `migrate_*.sql` 增量脚本：
  `migrate_book.sql` → `migrate_review_time.sql` → `migrate_user_time.sql`
  → `migrate_user_goals.sql` → `migrate_setup_prompt.sql`。

## 四、数据持久化

- MySQL 数据：命名卷 `mysql_data`；
- 用户头像等上传文件：命名卷 `uploads`（后端 `/app/uploads`），通过 `/uploads/**` 访问。

备份 / 恢复示例：

```bash
# 备份
docker compose exec mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" wordflow' > wordflow_backup.sql

# 恢复
cat wordflow_backup.sql | docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" wordflow'
```

## 五、常见问题

1. **80 端口被占用**：改 `.env` 的 `FRONTEND_PORT=8081` 等，然后 `docker compose up -d`；
2. **MySQL 起不来 / 乱码**：确认映射端口未冲突；容器内已强制 `utf8mb4_unicode_ci`；
3. **想要重置数据库**：`docker compose down -v` 会删除数据卷（**谨慎**，会清空全部数据），
   再 `docker compose up -d` 重新初始化；
4. **改了数据库迁移脚本后旧数据不更新**：新脚本只在空卷初始化时执行一次，
   老库请手动执行对应 `migrate_*.sql`；
5. **AI 报“服务未配置”**：检查 `.env` 中 `AI_PROVIDER`/`AI_API_KEY`，修改后
   `docker compose up -d backend` 重建生效；
6. **头像上传 413**：Nginx 已放开到 5MB，后端仍按 2MB 校验，图片建议压缩后再传。

## 六、后续移植 Android / 小程序

App 只需访问后端 API：`http://服务器IP:8080/api`，前端页面（Nginx）仅供 Web 使用；
建议给后端配 HTTPS 反向代理（Caddy/Nginx）后再供 App 生产调用。
