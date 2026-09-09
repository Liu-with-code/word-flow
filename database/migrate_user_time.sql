-- =====================================================================
-- WordFlow 用户时区与复习日边界迁移脚本
-- 说明：时间标准不再写死为服务器时间；每个用户记录自己的时区，
--       复习到期时间按用户时区的「日边界」计算。
-- =====================================================================

SET NAMES utf8mb4;
USE wordflow;

ALTER TABLE sys_user
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai'
        COMMENT '用户时区（IETF BCP 47，如 Asia/Shanghai）' AFTER active_book_id,
    ADD COLUMN day_boundary_hour TINYINT NOT NULL DEFAULT 0
        COMMENT '复习日边界小时 0-23：凌晨该点前背的词算前一天（0=不启用）' AFTER timezone,
    ADD COLUMN night_cutoff_hour TINYINT NOT NULL DEFAULT 6
        COMMENT '夜间学习判定截止小时 0-12（凌晨 0 点到该点之间背的词触发智能弹窗）' AFTER day_boundary_hour,
    ADD COLUMN night_prompt_date DATE NULL
        COMMENT '最近一次夜间学习智能提示日期（避免重复弹窗）' AFTER night_cutoff_hour;
