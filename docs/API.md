# WordFlow 接口文档

## 通用约定

- Base URL：`http://localhost:8080/api`
- 认证：除登录/注册外，均需请求头 `Authorization: Bearer <token>`
- 统一响应：

```json
{ "code": 200, "message": "成功", "data": { } }
```

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 / 登录过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 冲突（如用户名已占用） |
| 500 | 服务器内部错误 |

- Swagger UI：`/swagger-ui.html`（含在线调试）

## 认证 Auth

### POST /auth/register

注册并直接返回令牌。

```json
// 请求
{ "username": "alice", "password": "123456", "nickname": "爱丽丝" }
// 响应 data
{ "token": "eyJ...", "user": { "id": 1, "username": "alice", "nickname": "爱丽丝", "dailyWordGoal": 20 } }
```

### POST /auth/login

```json
{ "username": "alice", "password": "123456" }
```

响应结构与注册一致。

## 用户 User

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /user/me | 当前用户信息 |
| PUT | /user/profile | 更新昵称/头像/每日目标/复习目标/时区/复习日边界，`{ "nickname": "…", "avatarUrl": "…", "dailyWordGoal": 20, "dailyReviewGoal": 20, "timezone": "Asia/Shanghai", "dayBoundaryHour": 4 }` |

## 词书 Books

### GET /books

词书列表，包含当前选中标记与**每本书的学习进度**（进度按「账号 + 单词」维度统计，
与当前选中词书无关，因此切换词书不会清空任何一本书的进度）。

```json
// 响应 data 示例
[
  {
    "id": 1,
    "name": "四级核心词汇",
    "code": "CET4",
    "level": "四级",
    "description": "大学英语四级核心词汇…",
    "coverColor": "#6366f1",
    "wordCount": 4544,
    "sortNo": 10,
    "active": true,
    "learnedCount": 1136,
    "masteredCount": 0,
    "remainingCount": 3408,
    "progressPercent": 25
  }
]
```

| 字段 | 含义 |
| --- | --- |
| `learnedCount` | 已开始学习的单词数（进入过四步练习） |
| `masteredCount` | 已掌握（通过全部艾宾浩斯复习轮次，status=COMPLETE）的单词数 |
| `remainingCount` | 尚未学习的单词数（`wordCount - learnedCount`，不小于 0） |
| `progressPercent` | 学习进度百分比 0-100（`learnedCount / wordCount` 四舍五入） |

### POST /books/{id}/select

将 `{id}` 词书设为当前词书（写入 `sys_user.active_book_id`）。
只切换「当前使用」的词书，不影响任何一本书的进度。

### POST /books/{id}/restart

重新背诵该词书：清空当前账号在本书的学习进度，使其重新可学。

业务规则：

- 仅清除该词书的 `learn_progress` 进度行，以及今日「进行中」计划中属于该词书的明细；
- 若清理后今日计划已无任何单词，则一并删除该计划，下次「开始今日学习」按新进度重新抽词；
- **其他词书的进度不受影响**；已完成的计划视为历史记录保留；
- 出于数据安全，作答流水（`learn_record`）与已生成短文（`learn_article`）保留，仅重置进度；
- 返回重置后的完整词书列表（结构同 `GET /books`），前端可直接刷新进度条。

典型用途：一本词书学完后，点击「重新背诵」重新学习该书，避免「背完一次无法再背」。

## 词库 Words

### GET /words

分页查询：

| 参数 | 说明 |
| --- | --- |
| keyword | 单词或中文模糊搜索（可选） |
| bookId | 词书 ID，按词书筛选（可选） |
| level | CET4 / CET6 / 考研（可选） |
| page / size | 分页，默认 1 / 10 |

### GET /words/{id}

单词详情。

## 每日学习 Learning

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /learning/today | 今日计划与当前单词 |
| POST | /learning/start | 开始/继续今日学习（幂等，从当前词书抽词） |
| POST | /learning/reconcile | 协调今日计划：检测词书/每日目标变更并同步更新 |
| POST | /learning/add-words | 追加单词到今日计划（错题本「加入今日学习」），请求体 `{ "wordIds": [1, 2] }` |
| GET | /learning/practice/{wordId} | 获取 AI 练习句（包含已学单词与薄弱单词） |
| POST | /learning/check-zh | 看英文选中文释义 |
| POST | /learning/check-en | 看中文选英文单词 |
| POST | /learning/translate-en | 英译中（AI 批改） |
| POST | /learning/translate-zh | 中译英（AI 批改） |
| POST | /learning/translate-stream | 流式批改中英互译（SSE，实时返回批改增量） |
| POST | /learning/translate-hint | 翻译不会做时获取参考译文 |
| POST | /learning/complete-word | 标记单词完成 |
| POST | /learning/finish-day | 今日全部完成，生成总结短文 |
| POST | /learning/article/check | 批改总结短文翻译 |

`GET /learning/today` 与 `POST /learning/start` 的响应 `data` 额外包含：

```json
{
  "bookId": 1,
  "bookName": "四级核心词汇",
  "dailyWordGoal": 20
}
```

`/learning/add-words` 业务规则：

- 无今日计划时返回 `code=409`，提示先开始今日学习；
- 已完成的单词去重；计划中已存在的单词不重复添加；
- 追加后总数不超过 `daily_word_goal`，超出部分自动截断；
- 已完成（COMPLETED）的计划被追加单词后会重新置为 IN_PROGRESS。

### 流式批改 /translate-stream

请求体：

```json
{ "wordId": 1, "sentence": "Never abandon your dream…", "userTranslation": "永远不要放弃梦想…", "direction": "en2zh" }
```

`direction` 取值 `en2zh` / `zh2en`。响应为 SSE 流，每行 `data:` 均为 JSON：

```text
data: {"type":"chunk","payload":"你的翻译基本达意，但语序可以更自然。"}
data: {"type":"done","payload":{ "correct": false, "message": "…", "nextStep": "TRANS_EN", "score": 60, "comment": "…", "standard": "…", "finished": false }}
```

### 参考译文 /translate-hint

```json
// 请求
{ "wordId": 1, "direction": "en2zh" }
// 响应 data
{ "hint": "标准译文（大模型生成）", "sentence": "原始句子" }
```

### 逐步作答

```json
// check-zh
{ "wordId": 1, "selectedChinese": "放弃；抛弃" }
// check-en
{ "wordId": 1, "selectedWord": "abandon" }
// translate-en / translate-zh
{ "wordId": 1, "sentence": "Never abandon your dream…", "userTranslation": "永远不要放弃梦想…" }
```

单步响应 `data`：

```json
{
  "correct": true,
  "message": "释义正确！",
  "nextStep": "CHOOSE_EN",
  "score": null,
  "comment": null,
  "standard": null,
  "finished": false
}
```

`nextStep` 取值：`PREVIEW / CHOOSE_EN / CHOOSE_ZH / TRANS_EN / TRANS_ZH / COMPLETE_WORD / FINISH_DAY / NEXT_WORD`。

### 短文批改

```json
// 请求
{ "articleId": 1, "userTranslation": "今天我学会了……" }
// 响应 data
{
  "articleId": 1,
  "passed": true,
  "score": 85,
  "comment": "翻译基本符合语义",
  "errors": [
    { "segment": "…", "expected": "…", "user": "…", "suggestion": "…" }
  ],
  "standardZh": "标准译文"
}
```

`passed = score >= 60`；未通过时前端可“重新翻译”或“换一篇重练”。

## 复习 Review

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /review/overview | 待复习数量 |
| GET | /review/warmup | 复习热身：到期单词快速自测卡片（按紧急度排序，最多 20 个） |
| POST | /review/warmup | 热身作答：`{ "wordId": 1, "remembered": true }`，记得则推进复习阶段 |
| POST | /review/start | 按遗忘曲线生成复习短文 |
| POST | /review/check | 批改复习短文 |
| GET | /review/night-prompt | 夜间学习智能弹窗：`{ "show": true, "date": "2026-08-23", "count": 3 }` |
| POST | /review/night-prompt | 答复弹窗：`{ "apply": true }`（true=提前加入今日复习，false=保持默认） |

`GET /review/warmup` 响应 `data`：

```json
{
  "dueCount": 12,
  "total": 12,
  "markedCount": 0,
  "words": [
    {
      "word": { "id": 1, "word": "abandon", "phonetic": "/əˈbændən/", "chinese": "放弃；抛弃" },
      "stage": 2,
      "lastReviewAt": "2026-09-20T00:00",
      "priority": 4,
      "marked": false
    }
  ]
}
```

`POST /review/warmup` 响应 `data`：`{ "remembered": true, "stage": 3, "status": "REVIEWING", "remaining": 11 }`；
作答流水会写入 `learn_record`（`session_type=REVIEW`、`step_type=REVIEW_WARMUP`）。

`/review/start` 每次最多生成的文章词数受 `sys_user.daily_review_goal`（每日复习目标）限制。

`start` 响应 `data`：

```json
{
  "hasWords": true,
  "articleId": 3,
  "title": "REVIEW 复习短文",
  "contentEn": "…",
  "words": [ "单词列表" ],
  "frequency": [ { "word": "abandon", "priority": 4 } ]
}
```

`frequency.priority` 表示该单词在文章中的出现次数（1-4），由逾期天数与复习阶段计算。

## 统计 Statistics

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /stats/dashboard | 已学/掌握/待复习/今日进度/连续天数/正确率 |
| GET | /stats/weekly | 近 7 天作答量 |
| GET | /stats/recent?limit=10 | 最近作答记录 |
| GET | /stats/stage-distribution | 记忆阶段分布（艾宾浩斯各阶段单词数量） |
| GET | /stats/review-forecast?days=14 | 未来复习量预测（按用户学习日统计每日到期数，days 上限 60） |
| GET | /stats/weak-words?onlyWrong=true&limit=6 | 薄弱单词概览（答错次数最多） |
| GET | /stats/weak-words/page?page=1&size=10 | 错题本分页查询（size 上限 50） |

`GET /stats/stage-distribution` 响应 `data`：

```json
{
  "total": 72,
  "complete": 3,
  "reviewing": 40,
  "mastered": 25,
  "learning": 4,
  "buckets": [
    { "label": "学习中", "stage": 0, "count": 4, "description": "尚未完成今日四步练习" },
    { "label": "第 1 轮", "stage": 1, "count": 25, "description": "1 天后复习" },
    { "label": "长期记忆", "stage": 7, "count": 3, "description": "已通过全部复习" }
  ]
}
```

`GET /stats/review-forecast` 响应 `data`：

```json
{
  "days": 14,
  "overdueCount": 5,
  "totalPlanned": 37,
  "points": [ { "date": "2026-09-21", "count": 4 } ]
}
```

- `overdueCount`：已逾期（下次复习日早于今日）的单词数，单独统计，不重复计入 `points`；
- `totalPlanned`：窗口内将到期的单词总数；统计口径为用户时区的自然日。

`GET /stats/weak-words/page` 响应 `data`（分页结构）：

```json
{
  "list": [
    {
      "word": { "id": 1, "word": "abandon", "phonetic": "/əˈbændən/", "chinese": "放弃；抛弃" },
      "wrongCount": 3,
      "correctCount": 1,
      "accuracy": 25,
      "stage": 1,
      "status": "MASTERED",
      "lastLearnedAt": "2026-09-20 10:00:00"
    }
  ],
  "total": 18,
  "page": 1,
  "size": 10
}
```
