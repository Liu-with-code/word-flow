package com.wordflow.module.progress.service;

import com.wordflow.module.learning.mapper.LearningRecordMapper;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.mapper.WordProgressMapper;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 进度调度单元测试。
 *
 * 覆盖：复习到期时间按用户时区/日边界锚定、复习阶段推进、
 *       夜间学习单词查询与“立即到期”处理。
 * 使用固定 Clock，保证时间逻辑完全可复现。
 */
@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    /** 固定时刻：2026-08-23 12:00 上海 = 2026-08-23 00:00 纽约（EDT） */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-23T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private WordProgressMapper progressMapper;

    @Mock
    private LearningRecordMapper recordMapper;

    @Mock
    private UserMapper userMapper;

    private ProgressService progressService;

    @BeforeEach
    void setUp() {
        progressService = new ProgressService(progressMapper, recordMapper, userMapper, FIXED_CLOCK);
    }

    @Test
    void shouldScheduleNextMidnight_whenDefaultBoundary() {
        when(userMapper.selectById(1L)).thenReturn(user("Asia/Shanghai", 0));
        when(progressMapper.selectOne(any())).thenReturn(null);

        progressService.markLearned(1L, 100L);

        WordProgress saved = captureUpdatedProgress();
        assertThat(saved.getStatus()).isEqualTo("MASTERED");
        assertThat(saved.getStage()).isEqualTo(1);
        assertThat(saved.getNextReviewAt()).isEqualTo(LocalDateTime.of(2026, 8, 24, 0, 0));
    }

    @Test
    void shouldAttributeToPreviousStudyDay_whenNightBoundaryEnabled() {
        // 纽约当前 00:00（< 日边界 4 点），属于 8/22 学习日，到期 8/23 04:00（纽约时间）
        when(userMapper.selectById(1L)).thenReturn(user("America/New_York", 4));
        when(progressMapper.selectOne(any())).thenReturn(null);

        progressService.markLearned(1L, 100L);

        WordProgress saved = captureUpdatedProgress();
        assertThat(saved.getNextReviewAt()).isEqualTo(LocalDateTime.of(2026, 8, 23, 4, 0));
    }

    @Test
    void shouldAdvanceStageAndScheduleNextReview() {
        when(userMapper.selectById(1L)).thenReturn(user("Asia/Shanghai", 0));
        when(progressMapper.selectOne(any())).thenReturn(progress(1, "MASTERED", LocalDateTime.now()));

        progressService.advanceReview(1L, 100L);

        WordProgress saved = captureUpdatedProgress();
        assertThat(saved.getStatus()).isEqualTo("REVIEWING");
        assertThat(saved.getStage()).isEqualTo(2);
        // 学习日 +2 天 = 8/25 00:00
        assertThat(saved.getNextReviewAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 0, 0));
    }

    @Test
    void shouldComplete_whenFinalStageReached() {
        when(progressMapper.selectOne(any())).thenReturn(progress(6, "REVIEWING", LocalDateTime.now()));

        progressService.advanceReview(1L, 100L);

        WordProgress saved = captureUpdatedProgress();
        assertThat(saved.getStatus()).isEqualTo("COMPLETE");
        assertThat(saved.getNextReviewAt()).isNull();
    }

    @Test
    void shouldComputeNowInUserTimezone() {
        when(userMapper.selectById(1L)).thenReturn(user("America/New_York", 0));

        assertThat(progressService.nowOf(1L)).isEqualTo(LocalDateTime.of(2026, 8, 23, 0, 0));
    }

    @Test
    void shouldMakeDueNow_onlyForMatchingUserWords() {
        when(userMapper.selectById(1L)).thenReturn(user("Asia/Shanghai", 0));
        WordProgress mine = progress(1, "MASTERED", null);
        mine.setUserId(1L);
        WordProgress other = progress(2, "MASTERED", null);
        other.setUserId(2L);

        progressService.makeDueNow(1L, List.of(mine, other));

        verify(progressMapper).updateById(mine);
        verify(progressMapper, never()).updateById(other);
        assertThat(mine.getNextReviewAt()).isEqualTo(LocalDateTime.of(2026, 8, 23, 12, 0));
    }

    @Test
    void shouldListNightWords_byWindow() {
        WordProgress night = progress(1, "MASTERED", null);
        when(progressMapper.selectList(any())).thenReturn(List.of(night));

        List<WordProgress> result = progressService.listNightWords(
                1L, LocalDateTime.of(2026, 8, 23, 0, 0), LocalDateTime.of(2026, 8, 23, 6, 0));

        assertThat(result).containsExactly(night);
    }

    @Test
    void shouldCountLearned_onlyMasteredAndBeyond() {
        when(progressMapper.selectCount(any())).thenReturn(6L);

        assertThat(progressService.countLearned(1L)).isEqualTo(6);
    }

    @Test
    void shouldGroupLearnedDays_byTimezoneAndBoundary() {
        when(userMapper.selectById(1L)).thenReturn(user("America/New_York", 4));
        WordProgress night = progress(1, "MASTERED", null);
        night.setLastLearnedAt(LocalDateTime.of(2026, 8, 23, 0, 30)); // 纽约凌晨 00:30 -> 8/22 学习日
        WordProgress day = progress(2, "MASTERED", null);
        day.setLastLearnedAt(LocalDateTime.of(2026, 8, 23, 12, 0));   // 8/23 学习日
        when(progressMapper.selectList(any())).thenReturn(List.of(night, day));

        List<java.time.LocalDate> days = progressService.listLearnedDays(1L);

        assertThat(days).containsExactly(
                java.time.LocalDate.of(2026, 8, 23),
                java.time.LocalDate.of(2026, 8, 22));
    }

    @Test
    void shouldComputeCurrentStudyDay_withBoundary() {
        when(userMapper.selectById(1L)).thenReturn(user("America/New_York", 4));

        // 固定时刻：纽约 00:00（< 日边界 4 点），属于 8/22 学习日
        assertThat(progressService.currentStudyDay(1L))
                .isEqualTo(java.time.LocalDate.of(2026, 8, 22));
    }

    private WordProgress captureUpdatedProgress() {
        ArgumentCaptor<WordProgress> captor = ArgumentCaptor.forClass(WordProgress.class);
        verify(progressMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private User user(String timezone, int boundary) {
        User user = new User();
        user.setId(1L);
        user.setTimezone(timezone);
        user.setDayBoundaryHour(boundary);
        user.setNightCutoffHour(6);
        return user;
    }

    private WordProgress progress(int stage, String status, LocalDateTime nextReviewAt) {
        WordProgress progress = new WordProgress();
        progress.setId((long) stage);
        progress.setUserId(1L);
        progress.setWordId(100L);
        progress.setStage(stage);
        progress.setStatus(status);
        progress.setNextReviewAt(nextReviewAt);
        return progress;
    }
}
