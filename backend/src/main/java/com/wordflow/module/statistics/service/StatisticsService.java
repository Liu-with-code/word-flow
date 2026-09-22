package com.wordflow.module.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wordflow.common.PageResult;
import com.wordflow.module.learning.entity.LearningPlan;
import com.wordflow.module.learning.entity.LearningRecord;
import com.wordflow.module.learning.mapper.LearningPlanMapper;
import com.wordflow.module.learning.mapper.LearningRecordMapper;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.mapper.WordProgressMapper;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.statistics.dto.StatisticsDtos.DashboardStatsResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.RecentRecordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.ReviewForecastPoint;
import com.wordflow.module.statistics.dto.StatisticsDtos.ReviewForecastResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.StageBucket;
import com.wordflow.module.statistics.dto.StatisticsDtos.StageDistributionResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeakWordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeeklyPoint;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.mapper.WordMapper;
import com.wordflow.module.word.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 统计服务：仪表盘、走势、记忆阶段分布、复习量预测与错题查询。
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /** 艾宾浩斯间隔（天），与 {@link ProgressService} 保持一致，仅用于阶段文案 */
    private static final int[] STAGE_GAPS_DAYS = {1, 2, 4, 7, 15, 30};

    /** 复习预测默认天数与上限 */
    private static final int DEFAULT_FORECAST_DAYS = 14;
    private static final int MAX_FORECAST_DAYS = 60;

    /** 薄弱单词返回条数上限，避免一次拉取过多数据 */
    private static final int MAX_WEAK_WORDS = 200;

    /** 错题本分页大小上限 */
    private static final int MAX_PAGE_SIZE = 50;

    /** 走势图聚合天数 */
    private static final int WEEKLY_DAYS = 7;

    /** 预估集合容量：按日聚合的固定 7 天数据 */
    private static final int MAP_INIT_CAPACITY = 8;

    /** 阶段分布桶数量：学习中 + 长期记忆 + 6 个复习轮次 */
    private static final int STAGE_BUCKET_COUNT = 8;

    private final ProgressService progressService;
    private final LearningPlanMapper planMapper;
    private final LearningRecordMapper recordMapper;
    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final WordService wordService;

    public DashboardStatsResponse dashboard(Long userId) {
        LocalDate today = progressService.currentStudyDay(userId);
        LearningPlan todayPlan = planMapper.selectOne(
                Wrappers.<LearningPlan>lambdaQuery()
                        .eq(LearningPlan::getUserId, userId)
                        .eq(LearningPlan::getPlanDate, today));
        long correct = recordMapper.countCorrect(userId);
        long total = recordMapper.countTotal(userId);
        double accuracy = total == 0 ? 0 : Math.round(correct * 1000.0 / total) / 10.0;
        return new DashboardStatsResponse(
                progressService.countLearned(userId),
                progressService.countComplete(userId),
                progressService.countDue(userId),
                todayPlan == null ? 0 : todayPlan.getNewWordCount(),
                todayPlan == null ? 0 : todayPlan.getCompletedCount(),
                countStreakDays(userId),
                accuracy);
    }

    public List<WeeklyPoint> weekly(Long userId) {
        LocalDate today = progressService.currentStudyDay(userId);
        LocalDateTime since = today.minusDays(WEEKLY_DAYS - 1L).atStartOfDay();
        List<Map<String, Object>> rows = recordMapper.countByDay(userId, since);
        Map<String, Long> byDay = new HashMap<>(MAP_INIT_CAPACITY);
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), ((Number) row.get("cnt")).longValue());
        }
        List<WeeklyPoint> result = new ArrayList<>(WEEKLY_DAYS);
        for (int i = WEEKLY_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            result.add(new WeeklyPoint(day.toString(), byDay.getOrDefault(day.toString(), 0L)));
        }
        return result;
    }

    public List<RecentRecordVO> recent(Long userId, int limit) {
        return recordMapper.selectRecent(userId, Math.min(20, Math.max(1, limit))).stream()
                .map(this::toRecentVO)
                .toList();
    }

    /**
     * 记忆阶段分布：按艾宾浩斯阶段聚合单词数量。
     *
     * stage 语义：0=学习中，1-6 对应 1/2/4/7/15/30 天复习阶段，大于 6 表示已通过全部复习。
     */
    public StageDistributionResponse stageDistribution(Long userId) {
        List<WordProgress> all = progressMapper.selectList(
                Wrappers.<WordProgress>lambdaQuery().eq(WordProgress::getUserId, userId));

        long complete = countByStatus(all, ProgressService.STATUS_COMPLETE);
        long reviewing = countByStatus(all, ProgressService.STATUS_REVIEWING);
        long mastered = countByStatus(all, ProgressService.STATUS_MASTERED);
        long learning = countByStatus(all, ProgressService.STATUS_LEARNING);

        List<StageBucket> buckets = new ArrayList<>(STAGE_BUCKET_COUNT);
        buckets.add(new StageBucket("学习中", 0, learning, "尚未完成今日四步练习"));
        for (int stage = 1; stage <= STAGE_GAPS_DAYS.length; stage++) {
            int currentStage = stage;
            long count = all.stream()
                    .filter(progress -> progress.getStage() != null && progress.getStage() == currentStage)
                    .filter(progress -> !ProgressService.STATUS_COMPLETE.equals(progress.getStatus()))
                    .count();
            buckets.add(new StageBucket(
                    "第 " + stage + " 轮",
                    stage,
                    count,
                    STAGE_GAPS_DAYS[stage - 1] + " 天后复习"));
        }
        long longTerm = all.stream()
                .filter(progress -> progress.getStage() != null
                        && progress.getStage() > STAGE_GAPS_DAYS.length)
                .count();
        buckets.add(new StageBucket("长期记忆", STAGE_GAPS_DAYS.length + 1, longTerm, "已通过全部复习"));

        return new StageDistributionResponse(
                all.size(), complete, reviewing, mastered, learning, buckets);
    }

    /**
     * 未来复习量预测：按「用户学习日」统计每一天将到期的复习单词数。
     *
     * 未到期的单词按 next_review_at 归属到所在日期；已逾期的单词单独计入 overdueCount，
     * 不与未来日期重复统计。统计口径为用户时区的自然日，与服务器时区无关。
     */
    public ReviewForecastResponse reviewForecast(Long userId, int days) {
        int horizon = Math.max(1, Math.min(MAX_FORECAST_DAYS, days <= 0 ? DEFAULT_FORECAST_DAYS : days));
        LocalDate today = progressService.currentStudyDay(userId);
        LocalDate horizonEnd = today.plusDays(horizon - 1L);

        List<WordProgress> scheduled = progressMapper.selectList(
                Wrappers.<WordProgress>lambdaQuery()
                        .eq(WordProgress::getUserId, userId)
                        .in(WordProgress::getStatus,
                                ProgressService.STATUS_MASTERED, ProgressService.STATUS_REVIEWING)
                        .isNotNull(WordProgress::getNextReviewAt));

        Map<String, Long> countByDay = new HashMap<>(MAP_INIT_CAPACITY * 2);
        long overdue = 0;
        long planned = 0;
        for (WordProgress progress : scheduled) {
            LocalDate dueDay = progress.getNextReviewAt().toLocalDate();
            if (dueDay.isBefore(today)) {
                overdue++;
                continue;
            }
            if (!dueDay.isAfter(horizonEnd)) {
                countByDay.merge(dueDay.toString(), 1L, Long::sum);
                planned++;
            }
        }

        List<ReviewForecastPoint> points = new ArrayList<>(horizon);
        for (int i = 0; i < horizon; i++) {
            LocalDate day = today.plusDays(i);
            points.add(new ReviewForecastPoint(day.toString(), countByDay.getOrDefault(day.toString(), 0L)));
        }
        return new ReviewForecastResponse(horizon, overdue, planned, points);
    }

    /**
     * 薄弱单词（错题）概览：答错次数最多的单词。
     *
     * @param onlyWrong true=仅返回答错过的单词；false=返回全部已作答单词
     */
    public List<WeakWordVO> weakWords(Long userId, boolean onlyWrong, int limit) {
        int size = Math.max(1, Math.min(MAX_WEAK_WORDS, limit));
        return progressMapper.selectList(weakWordWrapper(userId, onlyWrong)
                        .last("LIMIT " + size)).stream()
                .map(this::toWeakWordVO)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 错题本分页查询：按答错次数降序、答对次数升序排列。 */
    public PageResult<WeakWordVO> weakWordPage(Long userId, long page, long size) {
        long current = Math.max(1, page);
        long pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        Page<WordProgress> result = progressMapper.selectPage(
                new Page<>(current, pageSize), weakWordWrapper(userId, true));
        List<WeakWordVO> list = result.getRecords().stream()
                .map(this::toWeakWordVO)
                .filter(Objects::nonNull)
                .toList();
        PageResult<WeakWordVO> response = new PageResult<>();
        response.setList(list);
        response.setTotal(result.getTotal());
        response.setPage(result.getCurrent());
        response.setSize(result.getSize());
        return response;
    }

    private LambdaQueryWrapper<WordProgress> weakWordWrapper(Long userId, boolean onlyWrong) {
        var wrapper = Wrappers.<WordProgress>lambdaQuery()
                .eq(WordProgress::getUserId, userId)
                .gt(WordProgress::getLearnCount, 0);
        if (onlyWrong) {
            wrapper.gt(WordProgress::getWrongCount, 0);
        }
        return wrapper.orderByDesc(WordProgress::getWrongCount)
                .orderByAsc(WordProgress::getCorrectCount)
                .orderByDesc(WordProgress::getLastLearnedAt);
    }

    private WeakWordVO toWeakWordVO(WordProgress progress) {
        Word word = wordMapper.selectById(progress.getWordId());
        if (word == null) {
            return null;
        }
        int correct = nvl(progress.getCorrectCount());
        int wrong = nvl(progress.getWrongCount());
        int answered = correct + wrong;
        int accuracy = answered == 0 ? 0 : (int) Math.round(correct * 100.0 / answered);
        return new WeakWordVO(
                wordService.toVO(word),
                wrong,
                correct,
                accuracy,
                nvl(progress.getStage()),
                progress.getStatus(),
                progress.getLastLearnedAt());
    }

    private RecentRecordVO toRecentVO(LearningRecord record) {
        Word word = wordMapper.selectById(record.getWordId());
        return new RecentRecordVO(
                word == null ? "已删除单词" : word.getWord(),
                record.getStepType(),
                record.getIsCorrect() != null && record.getIsCorrect() == 1,
                record.getCreatedAt());
    }

    /** 连续学习天数：从今天（或昨天）开始向前连续统计已完成的计划日期。 */
    private int countStreakDays(Long userId) {
        List<LocalDate> dates = progressService.listLearnedDays(userId);
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate expected = progressService.currentStudyDay(userId);
        if (!dates.get(0).equals(expected) && !dates.get(0).equals(expected.minusDays(1))) {
            return 0;
        }
        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected) || date.equals(expected.minusDays(1))) {
                streak++;
                expected = date.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private long countByStatus(List<WordProgress> list, String status) {
        return list.stream().filter(progress -> status.equals(progress.getStatus())).count();
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
