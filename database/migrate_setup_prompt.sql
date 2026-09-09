-- =====================================================================
-- WordFlow 设置弹窗处理状态迁移脚本
-- 用途：把「是否已处理过每日目标设置弹窗」记录到服务端，
--       不再依赖浏览器 localStorage，避免每次打开都重复弹出。
-- =====================================================================

SET NAMES utf8mb4;
USE wordflow;

ALTER TABLE sys_user
    ADD COLUMN setup_prompt_at DATETIME NULL
        COMMENT '最近一次设置弹窗处理时间（保存或暂不设置均记录）' AFTER last_login_at;
