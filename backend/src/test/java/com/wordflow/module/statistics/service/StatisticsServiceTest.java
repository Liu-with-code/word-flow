package com.wordflow.module.statistics.service;

import com.wordflow.module.learning.entity.LearningPlan;
import com.wordflow.module.learning.entity.LearningRecord;
import com.wordflow.module.learning.mapper.LearningPlanMapper;
import com.wordflow.module.learning.mapper.LearningRecordMapper;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.mapper.WordProgressMapper;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.statistics.dto.StatisticsDtos.DashboardStatsResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.RecentRecordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.ReviewForecastResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.StageBucket;
import com.wordflow.module.statistics.dto.StatisticsDtos.StageDistributionResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeakWordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeeklyPoint;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.dto.WordVO;
import com.wordflow.module.word.mapper.WordMapper;
import com.wordflow.module.word.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 统计服务单元测试。
 *
 * 覆盖：仪表盘聚合、近 7 天柱状数据、最近作答与连续学习天数，
 *       以及记忆阶段分布、未来复习量预测、薄弱单词查询。
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private ProgressService progressService;

    @Mock
    private LearningPlanMapper planMapper;

    @Mock
    private LearningRecordMapper recordMapper;

    @Mock
    private WordMapper wordMapper;

    @Mock
    private WordProgressMapper progressMapper;

    @Mock
    private WordService wordService;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(progressService, planMapper, recordMapper,
                wordMapper, progressMapper, wordService);
        lenient().when(progressService.zoneOf(any())).thenReturn(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void shouldBuildDashboard_withStreak() {
        LocalDate today = LocalDate.now();
        LearningPlan plan = new LearningPlan();
        plan.setNewWordCount(20);
        plan.setCompletedCount(5);
        when(progressService.currentStudyDay(1L)).thenReturn(today);
        when(planMapper.selectOne(any())).thenReturn(plan);
        when(recordMapper.countCorrect(1L)).thenReturn(80L);
        when(recordMapper.countTotal(1L)).thenReturn(100L);
        when(progressService.countLearned(1L)).thenReturn(30L);
        when(progressService.countComplete(1L)).thenReturn(3L);
        when(progressService.countDue(1L)).thenReturn(2L);
        when(progressService.listLearnedDays(1L)).thenReturn(List.of(today, today.minusDays(1)));

        DashboardStatsResponse stats = statisticsService.dashboard(1L);

        assertThat(stats.totalLearned()).isEqualTo(30);
        assertThat(stats.masteredCount()).isEqualTo(3);
        assertThat(stats.dueCount()).isEqualTo(2);
        assertThat(stats.todayTotal()).isEqualTo(20);
        assertThat(stats.todayCompleted()).isEqualTo(5);
        assertThat(stats.streakDays()).isEqualTo(2);
        assertThat(stats.accuracy()).isEqualTo(80.0);
    }

    @Test
    void shouldBuildWeekly_withSevenPoints() {
        LocalDate todayDate = LocalDate.now();
        when(progressService.currentStudyDay(1L)).thenReturn(todayDate);
        String today = todayDate.toString();
        when(recordMapper.countByDay(any(), any())).thenReturn(
                List.of(Map.of("day", today, "cnt", 5L)));

        List<WeeklyPoint> weekly = statisticsService.weekly(1L);

        assertThat(weekly).hasSize(7);
        assertThat(weekly.get(6).count()).isEqualTo(5);
        assertThat(weekly.get(0).count()).isZero();
    }

    @Test
    void shouldBuildRecentRecords() {
        LearningRecord record = new LearningRecord();
        record.setWordId(10L);
        record.setStepType("TRANS_EN");
        record.setIsCorrect(1);
        record.setCreatedAt(LocalDateTime.of(2026, 8, 23, 12, 0));
        when(recordMapper.selectRecent(1L, 5)).thenReturn(List.of(record));
        Word word = new Word();
        word.setId(10L);
        word.setWord("ability");
        when(wordMapper.selectById(10L)).thenReturn(word);

        List<RecentRecordVO> recent = statisticsService.recent(1L, 5);

        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).word()).isEqualTo("ability");
        assertThat(recent.get(0).correct()).isTrue();
    }

    @Test
    void shouldAggregateStageDistribution() {
        when(progressMapper.selectList(any())).thenReturn(List.of(
                progress("LEARNING", 0, 0, 0),
                progress("MASTERED", 1, 2, 0),
                progress("REVIEWING", 3, 5, 1),
                progress("REVIEWING", 3, 6, 1),
                progress("COMPLETE", 7, 9, 0)));

        StageDistributionResponse distribution = statisticsService.stageDistribution(1L);

        assertThat(distribution.total()).isEqualTo(5);
        assertThat(distribution.learning()).isEqualTo(1);
        assertThat(distribution.mastered()).isEqualTo(1);
        assertThat(distribution.reviewing()).isEqualTo(2);
        assertThat(distribution.complete()).isEqualTo(1);
        // 学习中共 1 个；6 个复习轮次桶 + 长期记忆桶
        assertThat(distribution.buckets()).hasSize(8);
        assertThat(bucket(distribution, 0).count()).isEqualTo(1);
        assertThat(bucket(distribution, 1).count()).isEqualTo(1);
        assertThat(bucket(distribution, 3).count()).isEqualTo(2);
        assertThat(bucket(distribution, 7).count()).isEqualTo(1);
    }

    @Test
    void shouldSplitOverdueAndFutureInForecast() {
        LocalDate today = LocalDate.of(2026, 9, 21);
        when(progressService.currentStudyDay(1L)).thenReturn(today);
        when(progressMapper.selectList(any())).thenReturn(List.of(
                progressWithNextReview(today.minusDays(3)),
                progressWithNextReview(today),
                progressWithNextReview(today.plusDays(2)),
                progressWithNextReview(today.plusDays(2)),
                // 超出预测窗口，不计入 points，也不计入 overdue
                progressWithNextReview(today.plusDays(30))));

        ReviewForecastResponse forecast = statisticsService.reviewForecast(1L, 7);

        assertThat(forecast.days()).isEqualTo(7);
        assertThat(forecast.overdueCount()).isEqualTo(1);
        assertThat(forecast.totalPlanned()).isEqualTo(3);
        assertThat(forecast.points()).hasSize(7);
        assertThat(forecast.points().get(0).count()).isEqualTo(1);
        assertThat(forecast.points().get(2).count()).isEqualTo(2);
        assertThat(forecast.points().get(6).count()).isZero();
    }

    @Test
    void shouldComputeWeakWordAccuracy() {
        WordProgress progress = progress("MASTERED", 1, 1, 3);
        progress.setWordId(9L);
        progress.setLastLearnedAt(LocalDateTime.of(2026, 9, 20, 10, 0));
        when(progressMapper.selectList(any())).thenReturn(List.of(progress));
        Word word = new Word();
        word.setId(9L);
        word.setWord("abandon");
        word.setChinese("放弃");
        when(wordMapper.selectById(9L)).thenReturn(word);
        when(wordService.toVO(word)).thenReturn(new WordVO(9L, 1L, "abandon", "/əˈbændən/",
                "放弃", "v.", "", "", 3, "CET4"));

        List<WeakWordVO> weakWords = statisticsService.weakWords(1L, true, 5);

        assertThat(weakWords).hasSize(1);
        WeakWordVO vo = weakWords.get(0);
        assertThat(vo.word().word()).isEqualTo("abandon");
        assertThat(vo.wrongCount()).isEqualTo(3);
        assertThat(vo.correctCount()).isEqualTo(1);
        assertThat(vo.accuracy()).isEqualTo(25);
        assertThat(vo.stage()).isEqualTo(1);
    }

    private StageBucket bucket(StageDistributionResponse distribution, int stage) {
        return distribution.buckets().stream()
                .filter(item -> item.stage() == stage)
                .findFirst()
                .orElseThrow();
    }

    private WordProgress progress(String status, int stage, int correct, int wrong) {
        WordProgress progress = new WordProgress();
        progress.setUserId(1L);
        progress.setWordId(1L);
        progress.setStatus(status);
        progress.setStage(stage);
        progress.setLearnCount(correct + wrong);
        progress.setCorrectCount(correct);
        progress.setWrongCount(wrong);
        return progress;
    }

    private WordProgress progressWithNextReview(LocalDate dueDay) {
        WordProgress progress = progress("REVIEWING", 2, 1, 0);
        progress.setNextReviewAt(dueDay.atStartOfDay());
        return progress;
    }
}
