-- =====================================================================
-- WordFlow 复习到期时间修正脚本
-- 背景：旧逻辑按「学习时刻 + 24h」安排复习，导致昨天背的词今天凌晨不出现。
-- 本脚本把所有已进入复习队列的单词，按「学习日 + 间隔天数」重算到期时间，
-- 并锚定到日边界（默认 00:00:00，与 application.yml 的 progress.day-boundary-hour 一致）。
-- =====================================================================

SET NAMES utf8mb4;
USE wordflow;

UPDATE learn_progress
SET next_review_at = TIMESTAMP(
        DATE_ADD(DATE(last_learned_at), INTERVAL
            CASE stage
                WHEN 1 THEN 1
                WHEN 2 THEN 2
                WHEN 3 THEN 4
                WHEN 4 THEN 7
                WHEN 5 THEN 15
                WHEN 6 THEN 30
                ELSE 1
            END DAY),
        TIME '00:00:00')
WHERE status IN ('MASTERED', 'REVIEWING')
  AND next_review_at IS NOT NULL
  AND last_learned_at IS NOT NULL;

-- 若日后调整了 day-boundary-hour，请把上面 TIME '00:00:00' 改成对应小时重新执行。
