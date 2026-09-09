# WordFlow 词流

**输出倒逼输入的智能背单词系统**：先看完整示例，再“看英选义 → 看义选英 → 英译中 → 中译英”，全部通过后由 AI 生成包含今日单词的短文做全文翻译，正确率 ≥ 60% 才算完成；复习模块按艾宾浩斯遗忘曲线生成短文，越容易忘的单词出现频率越高。

> 开发环境默认使用离线模拟模式，无需 API Key 即可跑通全流程；接入真实大模型只需配置 `ai.*` 相关参数。

## 技术栈

| 端 | 技术 | 说明 |
| --- | --- | --- |
| 后端 | Java 17 + Spring Boot 3.3 + MyBatis-Plus + MySQL 8 | JWT 无状态认证、SpringDoc 自动文档 |
| 前端 | Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus | 响应式布局，移动端底部导航 |
| AI | OpenAI 兼容 Chat Completions（可换 DeepSeek / 通义千问） | 翻译批改、句子生成、短文生成 |
| 数据库 | MySQL 8.0，10 张表 | 用户 / 词书 / 词库 / 进度 / 计划 / 流水 / 短文 |

## 目录结构

```text
wordflow/
├── README.md                  # 本文件
├── docs/
│   ├── ARCHITECTURE.md        # 框架图与模块说明
│   ├── API.md                 # 接口文档
│   └── SPECS.md               # 引用的开发规范与落地说明
├── database/
│   ├── schema.sql             # 建库建表脚本（含词书表）
│   ├── migrate_book.sql       # 旧库升级为词书模式的增量脚本
│   ├── seed_words.sql         # 40 个示例单词（已并入四级词书）
│   ├── raw/                   # 下载的 JSONL 原始词书（KyleBing/english-vocabulary）
│   └── init-db.cmd            # 一键初始化数据库
├── backend/                   # Spring Boot 后端
└── frontend/                  # Vue3 前端
```

## 快速开始

### 1. 初始化数据库

```bat
cd database
init-db.cmd
```

脚本默认使用 `root / root`，与你的本机环境一致；若密码不同，请修改 `init-db.cmd` 与
`backend/src/main/resources/application.yml` 中的 `spring.datasource.password`。

如果是从旧版本升级（数据库已存在），请先执行增量迁移：

```bat
cd database
mysql -uroot -proot --default-character-set=utf8mb4 < migrate_book.sql
```

### 1.5 （可选）导入完整词书

`database/raw/` 下已下载好 8 本真实词书（四级 / 六级 / 考研 / 托福 / 雅思 / GRE / 高中 / 初中），
首次启动前在 `backend/` 目录执行一次导入（幂等，可重复执行）：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=import -Dspring-boot.run.arguments="--wordflow.import-dir=../database/raw"
```

导入完成后，`learn_word` 将包含数万条带音标、释义、例句的真实词条，
`learn_book.word_count` 会被自动回写。词书原始数据来自
[KyleBing/english-vocabulary](https://github.com/KyleBing/english-vocabulary)（MIT 协议，仅供学习）。

### 2. 启动后端

> 注意：后端命令必须进入 `backend/` 目录执行，不要在项目根目录直接运行 `mvn`。

```bash
cd backend
mvn spring-boot:run
```

启动后：

- 接口地址：http://localhost:8080/api
- Swagger 文档：http://localhost:8080/swagger-ui.html

### 3. 启动前端

> 注意：前端命令必须进入 `frontend/` 目录执行，不要在 `backend/` 或其他目录直接运行 `npm`。
> 建议打开两个终端窗口分别启动前后端。

```bash
cd frontend
npm install   # 仅第一次需要；已安装过可跳过
npm run dev
```

如果命令报 `No plugin found for prefix 'spring-boot'`，说明你在没有 `pom.xml` 的目录（如项目根目录）运行了 `mvn`；
如果报 `Could not read package.json`，说明你在没有 `package.json` 的目录运行了 `npm`。请先用 `cd` 进入对应子目录再执行。

浏览器打开 http://localhost:5173 ，注册账号即可体验完整学习流程。

## AI 配置

默认 `ai.provider=mock`，无需任何 Key，可完整离线体验（批改为规则算法，句子/短文为模板生成）。

接入真实大模型（以 OpenAI 兼容接口为例）：

```yaml
ai:
  provider: openai
  base-url: https://api.openai.com/v1   # 也可换成 DeepSeek / 通义千问等
  model: gpt-4o-mini
```

并在启动后端前设置环境变量：`AI_API_KEY=sk-xxxx`。

## 核心流程

### 词书选择

用户先进入「词书」页选择一本当前词书（默认四级），「今日学习」开始时从该词书
中抽取未学单词。当天已经开始的学习计划不受换书影响，次日生效。

```mermaid
flowchart LR
    A[单词完整示例] --> B[看英文选中文]
    B -- 错 --> A
    B -- 对 --> C[看中文选英文]
    C -- 错 --> A
    C -- 对 --> D[英文短句译成中文 AI 批改]
    D -- 不通过 --> D
    D -- 通过 --> E[中文短句译成英文 AI 批改]
    E -- 不通过 --> E
    E -- 通过 --> F[下一单词 / 今日完成]
    F --> G[AI 生成今日总结短文]
    G --> H[全文翻译 AI 批改]
    H -- 正确率 >= 60% --> I[学习模块完成]
    H -- 低于 60% --> G
```

复习模块：到期单词按紧急度排序 → AI 生成复习短文（词频严格按遗忘曲线）→ 全文翻译批改 → 通过后推进所有单词的复习阶段。

## 文档索引

- [框架图与模块职责说明](docs/ARCHITECTURE.md)
- [接口文档](docs/API.md)
- [开发规范引用](docs/SPECS.md)
- [Docker 打包部署指南](docs/DEPLOY.md)

## Docker 部署

项目提供镜像构建与编排文件（后端 Java17 / 前端 Nginx / MySQL8），一条命令起全套：

```bash
copy .env.example .env      # 按需修改密钥与口令
docker compose up -d --build
docker compose --profile tools run --rm import   # 导入真实词库（一次性）
```

详细步骤、环境变量与数据库脚本说明见 [docs/DEPLOY.md](docs/DEPLOY.md)。

## 移植性设计（小程序 / Android）

- 前端将**页面组件**与**业务逻辑**分离：`api/`（网络层）、`stores/`（状态）、`types/`（模型）都不依赖 Element Plus，移植到 uni-app / Android WebView 时可整体复用；
- 后端为纯 REST + JWT，天然支持多端；
- 移植 uni-app 时只需重写 `views/` 与 `components/`（换成 uni 组件），`api/` 的请求封装替换为 uni.request 即可。

## 环境说明

- 本机已具备：Java 17、Maven 3.9.9、Node 22、MySQL 8.0、Git；
- 新增依赖均来自 Maven Central / npm registry（首次构建会自动下载）；
- 未使用 Redis / 消息队列等额外组件，当前单机架构即可运行。
