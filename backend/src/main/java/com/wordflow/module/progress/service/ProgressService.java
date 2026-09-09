package com.wordflow.module.progress.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordflow.module.learning.entity.LearningRecord;
import com.wordflow.module.learning.mapper.LearningRecordMapper;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.mapper.WordProgressMapper;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 单词进度服务（核心调度逻辑）。
 *
 * 模块职责：
 *   - 维护用户-单词进度；
 *   - 记录每一步作答流水；
 *   - 按艾宾浩斯遗忘曲线安排复习时间。
 *
 * 艾宾浩斯间隔（依据科普中国整理的经典复习周期）：
 *   [1, 2, 4, 7, 15, 30] 天，即学完后第 1/3/7/14/29/59 天左右复习，
 *   全部通过后进入 COMPLETE（长期记忆）。
 *
 * 时间标准说明：
 *   - 不写死服务器时间；每个用户使用自己的时区（sys_user.timezone）；
 *   - 复习到期时间按「用户学习日 + 间隔天数」锚定到用户所在时区的日边界；
 *   - 日边界小时来自 sys_user.day_boundary_hour（0=不启用，凌晨该点前背的词算前一天）。
 *
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    /** 艾宾浩斯复习间隔（天），下标 = 已完成的复习轮次 */
    private static final int[] EBBINGHAUS_GAPS_DAYS = {1, 2, 4, 7, 15, 30};

    public static final String STATUS_LEARNING = "LEARNING";
    public static final String STATUS_MASTERED = "MASTERED";
    public static final String STATUS_REVIEWING = "REVIEWING";
    public static final String STATUS_COMPLETE = "COMPLETE";

    private final WordProgressMapper progressMapper;
    private final LearningRecordMapper recordMapper;
    private final UserMapper userMapper;
    private final Clock clock;

    /** 获取（不存在则创建）用户单词进度行。 */
    @Transactional(rollbackFor = Exception.class)
    public WordProgress ensureProgress(Long userId, Long wordId) {
        WordProgress progress = progressMapper.selectOne(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getWordId, wordId));
        if (progress == null) {
            progress = new WordProgress();
            progress.setUserId(userId);
            progress.setWordId(wordId);
            progress.setStatus(STATUS_LEARNING);
            progress.setStage(0);
            progress.setLearnCount(0);
            progress.setCorrectCount(0);
            progress.setWrongCount(0);
            progressMapper.insert(progress);
        }
        return progress;
    }

    /** 记录一次作答，并更新正确/错误统计。 */
    @Transactional(rollbackFor = Exception.class)
    public void recordAnswer(Long userId, Long wordId, String sessionType, String stepType,
                             boolean correct, String userAnswer, String aiFeedback) {
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setWordId(wordId);
        record.setSessionType(sessionType);
        record.setStepType(stepType);
        record.setIsCorrect(correct ? 1 : 0);
        record.setUserAnswer(userAnswer);
        record.setAiFeedback(aiFeedback);
        recordMapper.insert(record);

        WordProgress progress = ensureProgress(userId, wordId);
        progress.setLearnCount(progress.getLearnCount() + 1);
        if (correct) {
            progress.setCorrectCount(progress.getCorrectCount() + 1);
        } else {
            progress.setWrongCount(progress.getWrongCount() + 1);
        }
        progress.setLastLearnedAt(LocalDateTime.now(clock));
        progressMapper.updateById(progress);
    }

    /** 今日学习流程全部通过后调用：进入艾宾浩斯第 1 轮（次日日边界到期）。 */
    @Transactional(rollbackFor = Exception.class)
    public void markLearned(Long userId, Long wordId) {
        WordProgress progress = ensureProgress(userId, wordId);
        progress.setStatus(STATUS_MASTERED);
        progress.setStage(1);
        progress.setNextReviewAt(nextReviewAt(userId, EBBINGHAUS_GAPS_DAYS[0]));
        progressMapper.updateById(progress);
    }

    /** 复习通过后调用：推进阶段并安排下一次复习；全部阶段完成则标记 COMPLETE。 */
    @Transactional(rollbackFor = Exception.class)
    public void advanceReview(Long userId, Long wordId) {
        WordProgress progress = ensureProgress(userId, wordId);
        int nextStage = progress.getStage() + 1;
        if (nextStage >= EBBINGHAUS_GAPS_DAYS.length + 1) {
            progress.setStatus(STATUS_COMPLETE);
            progress.setStage(nextStage);
            progress.setNextReviewAt(null);
        } else {
            progress.setStatus(STATUS_REVIEWING);
            progress.setStage(nextStage);
            progress.setNextReviewAt(nextReviewAt(userId, EBBINGHAUS_GAPS_DAYS[nextStage - 1]));
        }
        progressMapper.updateById(progress);
    }

    /** 查询到期待复习的单词进度（按下次复习时间升序，即最该复习的在前）。 */
    public List<WordProgress> listDueProgress(Long userId) {
        return progressMapper.selectList(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .in(WordProgress::getStatus, STATUS_MASTERED, STATUS_REVIEWING)
                        .le(WordProgress::getNextReviewAt, nowOf(userId))
                        .orderByAsc(WordProgress::getNextReviewAt));
    }

    public long countDue(Long userId) {
        return progressMapper.selectCount(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .in(WordProgress::getStatus, STATUS_MASTERED, STATUS_REVIEWING)
                        .le(WordProgress::getNextReviewAt, nowOf(userId)));
    }

    /** 真正学过的单词数（已进入复习队列的：MASTERED/REVIEWING/COMPLETE）。 */
    public long countLearned(Long userId) {
        return progressMapper.selectCount(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .in(WordProgress::getStatus, STATUS_MASTERED, STATUS_REVIEWING, STATUS_COMPLETE));
    }

    /**
     * 用户实际学过单词的“学习日”列表（倒序、去重）。
     * 学习日按用户时区 + 日边界归并：凌晨日边界前学的词算前一天。
     */
    public List<LocalDate> listLearnedDays(Long userId) {
        ZoneId zone = zoneOf(userId);
        int boundary = boundaryHour(userId);
        List<WordProgress> learned = progressMapper.selectList(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .in(WordProgress::getStatus, STATUS_MASTERED, STATUS_REVIEWING, STATUS_COMPLETE));
        return learned.stream()
                .map(WordProgress::getLastLearnedAt)
                .filter(Objects::nonNull)
                .map(time -> {
                    LocalDate day = time.toLocalDate();
                    return time.getHour() < boundary ? day.minusDays(1) : day;
                })
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** 查询「凌晨 0 点到 cutoff 点之间」学过的单词（用于夜间学习智能弹窗）。 */
    public List<WordProgress> listNightWords(Long userId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return progressMapper.selectList(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .in(WordProgress::getStatus, STATUS_MASTERED, STATUS_REVIEWING)
                        .ge(WordProgress::getLastLearnedAt, windowStart)
                        .lt(WordProgress::getLastLearnedAt, windowEnd));
    }

    /** 把一批单词的复习时间改为“现在”，使其立即进入今日复习队列。 */
    @Transactional(rollbackFor = Exception.class)
    public void makeDueNow(Long userId, List<WordProgress> words) {
        LocalDateTime now = nowOf(userId);
        for (WordProgress progress : words) {
            if (progress.getUserId().equals(userId)) {
                progress.setNextReviewAt(now);
                progressMapper.updateById(progress);
            }
        }
    }

    /** 回收“从未作答”的占位进度（LEARNING 且 learn_count=0），使单词可被重新选入计划。 */
    @Transactional(rollbackFor = Exception.class)
    public void removePlaceholderProgress(Long userId, List<Long> wordIds) {
        if (wordIds == null || wordIds.isEmpty()) {
            return;
        }
        progressMapper.delete(Wrappers.<WordProgress>lambdaQuery()
                .eq(WordProgress::getUserId, userId)
                .in(WordProgress::getWordId, wordIds)
                .eq(WordProgress::getStatus, STATUS_LEARNING)
                .eq(WordProgress::getLearnCount, 0));
    }

    /** 用户时区下的“当前时间”（包级可见，便于单元测试断言）。 */
    LocalDateTime nowOf(Long userId) {
        return LocalDateTime.now(clock.withZone(zoneOf(userId)));
    }

    /** 用户所在时区（读取失败回退系统时区）。 */
    public ZoneId zoneOf(Long userId) {
        User user = userMapper.selectById(userId);
        return zoneOf(user);
    }

    private ZoneId zoneOf(User user) {
        String tz = user == null ? null : user.getTimezone();
        if (tz == null || tz.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(tz);
        } catch (Exception ex) {
            return ZoneId.systemDefault();
        }
    }

    /** 当前“学习日”：用户时区下，日边界前（如凌晨 0-4 点）学习算前一天。 */
    public LocalDate currentStudyDay(Long userId) {
        ZoneId zone = zoneOf(userId);
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));
        int boundary = boundaryHour(userId);
        return now.getHour() < boundary ? now.toLocalDate().minusDays(1) : now.toLocalDate();
    }

    /** 复习到期时间：锚定到「学习日 + 间隔天数」当天的日边界时刻（用户时区）。 */
    private LocalDateTime nextReviewAt(Long userId, int gapDays) {
        int boundary = boundaryHour(userId);
        return LocalDateTime.of(currentStudyDay(userId).plusDays(gapDays), LocalTime.of(boundary, 0));
    }

    private int boundaryHour(Long userId) {
        User user = userMapper.selectById(userId);
        Integer hour = user == null ? null : user.getDayBoundaryHour();
        if (hour == null) {
            return 0;
        }
        return Math.max(0, Math.min(23, hour));
    }

    public long countAll(Long userId) {
        return progressMapper.selectCount(
                Wrappers.<WordProgress>lambdaQuery().eq(WordProgress::getUserId, userId));
    }

    public long countComplete(Long userId) {
        return progressMapper.selectCount(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getStatus, STATUS_COMPLETE));
    }
}
