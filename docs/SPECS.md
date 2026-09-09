# 开发规范引用与落地说明

本项目严格遵循以下公开规范，引用来源均已核实：

## 1. 《阿里巴巴Java开发手册（嵩山版）》

来源：https://github.com/alibaba/p3c （阿里云开发者社区发布，含 PDF 下载）

后端落地要点：

- 分层规约：Controller 薄、Service 业务、Mapper 持久化；
- 命名规约：类名 UpperCamelCase、方法 lowerCamelCase、常量全大写、表名字段小写下划线；
- 表名规约：`业务模块名_表名`，如 `learn_progress`、`sys_user`；主键 `id`、必备 `created_at/updated_at`；
- 异常规约：业务异常显式抛出，全局处理器统一捕获，日志记录完整堆栈；
- 数据库规约：InnoDB + utf8mb4、唯一索引、常用查询字段建索引；
- POJO 规约：DO 与 VO 分离，出参不暴露密码哈希等敏感字段；
- 安全规约：密码 BCrypt 哈希存储，JWT 密钥不写死业务代码，AI API Key 通过环境变量注入。

## 2. 《阿里巴巴前端规约》F2E-Spec

来源：https://github.com/yanyue404/f2e-spec （阿里内部前端规约的开源版）

前端落地要点：

- 目录按业务域划分（api / stores / views / components）；
- TypeScript 严格模式，杜绝 `any` 滥用；
- 命名：组件 PascalCase、组合函数 `useXxx`、常量大写；
- 工程化：Vite + TS + ESLint 思路；后续可用 `npx f2elint` 一键接入完整 Lint 卡口（本项目未强行引入，避免依赖膨胀）。

## 3. Vue 3 官方风格指南

来源：https://vuejs.org/style-guide/

- SFC 组件名多单词 PascalCase（如 `WordCard.vue`）；
- 单文件组件内 `template/script/style` 顺序；
- 通过 `defineProps/defineEmits` 显式声明，避免隐式传参；
- 路由组件懒加载。

## 4. 企业级 Vue3 项目目录实践

参考：掘金《Vue3 项目目录结构规范：按业务域划分》与 51CTO《大型 Vue3 项目目录架构》

- 通用组件与业务组件分离；
- 网络层独立封装（`api/request.ts`），方便换端复用；
- Pinia 状态独立于组件。

## 5. 视觉与交互

- Element Plus 设计体系：https://element-plus.org
- Material Design 3：https://m3.material.io （间距 4/8/12/16/24、卡片层级、圆角与阴影）

## 6. 记忆曲线依据

- 科普中国《艾宾浩斯遗忘曲线了解一下》：5 分钟、30 分钟、12 小时、1 天、2 天、4 天、7 天、15 天分散复习；
- 本项目按日粒度实现：1 / 2 / 4 / 7 / 15 / 30 天间隔，短周期（5 分钟 / 30 分钟 / 12 小时）可在 `ProgressService` 中扩展为小时级调度。
- 到期时间按「用户学习日 + 间隔天数」锚定到**用户时区**的日边界
  （`sys_user.timezone` + `day_boundary_hour`，默认 0:00）：前一天背的单词在次日 0:00 进入复习队列，
  与具体学习时刻无关；
- 夜猫子两种处理：① 设置页配置日边界小时（如 4:00，凌晨 0-4 点背的词算前一天）；
  ② 保持 0 点默认时，系统自动检测凌晨背词（`night_cutoff_hour`，默认 6），
  白天登录弹出智能询问，确认后提前加入今日复习，且每天最多询问一次。

## 7. 词书数据来源

- 词书原始数据来自 [KyleBing/english-vocabulary](https://github.com/KyleBing/english-vocabulary)
  （MIT 协议），文件位于 `full_line_jsonl/sentence/正序/*.jsonl`；
- 已下载 8 本词书到 `database/raw/`，由 `BookDataImporter` 解析为 `learn_word`
  标准字段（单词/音标/释义/词性/例句/难度），同一单词在文件内多次出现时合并全部释义与词性；
- 仅供学习研究使用，若商用请自行替换为有授权词库。

## 未使用但可选的工具

- `f2elint`（阿里前端规约一键接入）：建议后续执行 `npx f2elint` 接入 commit 卡口；
- Redis：单机阶段无需；多端并发后可缓存 AI 短文与热点词库。
