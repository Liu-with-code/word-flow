# 开发规范与质量卡口

本项目遵循《阿里巴巴Java开发手册（嵩山版）》与《阿里巴巴前端规约 F2E-Spec》，
并按规范落地了可执行的检查命令。

参考来源：

- 《阿里巴巴Java开发手册（嵩山版）》：https://github.com/alibaba/p3c
- 《阿里巴巴前端规约 F2E-Spec》：https://github.com/alibaba/f2e-spec
- Vue 3 官方风格指南：https://vuejs.org/style-guide/

## 1. 后端卡口（p3c-pmd）

| 项 | 说明 |
| --- | --- |
| 规则来源 | `com.alibaba.p3c:p3c-pmd:2.1.1`（官方 PMD 实现） |
| 规则集 | `backend/p3c-ruleset.xml`，引用 10 个 `ali-*.xml` |
| 裁剪 | 排除 `ClassMustHaveAuthorRule`（作者信息以 Git 记录为准）、`ServiceOrDaoClassShouldEndWithImplRule`（AI 服务为策略模式条件装配，类名体现 provider 更利于排查） |
| 插件 | `maven-pmd-plugin:3.21.2`（内置 PMD 6.55.0） |
| 卡口 | 绑定 `verify` 阶段，`failOnViolation=true`；Dockerfile 只执行 `package`，不受影响 |
| 命令 | `mvn verify`（含卡口）或 `mvn pmd:check`（仅检查） |

## 2. 前端卡口（ESLint + Prettier）

| 项 | 说明 |
| --- | --- |
| 规则包 | `eslint-config-ali@16`（flat config）、`typescript-eslint@8`、`eslint-plugin-vue@10` |
| 格式化 | `prettier-config-ali`（printWidth 100、单引号、分号、trailingComma all、LF） |
| 配置 | `frontend/eslint.config.mjs`、`frontend/.prettierignore`、`package.json` 的 `prettier` 字段 |
| 命令 | `npm run lint` / `npm run format:check` / `npm run format` / `npm run check` |

格式化统一由 Prettier 负责，ESLint 只保留语义与最佳实践规则；
模板格式类规则在配置中关闭，避免两边互相覆盖。

## 3. 关键实现约定

**并发与缓存**

- 线程池不使用 `Executors` 快速创建：SSE 流式批改使用 `AsyncConfig#aiTaskExecutor`
  （显式线程数与有界队列、自定义线程名前缀、`CallerRunsPolicy` 背压），不占用公共 ForkJoinPool；
- 练习句缓存为同步访问的 LRU 有界缓存（容量 512），避免长时间运行内存持续增长。

**入参校验**

- 写接口的请求体在 Controller 层用 `@Valid` 触发校验，约束注解集中在 DTO 上；
- 校验失败由 `GlobalExceptionHandler` 统一转换为 `code=400` 的 `Result` 响应。

**时间口径**

- 所有复习到期时间按「用户学习日 + 间隔天数」锚定到用户时区的日边界，
  不依赖服务器时区，详见 `ProgressService`。

## 4. 记忆曲线依据

- 科普中国《艾宾浩斯遗忘曲线了解一下》给出的复习周期：5 分钟、30 分钟、12 小时、1 天、2 天、4 天、7 天、15 天；
- 本项目按日粒度实现 1 / 2 / 4 / 7 / 15 / 30 天间隔，
  短周期（5 分钟 / 30 分钟 / 12 小时）可在 `ProgressService` 中扩展为小时级调度；
- 夜猫子处理：① 设置页配置日边界小时（如 4:00，凌晨 0-4 点背的词算前一天）；
  ② 保持 0 点默认时，系统检测凌晨背词（`night_cutoff_hour`，默认 6），
  白天登录弹窗询问是否提前加入今日复习，每天最多询问一次。

## 5. 词书数据来源

- 原始数据来自 [KyleBing/english-vocabulary](https://github.com/KyleBing/english-vocabulary)（MIT 协议）；
- 8 本词书位于 `database/raw/`，由 `BookDataImporter` 解析为 `learn_word` 标准字段，
  同一单词多次出现时合并全部释义与词性；
- 仅供学习研究使用，商用请替换为有授权词库。

## 6. 视觉与交互参考

- Element Plus 设计体系：https://element-plus.org
- Material Design 3：https://m3.material.io （间距 4/8/12/16/24、卡片层级、圆角与阴影）
