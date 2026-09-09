-- =====================================================================
-- WordFlow 用户目标设置迁移脚本
-- 新增：每日复习目标、最近登录时间（用于首次/回归欢迎弹窗）。
-- =====================================================================

SET NAMES utf8mb4;
USE wordflow;

ALTER TABLE sys_user
    ADD COLUMN daily_review_goal INT NOT NULL DEFAULT 20
        COMMENT '每日复习目标数' AFTER daily_word_goal,
    ADD COLUMN last_login_at DATETIME NULL
        COMMENT '最近登录时间（用于欢迎弹窗与长期未登录判定）' AFTER night_prompt_date;
