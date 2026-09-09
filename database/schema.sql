-- =====================================================================
-- WordFlow「词流」数据库初始化脚本
-- 适用数据库：MySQL 8.0+
-- 设计规范：遵循《阿里巴巴Java开发手册》MySQL 规约
--   1. 表名：业务模块前缀_表名，全部小写，下划线分词
--   2. 字段：小写下划线命名；主键统一为 id
--   3. 必备字段：created_at / updated_at
--   4. 字符集 utf8mb4，InnoDB 引擎
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS wordflow
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE wordflow;

-- ---------------------------------------------------------------------
-- 1. 用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username        VARCHAR(50)  NOT NULL COMMENT '登录名',
    password_hash   VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    nickname        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    avatar_url      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像地址',
    daily_word_goal INT          NOT NULL DEFAULT 20 COMMENT '每日新词目标数',
    daily_review_goal INT        NOT NULL DEFAULT 20 COMMENT '每日复习目标数',
    active_book_id  BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '用户当前选择的词书ID',
    timezone        VARCHAR(64)  NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '用户时区（IETF BCP 47）',
    day_boundary_hour TINYINT    NOT NULL DEFAULT 0 COMMENT '复习日边界小时 0-23（0=不启用）',
    night_cutoff_hour TINYINT    NOT NULL DEFAULT 6 COMMENT '夜间学习判定截止小时 0-12',
    night_prompt_date DATE        NULL COMMENT '最近一次夜间学习智能提示日期',
    last_login_at   DATETIME      NULL COMMENT '最近登录时间',
    setup_prompt_at DATETIME      NULL COMMENT '最近一次设置弹窗处理时间',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '用户表';

-- ---------------------------------------------------------------------
-- 2. 词书表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_book (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '词书名称',
    code        VARCHAR(32)  NOT NULL COMMENT '词书编码（唯一标识，如 CET4）',
    level       VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '级别标签',
    description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '词书简介',
    cover_color VARCHAR(16)  NOT NULL DEFAULT '#6366f1' COMMENT '前端封面主色',
    word_count  INT          NOT NULL DEFAULT 0 COMMENT '词书单词总数（导入后更新）',
    sort_no     INT          NOT NULL DEFAULT 0 COMMENT '展示排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    source      VARCHAR(128) NOT NULL DEFAULT '' COMMENT '数据来源',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = '词书表';

-- ---------------------------------------------------------------------
-- 3. 单词表（基础词库）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_word (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    book_id         BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '词书ID',
    word            VARCHAR(64)  NOT NULL COMMENT '英文单词',
    phonetic        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '音标',
    chinese         VARCHAR(255) NOT NULL COMMENT '中文释义',
    part_of_speech  VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '词性',
    example_en      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '英文例句',
    example_zh      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '例句中文翻译',
    difficulty      TINYINT      NOT NULL DEFAULT 3 COMMENT '难度 1-5',
    level           VARCHAR(16)  NOT NULL DEFAULT 'CET4' COMMENT '词库级别：CET4/CET6/考研',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_book_word (book_id, word),
    KEY idx_book (book_id)
) ENGINE = InnoDB COMMENT = '单词表';

-- ---------------------------------------------------------------------
-- 4. 用户单词进度表
--    核心表：记录每个用户对每个单词的学习状态与艾宾浩斯复习时间
--    stage 语义：0=刚学完待首次复习；1..6=对应 1/2/4/7/15/30 天复习阶段；
--                stage 达到 6 且通过复习后 status 变为 COMPLETE（长期记忆）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_progress (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    word_id         BIGINT UNSIGNED NOT NULL COMMENT '单词ID',
    status          VARCHAR(16)     NOT NULL DEFAULT 'LEARNING'
                    COMMENT '状态：LEARNING-学习中 MASTERED-已学会(待复习) REVIEWING-复习中 COMPLETE-长期掌握',
    stage           TINYINT         NOT NULL DEFAULT 0 COMMENT '艾宾浩斯复习阶段 0-6',
    learn_count     INT             NOT NULL DEFAULT 0 COMMENT '学习/复习总次数',
    correct_count   INT             NOT NULL DEFAULT 0 COMMENT '答对次数',
    wrong_count     INT             NOT NULL DEFAULT 0 COMMENT '答错次数',
    last_learned_at DATETIME        NULL COMMENT '最近学习时间',
    next_review_at  DATETIME        NULL COMMENT '下次复习时间（艾宾浩斯曲线）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_word (user_id, word_id),
    KEY idx_next_review (user_id, status, next_review_at)
) ENGINE = InnoDB COMMENT = '用户单词进度表';

-- ---------------------------------------------------------------------
-- 5. 每日学习计划表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_plan (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    book_id          BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '词书ID',
    plan_date        DATE            NOT NULL COMMENT '计划日期',
    new_word_count   INT             NOT NULL DEFAULT 0 COMMENT '计划新词数',
    completed_count  INT             NOT NULL DEFAULT 0 COMMENT '已完成单词数',
    status           VARCHAR(16)     NOT NULL DEFAULT 'IN_PROGRESS'
                     COMMENT '状态：IN_PROGRESS-进行中 COMPLETED-已完成',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, plan_date)
) ENGINE = InnoDB COMMENT = '每日学习计划表';

-- ---------------------------------------------------------------------
-- 6. 计划单词明细表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_plan_word (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    plan_id      BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    word_id      BIGINT UNSIGNED NOT NULL COMMENT '单词ID',
    order_no     INT             NOT NULL DEFAULT 0 COMMENT '学习顺序',
    status       VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                 COMMENT '状态：PENDING-待学习 COMPLETED-已完成',
    completed_at DATETIME        NULL COMMENT '完成时间',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_plan_word (plan_id, word_id),
    KEY idx_plan (plan_id)
) ENGINE = InnoDB COMMENT = '计划单词明细表';

-- ---------------------------------------------------------------------
-- 7. 学习记录表（每一步作答的流水，用于统计与纠错分析）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_record (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    word_id       BIGINT UNSIGNED NOT NULL COMMENT '单词ID',
    session_type  VARCHAR(16)     NOT NULL COMMENT '场景：DAILY_LEARN-每日学习 REVIEW-复习 ARTICLE-短文翻译',
    step_type     VARCHAR(20)     NOT NULL COMMENT '步骤：CHOOSE_ZH/CHOOSE_EN/TRANS_EN/TRANS_ZH/ARTICLE',
    is_correct    TINYINT         NOT NULL DEFAULT 0 COMMENT '是否答对：1-对 0-错',
    user_answer   TEXT            NULL COMMENT '用户作答',
    ai_feedback   TEXT            NULL COMMENT 'AI 反馈',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_time (user_id, created_at)
) ENGINE = InnoDB COMMENT = '学习记录流水表';

-- ---------------------------------------------------------------------
-- 8. 短文表（AI 生成的今日总结/复习文章）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_article (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    type        VARCHAR(20)     NOT NULL COMMENT '类型：DAILY_SUMMARY-今日总结 REVIEW-复习文章',
    title       VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '标题',
    content_en  MEDIUMTEXT      NOT NULL COMMENT '英文正文',
    content_zh  MEDIUMTEXT      NOT NULL COMMENT '标准中文翻译',
    words_json  VARCHAR(2000)   NOT NULL DEFAULT '[]' COMMENT '包含的单词列表(JSON)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB COMMENT = 'AI 生成短文表';

-- ---------------------------------------------------------------------
-- 9. 短文翻译尝试表（记录每次批改得分与错误明细）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_article_attempt (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    article_id       BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    user_translation MEDIUMTEXT       NOT NULL COMMENT '用户译文',
    score            DECIMAL(5, 2)    NOT NULL DEFAULT 0 COMMENT 'AI 批改得分 0-100',
    errors_json      MEDIUMTEXT       NULL COMMENT '错误明细(JSON)',
    is_pass          TINYINT          NOT NULL DEFAULT 0 COMMENT '是否通过(>=60)：1-通过 0-未通过',
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_article (article_id)
) ENGINE = InnoDB COMMENT = '短文翻译尝试表';

-- ---------------------------------------------------------------------
-- 10. AI 调用日志表（便于调试 Prompt 与排查费用）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_ai_log (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id       BIGINT UNSIGNED NULL COMMENT '用户ID（可为空）',
    scene         VARCHAR(32)     NOT NULL COMMENT '场景：JUDGE_TRANSLATION/GENERATE_SENTENCE/GENERATE_ARTICLE',
    request_json  MEDIUMTEXT      NOT NULL COMMENT '请求内容',
    response_json MEDIUMTEXT      NULL COMMENT '响应内容',
    model         VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '模型名称',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_scene_time (scene, created_at)
) ENGINE = InnoDB COMMENT = 'AI 调用日志表';

-- ---------------------------------------------------------------------
-- 11. 初始化词书元数据（词数由导入脚本回写）
-- ---------------------------------------------------------------------
INSERT INTO learn_book (id, name, code, level, description, cover_color, word_count, sort_no, status, source)
VALUES
    (1, '四级核心词汇', 'CET4', '四级', '大学英语四级核心词汇，覆盖高频考点与常用例句。', '#6366f1', 0, 10, 1, 'KyleBing/english-vocabulary'),
    (2, '六级核心词汇', 'CET6', '六级', '大学英语六级核心词汇，难度较四级进一步提升。', '#8b5cf6', 0, 20, 1, 'KyleBing/english-vocabulary'),
    (3, '考研词汇', 'KY', '考研', '考研英语大纲词汇，含历年真题高频词。', '#0ea5e9', 0, 30, 1, 'KyleBing/english-vocabulary'),
    (4, '托福词汇', 'TOEFL', '托福', '托福考试核心词汇，覆盖学术场景常用表达。', '#f59e0b', 0, 40, 1, 'KyleBing/english-vocabulary'),
    (5, '雅思词汇', 'IELTS', '雅思', '雅思考试核心词汇，覆盖听说读写常见话题词。', '#10b981', 0, 50, 1, 'KyleBing/english-vocabulary'),
    (6, 'GRE 词汇', 'GRE', 'GRE', 'GRE 考试高阶词汇，适合冲刺北美研究生入学考试。', '#ef4444', 0, 60, 1, 'KyleBing/english-vocabulary'),
    (7, '高中英语词汇', 'SENIOR', '高中', '高中阶段英语词汇，同步课标常用词。', '#ec4899', 0, 70, 1, 'KyleBing/english-vocabulary'),
    (8, '初中英语词汇', 'JUNIOR', '初中', '初中阶段英语基础词汇，适合打牢根基。', '#22c55e', 0, 80, 1, 'KyleBing/english-vocabulary')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    level = VALUES(level),
    description = VALUES(description),
    cover_color = VALUES(cover_color),
    source = VALUES(source);
