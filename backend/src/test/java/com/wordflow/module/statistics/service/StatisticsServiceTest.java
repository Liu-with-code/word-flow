package com.wordflow.module.statistics.service;

import com.wordflow.module.learning.entity.LearningPlan;
import com.wordflow.module.learning.entity.LearningRecord;
import com.wordflow.module.learning.mapper.LearningPlanMapper;
import com.wordflow.module.learning.mapper.LearningRecordMapper;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.statistics.dto.StatisticsDtos.DashboardStatsResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.RecentRecordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeeklyPoint;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.mapper.WordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * 统计服务单元测试。
 *
 * 覆盖：仪表盘聚合、近 7 天柱状数据、最近作答与连续学习天数。
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

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(progressService, planMapper, recordMapper, wordMapper);
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
}
