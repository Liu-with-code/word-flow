package com.wordflow.module.statistics.dto;

import java.time.LocalDateTime;

/**
 * 统计模块数据传输对象。
 */
public final class StatisticsDtos {

    private StatisticsDtos() {
    }

    /** 首页仪表盘 */
    public record DashboardStatsResponse(
            long totalLearned,
            long masteredCount,
            long dueCount,
            int todayTotal,
            int todayCompleted,
            int streakDays,
            double accuracy
    ) {
    }

    /** 近 7 天学习量 */
    public record WeeklyPoint(String date, long count) {
    }

    /** 最近作答 */
    public record RecentRecordVO(String word, String stepType, boolean correct,
                                 LocalDateTime createdAt) {
    }
}

