# WordFlow 云服务器部署（复用已有 nginx / mysql 容器）

适用场景：服务器上已有官方 `nginx` 容器（占 80 端口）和 `mysql:8.0` 容器
（`mysql-container`，宿主 13306→3306），需要在此基础上部署本项目。

## 1. 服务器准备

1. 把整个项目上传到服务器（推荐放到 `/opt/wordflow`）：

```bash
# 本地执行（将 wordflow 目录传到服务器）
scp -r wordflow root@服务器IP:/opt/
```

2. 确认磁盘与网络：Docker 构建需要拉取 Maven/Node/Nginx 基础镜像与依赖，请保证可访问
   Docker Hub、Maven Central、npm registry。

3. 准备环境变量文件（上传后执行）：

```bash
cd /opt/wordflow
cp .env.example .env
vi .env        # 至少修改 JWT_SECRET、DB_PASSWORD；填 AI Key（如需真实大模型）
```

## 2. 准备 MySQL（复用 mysql-container）

> 你的 MySQL 容器已初始化过，官方“首次启动自动建库”不会再触发，需要手动建库。

```bash
# 在 mysql-container 中创建数据库与应用账号（root 口令替换为你的）
docker exec -i mysql-container mysql -uroot -p \
  -e "CREATE DATABASE IF NOT EXISTS wordflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; \
      CREATE USER IF NOT EXISTS 'wordflow'@'%' IDENTIFIED BY 'wordflow'; \
      GRANT ALL PRIVILEGES ON wordflow.* TO 'wordflow'@'%'; FLUSH PRIVILEGES;"

# 导入表结构与词书元数据
docker exec -i mysql-container mysql -uroot -p --default-character-set=utf8mb4 < database/schema.sql
```

`DB_PASSWORD` 若在 `.env` 改了，请把上面的 `'wordflow'` 换成对应值，保持一致。

## 3. 建网络并接入 MySQL

```bash
docker network create wordflow
# 把已有 MySQL 容器接入，并起别名 mysql（后端/导入任务通过 mysql:3306 访问它内部端口）
docker network connect --alias mysql wordflow mysql-container
```

## 4. 让出 80 端口

当前官方 `nginx` 占用了 80，与我们的前端容器冲突（前端镜像自带 Nginx + API 反代）：

```bash
docker stop nginx && docker rm nginx
```

如果这台 nginx 还承担其它站点，请改用别的端口部署前端
（改 `docker-compose.server.yml` 中 frontend 的 `ports: "8081:80"`），并把
`/api`、`/uploads` 反向代理到后端即可。

## 5. 构建并启动

```bash
cd /opt/wordflow
docker compose -f docker-compose.server.yml up -d --build

# 校验
docker compose -f docker-compose.server.yml ps
docker compose -f docker-compose.server.yml logs -f backend
```

访问：浏览器打开 `http://服务器IP`。

## 6. 导入真实词库（一次性）

```bash
docker compose -f docker-compose.server.yml --profile tools run --rm import
```

成功后 `learn_word` 约 4.5 万词条、`learn_book.word_count` 自动回写；重复执行安全。

## 7. 安全与运维

- **MySQL 不要对公网开放**：`13306` 端口仅应允许本机访问，或在云安全组限制来源 IP；
  容器之间走 `wordflow` 网络内部通信，不需要公网暴露 3306。
- 修改配置后重启：`docker compose -f docker-compose.server.yml restart backend`；
- 数据卷：MySQL 数据在 `mysql-container` 自身数据卷；头像文件在 `wordflow_uploads`；
- 备份：`docker exec mysql-container sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" wordflow' > backup.sql`；
- 首次导入词库前确认 `database/raw/` 已随项目上传（约 50MB，8 个 jsonl）。
