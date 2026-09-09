-- =====================================================================
-- WordFlow 词书模块增量迁移脚本
-- 适用：已在旧版 schema 上运行的数据库（保留已有学习数据）
-- 执行方式：mysql -uroot -proot --default-character-set=utf8mb4 < migrate_book.sql
-- =====================================================================

SET NAMES utf8mb4;
USE wordflow;

-- ---------------------------------------------------------------------
-- 1. 词书表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learn_book (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '词书名称',
    code        VARCHAR(32)  NOT NULL COMMENT '词书编码（唯一标识，如 CET4）',
    level       VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '级别标签：初中/高中/四级/六级/考研/托福/雅思/GRE',
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
-- 2. learn_word 增加 book_id，并调整唯一约束为 (book_id, word)
--    旧数据默认归入 book_id=1（四级），用户已有进度/计划不受影响。
-- ---------------------------------------------------------------------
ALTER TABLE learn_word
    ADD COLUMN book_id BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '词书ID' AFTER id;

ALTER TABLE learn_word
    DROP INDEX uk_word,
    ADD UNIQUE KEY uk_book_word (book_id, word),
    ADD KEY idx_book (book_id);

-- ---------------------------------------------------------------------
-- 3. sys_user 增加当前词书
-- ---------------------------------------------------------------------
ALTER TABLE sys_user
    ADD COLUMN active_book_id BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '用户当前选择的词书ID' AFTER daily_word_goal;

-- ---------------------------------------------------------------------
-- 4. learn_plan 记录当日计划来自哪本词书
-- ---------------------------------------------------------------------
ALTER TABLE learn_plan
    ADD COLUMN book_id BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '词书ID' AFTER user_id;

-- ---------------------------------------------------------------------
-- 5. 初始化词书元数据（真实词数由导入脚本导入后回写）
--    book_id=1 固定为四级，保证旧数据默认归属。
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
