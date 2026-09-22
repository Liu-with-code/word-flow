package com.wordflow.module.statistics.dto;

import com.wordflow.module.word.dto.WordVO;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 记忆阶段分布（可视化用） */
    public record StageDistributionResponse(
            long total,
            long complete,
            long reviewing,
            long mastered,
            long learning,
            List<StageBucket> buckets
    ) {
    }

    /** 单个艾宾浩斯阶段桶 */
    public record StageBucket(String label, int stage, long count, String description) {
    }

    /** 未来复习量预测的单日数据点 */
    public record ReviewForecastPoint(String date, long count) {
    }

    /** 未来复习量预测 */
    public record ReviewForecastResponse(
            int days,
            long overdueCount,
            long totalPlanned,
            List<ReviewForecastPoint> points
    ) {
    }

    /** 薄弱单词（错题）概览 */
    public record WeakWordVO(
            WordVO word,
            int wrongCount,
            int correctCount,
            int accuracy,
            int stage,
            String status,
            LocalDateTime lastLearnedAt
    ) {
    }
}
